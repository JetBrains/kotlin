/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.DescriptorsJvmAbiUtil
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.isUnderlyingPropertyOfInlineClass
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.types.TypeUtils
import java.lang.reflect.*
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.IllegalPropertyDelegateAccessException
import kotlin.reflect.jvm.internal.JvmPropertySignature.*
import kotlin.reflect.jvm.internal.calls.*
import kotlin.reflect.jvm.isAccessible

internal abstract class KPropertyImpl<out V> private constructor(
    override val container: KDeclarationContainerImpl,
    override val name: String,
    val signature: String,
    descriptorInitialValue: PropertyDescriptor?,
    private val rawBoundReceiver: Any?
) : KCallableImpl<V>(), KProperty<V> {
    constructor(container: KDeclarationContainerImpl, name: String, signature: String, boundReceiver: Any?) : this(
        container, name, signature, null, boundReceiver
    )

    constructor(container: KDeclarationContainerImpl, descriptor: PropertyDescriptor) : this(
        container,
        descriptor.name.asString(),
        RuntimeTypeMapper.mapPropertySignature(descriptor).asString(),
        descriptor,
        CallableReference.NO_RECEIVER
    )

    val boundReceiver
        get() = rawBoundReceiver.coerceToExpectedReceiverType(descriptor)

    override val isBound: Boolean get() = rawBoundReceiver !== CallableReference.NO_RECEIVER

    private val _javaField = lazy(PUBLICATION) {
        when (val jvmSignature = RuntimeTypeMapper.mapPropertySignature(descriptor)) {
            is KotlinProperty -> {
                val descriptor = jvmSignature.descriptor
                JvmProtoBufUtil.getJvmFieldSignature(jvmSignature.proto, jvmSignature.nameResolver, jvmSignature.typeTable)?.let {
                    val owner = if (DescriptorsJvmAbiUtil.isPropertyWithBackingFieldInOuterClass(descriptor) ||
                        JvmProtoBufUtil.isMovedFromInterfaceCompanion(jvmSignature.proto)
                    ) {
                        container.jClass.enclosingClass
                    } else descriptor.containingDeclaration.let { containingDeclaration ->
                        if (containingDeclaration is ClassDescriptor) containingDeclaration.toJavaClass()
                        else container.jClass
                    }

                    try {
                        owner?.getDeclaredField(it.name)
                    } catch (e: NoSuchFieldException) {
                        null
                    }
                }
            }
            is JavaField -> jvmSignature.field
            is JavaMethodProperty -> null
            is MappedKotlinProperty -> null
        }
    }

    val javaField: Field? get() = _javaField.value

    protected fun computeDelegateSource(): Member? {
        if (!descriptor.isDelegated) return null
        val jvmSignature = RuntimeTypeMapper.mapPropertySignature(descriptor)
        if (jvmSignature is KotlinProperty && jvmSignature.signature.hasDelegateMethod()) {
            val method = jvmSignature.signature.delegateMethod
            if (!method.hasName() || !method.hasDesc()) return null
            val name = jvmSignature.nameResolver.getString(method.name)
            val desc = jvmSignature.nameResolver.getString(method.desc)
            return container.findMethodBySignature(name, desc)
        }
        return javaField
    }

    protected fun getDelegateImpl(fieldOrMethod: Member?, receiver1: Any?, receiver2: Any?): Any? =
        try {
            if (receiver1 === EXTENSION_PROPERTY_DELEGATE || receiver2 === EXTENSION_PROPERTY_DELEGATE) {
                if (descriptor.extensionReceiverParameter == null) {
                    throw RuntimeException(
                        "'$this' is not an extension property and thus getExtensionDelegate() " +
                                "is not going to work, use getDelegate() instead"
                    )
                }
            }

            val realReceiver1 = (if (isBound) boundReceiver else receiver1).takeIf { it !== EXTENSION_PROPERTY_DELEGATE }
            val realReceiver2 = (if (isBound) receiver1 else receiver2).takeIf { it !== EXTENSION_PROPERTY_DELEGATE }
            (fieldOrMethod as? AccessibleObject)?.isAccessible = isAccessible
            when (fieldOrMethod) {
                null -> null
                is Field -> fieldOrMethod.get(realReceiver1)
                is Method -> when (fieldOrMethod.parameterTypes.size) {
                    0 -> fieldOrMethod.invoke(null)
                    1 -> fieldOrMethod.invoke(null, realReceiver1 ?: defaultPrimitiveValue(fieldOrMethod.parameterTypes[0]))
                    2 -> fieldOrMethod.invoke(null, realReceiver1, realReceiver2 ?: defaultPrimitiveValue(fieldOrMethod.parameterTypes[1]))
                    else -> throw AssertionError("delegate method $fieldOrMethod should take 0, 1, or 2 parameters")
                }
                else -> throw AssertionError("delegate field/method $fieldOrMethod neither field nor method")
            }
        } catch (e: IllegalAccessException) {
            throw IllegalPropertyDelegateAccessException(e)
        }

    abstract override val getter: Getter<V>

    private val _descriptor = ReflectProperties.lazySoft(descriptorInitialValue) {
        container.findPropertyDescriptor(name, signature)
    }

    override val descriptor: PropertyDescriptor get() = _descriptor()

    override val caller: Caller<*> get() = getter.caller

    override val defaultCaller: Caller<*>? get() = getter.defaultCaller

    override val isLateinit: Boolean get() = descriptor.isLateInit

    override val isConst: Boolean get() = descriptor.isConst

    override val isSuspend: Boolean get() = false

    override fun equals(other: Any?): Boolean {
        val that = other.asKPropertyImpl() ?: return false
        return container == that.container && name == that.name && signature == that.signature && rawBoundReceiver == that.rawBoundReceiver
    }

    override fun hashCode(): Int =
        (container.hashCode() * 31 + name.hashCode()) * 31 + signature.hashCode()

    override fun toString(): String =
        ReflectionObjectRenderer.renderProperty(descriptor)

    abstract class Accessor<out PropertyType, out ReturnType> :
        KCallableImpl<ReturnType>(), KProperty.Accessor<PropertyType>, KFunction<ReturnType> {
        abstract override val property: KPropertyImpl<PropertyType>

        abstract override val descriptor: PropertyAccessorDescriptor

        override val container: KDeclarationContainerImpl get() = property.container

        override val defaultCaller: Caller<*>? get() = null

        override val isBound: Boolean get() = property.isBound

        override val isInline: Boolean get() = descriptor.isInline
        override val isExternal: Boolean get() = descriptor.isExternal
        override val isOperator: Boolean get() = descriptor.isOperator
        override val isInfix: Boolean get() = descriptor.isInfix
        override val isSuspend: Boolean get() = descriptor.isSuspend
    }

    abstract class Getter<out V> : Accessor<V, V>(), KProperty.Getter<V> {
        override val name: String get() = "<get-${property.name}>"

        override val descriptor: PropertyGetterDescriptor by ReflectProperties.lazySoft {
            // TODO: default getter created this way won't have any source information
            property.descriptor.getter ?: DescriptorFactory.createDefaultGetter(property.descriptor, Annotations.EMPTY)
        }

        override val caller: Caller<*> by lazy(PUBLICATION) {
            computeCallerForAccessor(isGetter = true)
        }

        override fun toString(): String = "getter of $property"

        override fun equals(other: Any?): Boolean =
            other is Getter<*> && property == other.property

        override fun hashCode(): Int =
            property.hashCode()
    }

    abstract class Setter<V> : Accessor<V, Unit>(), KMutableProperty.Setter<V> {
        override val name: String get() = "<set-${property.name}>"

        override val descriptor: PropertySetterDescriptor by ReflectProperties.lazySoft {
            // TODO: default setter created this way won't have any source information
            property.descriptor.setter ?: DescriptorFactory.createDefaultSetter(property.descriptor, Annotations.EMPTY, Annotations.EMPTY)
        }

        override val caller: Caller<*> by lazy(PUBLICATION) {
            computeCallerForAccessor(isGetter = false)
        }

        override fun toString(): String = "setter of $property"

        override fun equals(other: Any?): Boolean =
            other is Setter<*> && property == other.property

        override fun hashCode(): Int =
            property.hashCode()
    }

    companion object {
        val EXTENSION_PROPERTY_DELEGATE = Any()
    }
}

