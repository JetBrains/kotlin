/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.types.TypeUtils
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.IllegalPropertyDelegateAccessException
import kotlin.reflect.jvm.internal.JvmPropertySignature.*
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.CallerImpl
import kotlin.reflect.jvm.internal.calls.ThrowingCaller

internal abstract class KPropertyImpl<out R> private constructor(
    override val container: KDeclarationContainerImpl,
    override val name: String,
    val signature: String,
    descriptorInitialValue: PropertyDescriptor?,
    val boundReceiver: Any?
) : KCallableImpl<R>(), KProperty<R> {
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

    override val isBound: Boolean get() = boundReceiver != CallableReference.NO_RECEIVER

    private val _javaField = ReflectProperties.lazySoft {
        val jvmSignature = RuntimeTypeMapper.mapPropertySignature(descriptor)
        when (jvmSignature) {
            is KotlinProperty -> {
                val descriptor = jvmSignature.descriptor
                JvmProtoBufUtil.getJvmFieldSignature(jvmSignature.proto, jvmSignature.nameResolver, jvmSignature.typeTable)?.let {
                    val owner = if (JvmAbi.isPropertyWithBackingFieldInOuterClass(descriptor) ||
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

    val javaField: Field? get() = _javaField()

    protected fun computeDelegateField(): Field? =
        if (@Suppress("DEPRECATION") descriptor.isDelegated) javaField else null

    protected fun getDelegate(field: Field?, receiver: Any?): Any? =
        try {
            if (receiver === EXTENSION_PROPERTY_DELEGATE) {
                if (descriptor.extensionReceiverParameter == null) {
                    throw RuntimeException(
                        "'$this' is not an extension property and thus getExtensionDelegate() " +
                                "is not going to work, use getDelegate() instead"
                    )
                }
            }
            field?.get(receiver)
        } catch (e: IllegalAccessException) {
            throw IllegalPropertyDelegateAccessException(e)
        }

    abstract override val getter: Getter<R>

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
        return container == that.container && name == that.name && signature == that.signature && boundReceiver == that.boundReceiver
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

    abstract class Getter<out R> : Accessor<R, R>(), KProperty.Getter<R> {
        override val name: String get() = "<get-${property.name}>"

        override val descriptor: PropertyGetterDescriptor by ReflectProperties.lazySoft {
            // TODO: default getter created this way won't have any source information
            property.descriptor.getter ?: DescriptorFactory.createDefaultGetter(property.descriptor, Annotations.EMPTY)
        }

        override val caller: Caller<*> by ReflectProperties.lazySoft {
            computeCallerForAccessor(isGetter = true)
        }
    }

    abstract class Setter<R> : Accessor<R, Unit>(), KMutableProperty.Setter<R> {
        override val name: String get() = "<set-${property.name}>"

        override val descriptor: PropertySetterDescriptor by ReflectProperties.lazySoft {
            // TODO: default setter created this way won't have any source information
            property.descriptor.setter ?: DescriptorFactory.createDefaultSetter(property.descriptor, Annotations.EMPTY, Annotations.EMPTY)
        }

        override val caller: Caller<*> by ReflectProperties.lazySoft {
            computeCallerForAccessor(isGetter = false)
        }
    }

    companion object {
        val EXTENSION_PROPERTY_DELEGATE = Any()
    }
}


private fun KPropertyImpl.Accessor<*, *>.computeCallerForAccessor(isGetter: Boolean): Caller<*> {
    if (KDeclarationContainerImpl.LOCAL_PROPERTY_SIGNATURE.matches(property.signature)) {
        return ThrowingCaller
    }

    fun isInsideClassCompanionObject(): Boolean {
        val possibleCompanionObject = property.descriptor.containingDeclaration
        return DescriptorUtils.isCompanionObject(possibleCompanionObject) &&
                !DescriptorUtils.isInterface(possibleCompanionObject.containingDeclaration)
    }

    fun isInsideJvmInterfaceCompanionObject(): Boolean {
        val possibleCompanionObject = property.descriptor.containingDeclaration
        return DescriptorUtils.isCompanionObject(possibleCompanionObject) &&
                (DescriptorUtils.isInterface(possibleCompanionObject.containingDeclaration) ||
                        DescriptorUtils.isAnnotationClass(possibleCompanionObject.containingDeclaration))
    }

    fun isInsideInterfaceCompanionObjectWithJvmField(): Boolean {
        val propertyDescriptor = property.descriptor
        if (propertyDescriptor !is DeserializedPropertyDescriptor || !isInsideJvmInterfaceCompanionObject()) return false
        return JvmProtoBufUtil.isMovedFromInterfaceCompanion(propertyDescriptor.proto)
    }

    fun isJvmStaticProperty() =
        property.descriptor.annotations.findAnnotation(JVM_STATIC) != null

    fun isNotNullProperty() =
        !TypeUtils.isNullableType(property.descriptor.type)

    fun computeFieldCaller(field: Field): Caller<Field> = when {
        isInsideClassCompanionObject() || isInsideInterfaceCompanionObjectWithJvmField() -> {
            val klass = (descriptor.containingDeclaration as ClassDescriptor).toJavaClass()!!
            if (isGetter)
                if (isBound) CallerImpl.BoundClassCompanionFieldGetter(field, klass)
                else CallerImpl.ClassCompanionFieldGetter(field, klass)
            else
                if (isBound) CallerImpl.BoundClassCompanionFieldSetter(field, klass)
                else CallerImpl.ClassCompanionFieldSetter(field, klass)
        }
        !Modifier.isStatic(field.modifiers) ->
            if (isGetter)
                if (isBound) CallerImpl.BoundInstanceFieldGetter(field, property.boundReceiver)
                else CallerImpl.InstanceFieldGetter(field)
            else
                if (isBound) CallerImpl.BoundInstanceFieldSetter(field, isNotNullProperty(), property.boundReceiver)
                else CallerImpl.InstanceFieldSetter(field, isNotNullProperty())
        isJvmStaticProperty() ->
            if (isGetter)
                if (isBound) CallerImpl.BoundJvmStaticInObjectFieldGetter(field)
                else CallerImpl.JvmStaticInObjectFieldGetter(field)
            else
                if (isBound) CallerImpl.BoundJvmStaticInObjectFieldSetter(field, isNotNullProperty())
                else CallerImpl.JvmStaticInObjectFieldSetter(field, isNotNullProperty())
        else ->
            if (isGetter) CallerImpl.StaticFieldGetter(field)
            else CallerImpl.StaticFieldSetter(field, isNotNullProperty())
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
                    jvmSignature.nameResolver.getString(signature.desc),
                    descriptor.isPublicInBytecode
                )
            }

            when {
                accessor == null -> computeFieldCaller(
                    property.javaField ?: throw KotlinReflectionInternalError("No accessors or field is found for property $property")
                )
                !Modifier.isStatic(accessor.modifiers) ->
                    if (isBound) CallerImpl.BoundInstanceMethod(accessor, property.boundReceiver)
                    else CallerImpl.InstanceMethod(accessor)
                isJvmStaticProperty() ->
                    if (isBound) CallerImpl.BoundJvmStaticInObject(accessor)
                    else CallerImpl.JvmStaticInObject(accessor)
                else ->
                    if (isBound) CallerImpl.BoundStaticMethod(accessor, property.boundReceiver)
                    else CallerImpl.StaticMethod(accessor)
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
            if (isBound) CallerImpl.BoundInstanceMethod(method, property.boundReceiver)
            else CallerImpl.InstanceMethod(method)
        }
        is MappedKotlinProperty -> {
            val signature =
                if (isGetter) jvmSignature.getterSignature
                else (jvmSignature.setterSignature ?: throw KotlinReflectionInternalError("No setter found for property $property"))
            val accessor = property.container.findMethodBySignature(
                signature.methodName, signature.methodDesc, descriptor.isPublicInBytecode
            ) ?: throw KotlinReflectionInternalError("No accessor found for property $property")

            assert(!Modifier.isStatic(accessor.modifiers)) { "Mapped property cannot have a static accessor: $property" }

            return if (isBound) CallerImpl.BoundInstanceMethod(accessor, property.boundReceiver)
            else CallerImpl.InstanceMethod(accessor)
        }
    }
}