internal val KPropertyImpl.Accessor<*, *>.boundReceiver
    get() = property.boundReceiver

private fun KPropertyImpl.Accessor<*, *>.computeCallerForAccessor(isGetter: Boolean): Caller<*> {
    if (KDeclarationContainerImpl.LOCAL_PROPERTY_SIGNATURE.matches(property.signature)) {
        return ThrowingCaller
    }

    fun isJvmStaticProperty(): Boolean =
        property.descriptor.annotations.hasAnnotation(JVM_STATIC)

    fun isNotNullProperty(): Boolean =
        !TypeUtils.isNullableType(property.descriptor.type)

    fun computeFieldCaller(field: Field): CallerImpl<Field> = when {
        property.descriptor.isJvmFieldPropertyInCompanionObject() || !Modifier.isStatic(field.modifiers) ->
            if (isGetter)
                if (isBound) CallerImpl.FieldGetter.BoundInstance(field, boundReceiver)
                else CallerImpl.FieldGetter.Instance(field)
            else
                if (isBound) CallerImpl.FieldSetter.BoundInstance(field, isNotNullProperty(), boundReceiver)
                else CallerImpl.FieldSetter.Instance(field, isNotNullProperty())
        isJvmStaticProperty() ->
            if (isGetter)
                if (isBound) CallerImpl.FieldGetter.BoundJvmStaticInObject(field)
                else CallerImpl.FieldGetter.JvmStaticInObject(field)
            else
                if (isBound) CallerImpl.FieldSetter.BoundJvmStaticInObject(field, isNotNullProperty())
                else CallerImpl.FieldSetter.JvmStaticInObject(field, isNotNullProperty())
        else ->
            if (isGetter) CallerImpl.FieldGetter.Static(field)
            else CallerImpl.FieldSetter.Static(field, isNotNullProperty())
    }

    val jvmSignature = RuntimeTypeMapper.mapPropertySignature(property.descriptor)
    return when (jvmSignature) {
        is KotlinProperty -> {
            val accessorSignature = jvmSignature.signature.run {
                when {
                    isGetter -> if (hasGetter()) getter else null
                    else -> if (hasSetter()) setter else null
                }
            }

            val accessor = accessorSignature?.let { signature ->
                property.container.findMethodBySignature(
                    jvmSignature.nameResolver.getString(signature.name),
                    jvmSignature.nameResolver.getString(signature.desc)
                )
            }

            when {
                accessor == null -> {
                    if (property.descriptor.isUnderlyingPropertyOfInlineClass() &&
                        property.descriptor.visibility == DescriptorVisibilities.INTERNAL
                    ) {
                        val unboxMethod =
                            property.descriptor.containingDeclaration.toInlineClass()?.getInlineClassUnboxMethod(property.descriptor)
                                ?: throw KotlinReflectionInternalError("Underlying property of inline class $property should have a field")
                        if (isBound) InternalUnderlyingValOfInlineClass.Bound(unboxMethod, boundReceiver)
                        else InternalUnderlyingValOfInlineClass.Unbound(unboxMethod)
                    } else {
                        val javaField = property.javaField
                            ?: throw KotlinReflectionInternalError("No accessors or field is found for property $property")
                        computeFieldCaller(javaField)
                    }
                }
                !Modifier.isStatic(accessor.modifiers) ->
                    if (isBound) CallerImpl.Method.BoundInstance(accessor, boundReceiver)
                    else CallerImpl.Method.Instance(accessor)
                isJvmStaticProperty() ->
                    if (isBound) CallerImpl.Method.BoundJvmStaticInObject(accessor)
                    else CallerImpl.Method.JvmStaticInObject(accessor)
                else ->
                    if (isBound) CallerImpl.Method.BoundStatic(accessor, boundReceiver)
                    else CallerImpl.Method.Static(accessor)
            }
        }
        is JavaField -> {
            computeFieldCaller(jvmSignature.field)
        }
        is JavaMethodProperty -> {
            val method =
                if (isGetter) jvmSignature.getterMethod
                else jvmSignature.setterMethod ?: throw KotlinReflectionInternalError(
                    "No source found for setter of Java method property: ${jvmSignature.getterMethod}"
                )
            if (isBound) CallerImpl.Method.BoundInstance(method, boundReceiver)
            else CallerImpl.Method.Instance(method)
        }
        is MappedKotlinProperty -> {
            val signature =
                if (isGetter) jvmSignature.getterSignature
                else (jvmSignature.setterSignature ?: throw KotlinReflectionInternalError("No setter found for property $property"))
            val accessor =
                property.container.findMethodBySignature(signature.methodName, signature.methodDesc)
                    ?: throw KotlinReflectionInternalError("No accessor found for property $property")
            assert(!Modifier.isStatic(accessor.modifiers)) { "Mapped property cannot have a static accessor: $property" }

            return if (isBound) CallerImpl.Method.BoundInstance(accessor, boundReceiver)
            else CallerImpl.Method.Instance(accessor)
        }
    }.createValueClassAwareCallerIfNeeded(descriptor)
}

private fun PropertyDescriptor.isJvmFieldPropertyInCompanionObject(): Boolean {
    val container = containingDeclaration
    if (!DescriptorUtils.isCompanionObject(container)) return false

    val outerClass = container.containingDeclaration
    return when {
        DescriptorUtils.isInterface(outerClass) || DescriptorUtils.isAnnotationClass(outerClass) ->
            this is DeserializedPropertyDescriptor && JvmProtoBufUtil.isMovedFromInterfaceCompanion(proto)
        else -> true
    }
}
