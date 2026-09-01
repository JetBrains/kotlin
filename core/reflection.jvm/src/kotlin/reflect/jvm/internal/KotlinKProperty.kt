/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.CompanionObjectMapping
import org.jetbrains.kotlin.builtins.isMappedIntrinsicCompanionObjectClassId
import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.JvmStandardClassIds
import java.lang.reflect.*
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.*
import kotlin.metadata.jvm.*
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.calls.*

internal abstract class KotlinKProperty<out V>(
    override val container: KDeclarationContainerImpl,
    override val signature: String,
    override val rawBoundReceiver: Any?,
    val kmProperty: KmProperty,
    overriddenStorage: KCallableOverriddenStorage,
) : KotlinKCallable<V>(overriddenStorage), ReflectKProperty<V> {
    override val name: String get() = kmProperty.name

    private val extensionReceiverType: KmType? get() = kmProperty.receiverParameterType

    override val allParameters: List<KParameter> by lazy(PUBLICATION) {
        computeParameters(this, includeReceivers = true)
    }

    override val parameters: List<KParameter> by lazy(PUBLICATION) {
        if (isBound) computeParameters(this, includeReceivers = false) else allParameters
    }

    private fun computeParameters(propertyOrAccessor: KotlinKCallable<*>, includeReceivers: Boolean): List<KParameter> =
        propertyOrAccessor.computeParameters(
            kmProperty.contextParameters, extensionReceiverType, valueParameters = emptyList(), typeParameterTable.value, includeReceivers,
        )

    override val returnType: KType by lazy(PUBLICATION) {
        substituteType(
            kmProperty.returnType.toKType(
                container.jClass.safeClassLoader, typeParameterTable.value,
                computeJavaType = if (isLocalDelegated) null else fun(): Type = caller.returnType,
            )
        )
    }

    val typeParameterTable: Lazy<TypeParameterTable> = lazy(PUBLICATION) {
        val parent = (originalContainer as? KClassImpl<*>)?.typeParameterTable
        TypeParameterTable.create(kmProperty.typeParameters, parent, this, container.jClass.safeClassLoader)
    }

    override val typeParameters: List<KTypeParameter> get() = typeParameterTable.value.ownTypeParameters

    override val visibility: KVisibility? get() = kmProperty.visibility.toKVisibility()
    override val modality: Modality get() = kmProperty.modality
    override val isSuspend: Boolean get() = false
    override val isLateinit: Boolean get() = kmProperty.isLateinit
    override val isConst: Boolean get() = kmProperty.isConst

    @OptIn(ExperimentalCompanionBlocksAndExtensions::class)
    @ExperimentalCompanionExtensions
    override val companionExtensionClass: KClass<*>?
        get() = (kmProperty.companionExtensionReceiverType?.classifier as KmClassifier.Class?)?.let {
            container.jClass.safeClassLoader.loadKClass(it.name)
        }

    abstract override val getter: Getter<V>

    override val javaField: Field? by lazy(PUBLICATION) {
        if (isLocalDelegated) return@lazy null
        val fieldSignature = kmProperty.fieldSignature ?: return@lazy null
        val owner =
            if (kmProperty.isMovedFromInterfaceCompanion || isPropertyWithBackingFieldInOuterClass())
                originalContainer.jClass.enclosingClass
            else
                originalContainer.jClass
        try {
            owner.getDeclaredField(fieldSignature.name)
        } catch (_: NoSuchFieldException) {
            null
        }
    }

    private fun isPropertyWithBackingFieldInOuterClass(): Boolean {
        val container = container as? KClassImpl<*> ?: return false
        return overriddenStorage == KCallableOverriddenStorage.EMPTY &&
                isClassCompanionObjectWithBackingFieldsInOuter(container)
    }

    private fun isClassCompanionObjectWithBackingFieldsInOuter(klass: KClassImpl<*>): Boolean =
        klass.isCompanion && klass.java.enclosingClass.kotlin.isClassOrEnumClass &&
                !CompanionObjectMapping.isMappedIntrinsicCompanionObjectClassId(klass.classId)

    private val KClass<*>.isClassOrEnumClass: Boolean
        get() = this is KClassImpl<*> && (classKind == ClassKind.CLASS || classKind == ClassKind.ENUM_CLASS)

    protected fun computeDelegateSource(): Member? {
        if (!kmProperty.isDelegated) return null
        val method = kmProperty.syntheticMethodForDelegate
        if (method != null) {
            return container.findMethodBySignature(method.name, method.descriptor)
        }
        return javaField
    }

    override val caller: Caller<*> get() = getter.caller

    override val callerWithDefaults: Caller<*>? get() = getter.callerWithDefaults

    override val annotations: List<Annotation>
        get() {
            if (isLocalDelegated || container.jClass.isAnnotation) {
                // Annotations on local delegated properties and annotation constructor properties are present only in the metadata.
                return kmProperty.annotations.map { it.toAnnotation(container.jClass.safeClassLoader) }
            }

            val container = originalContainer
            val annotationContainer = if ((container as? KClassImpl<*>)?.classKind == ClassKind.INTERFACE) {
                container.jClass.classes.firstOrNull { it.simpleName == JvmAbi.DEFAULT_IMPLS_CLASS_NAME }
                    ?.kotlin as KDeclarationContainerImpl? ?: container
            } else container
            val syntheticMethod = kmProperty.syntheticMethodForAnnotations ?: return emptyList()
            val annotations = annotationContainer.findMethodBySignature(syntheticMethod.name, syntheticMethod.descriptor)
                ?.annotations?.toList()
                ?: throw KotlinReflectionInternalError("No synthetic method found: $this")
            return annotations.unwrapKotlinRepeatableAnnotations()
        }

    @OptIn(ExperimentalCompanionBlocksAndExtensions::class)
    override val isCompanionBlockMember: Boolean
        get() = container is KClassImpl<*> && kmProperty.isStatic

    abstract class Accessor<out PropertyType, out ReturnType> :
        KotlinKCallable<ReturnType>(KCallableOverriddenStorage.EMPTY), KProperty.Accessor<PropertyType>, KFunction<ReturnType> {
        abstract override val property: KotlinKProperty<PropertyType>

        abstract val accessor: KmPropertyAccessorAttributes?

        override val container: KDeclarationContainerImpl get() = property.container

        override val callerWithDefaults: Caller<*>? get() = null

        override val rawBoundReceiver: Any? get() = property.rawBoundReceiver

        override val typeParameters: List<KTypeParameter> get() = property.typeParameters

        override val modality: Modality get() = accessor?.modality ?: property.modality
        override val visibility: KVisibility? get() = accessor?.visibility?.toKVisibility() ?: property.visibility
        override val isInline: Boolean get() = accessor?.isInline == true
        override val isExternal: Boolean get() = accessor?.isExternal == true
        override val isOperator: Boolean get() = false
        override val isInfix: Boolean get() = false
        override val isSuspend: Boolean get() = false

        override val isCompanionBlockMember: Boolean get() = property.isCompanionBlockMember

        @ExperimentalCompanionExtensions
        override val companionExtensionClass: KClass<*>? get() = property.companionExtensionClass

        final override fun shallowCopy(
            container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage,
        ): ReflectKCallable<ReturnType> =
            error("Property accessors can only be copied by copying the corresponding property")

        override fun rebind(boundReceiver: Any?): ReflectKCallable<ReturnType> =
            error("Property accessors can only be bound by copying the corresponding property")

        override val annotations: List<Annotation>
            get() =
                if (property.isLocalDelegated) emptyList()
                else (caller.member as? Method)?.annotations?.toList().orEmpty().unwrapKotlinRepeatableAnnotations()
    }

    abstract class Getter<out V> : Accessor<V, V>(), KProperty.Getter<V> {
        override val name: String get() = "<get-${property.name}>"

        override val accessor: KmPropertyAccessorAttributes?
            get() = property.kmProperty.getter

        override val allParameters: List<KParameter> by lazy(PUBLICATION) {
            property.computeParameters(this, includeReceivers = true)
        }
        override val parameters: List<KParameter> by lazy(PUBLICATION) {
            if (isBound) property.computeParameters(this, includeReceivers = false) else allParameters
        }

        override val returnType: KType get() = property.returnType

        override val caller: Caller<*> by lazy(PUBLICATION) {
            computeCallerForAccessor(isGetter = true)
        }

        override fun equals(other: Any?): Boolean = other is KProperty.Getter<*> && property == other.property
        override fun hashCode(): Int = property.hashCode()
        override fun toString(): String = "getter of $property"
    }

    abstract class Setter<V> : Accessor<V, Unit>(), KMutableProperty.Setter<V> {
        override val name: String get() = "<set-${property.name}>"

        override val accessor: KmPropertyAccessorAttributes?
            get() = property.kmProperty.setter

        override val allParameters: List<KParameter> by lazy(PUBLICATION) {
            val propertyParameters = property.computeParameters(this, includeReceivers = true)
            propertyParameters + createSetterParameter(propertyParameters.size)
        }
        override val parameters: List<KParameter> by lazy(PUBLICATION) {
            if (isBound) {
                val propertyParameters = property.computeParameters(this, includeReceivers = false)
                propertyParameters + createSetterParameter(propertyParameters.size)
            } else allParameters
        }

        private fun createSetterParameter(index: Int): KParameter =
            property.kmProperty.setterParameter?.let {
                KotlinKParameter(this, it, index, KParameter.Kind.VALUE, property.typeParameterTable.value)
            } ?: DefaultSetterValueParameter(property, index)

        override val returnType: KType get() = StandardKTypes.UNIT_RETURN_TYPE

        override val caller: Caller<*> by lazy(PUBLICATION) {
            computeCallerForAccessor(isGetter = false)
        }

        override fun equals(other: Any?): Boolean = other is KMutableProperty.Setter<*> && property == other.property
        override fun hashCode(): Int = property.hashCode()
        override fun toString(): String = "setter of $property"
    }

    override fun equals(other: Any?): Boolean {
        val that = other.asReflectProperty() ?: return false
        return container == that.container && name == that.name && signature == that.signature && rawBoundReceiver == that.rawBoundReceiver
    }

    override fun hashCode(): Int =
        (container.hashCode() * 31 + name.hashCode()) * 31 + signature.hashCode()

    override fun toString(): String =
        ReflectionObjectRenderer.renderProperty(this)
}

internal val KotlinKProperty.Accessor<*, *>.boundReceiver: Any?
    get() = property.boundReceiver

internal fun KotlinKProperty.Accessor<*, *>.computeCallerForAccessor(isGetter: Boolean): Caller<*> {
    val property = property
    if (property.isLocalDelegated) return ThrowingCaller
    val kmProperty = property.kmProperty

    fun isJvmStaticProperty(): Boolean =
        container is KClassImpl<*> && kmProperty.annotations.any {
            it.className == JvmStandardClassIds.Annotations.JvmStatic.asString()
        }

    fun isNotNullProperty(): Boolean =
        !property.returnType.isNullableType()

    fun computeFieldCaller(field: Field): CallerImpl<Field> = when {
        property.isJvmFieldPropertyInCompanionObject() || !Modifier.isStatic(field.modifiers) ->
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

    val accessorSignature = when {
        isGetter -> kmProperty.getterSignature ?: run {
            // If both getter and field signatures are absent, it's a builtin property, so we need to compute the signature and use
            // the accessor only (which must be getter, as there are no builtin mutable properties so far).
            if (kmProperty.fieldSignature == null) kmProperty.computeJvmSignature(property.container) else null
        }
        else -> kmProperty.setterSignature
    }
    val accessor = accessorSignature?.let { signature ->
        property.container.findMethodBySignature(signature.name, signature.descriptor)
    }
    return when {
        accessor == null -> {
            if (property.isUnderlyingPropertyOfValueClass() && property.visibility == KVisibility.INTERNAL) {
                val unboxMethod = property.allParameters.single().type.toInlineClass()?.getInlineClassUnboxMethod(property)
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
            if (isBound) CallerImpl.Method.BoundStatic(accessor, isCallByToValueClassMangledMethod = false, boundReceiver)
            else CallerImpl.Method.Static(accessor)
    }.createValueClassAwareCallerIfNeeded(this, isDefault = false, forbidUnboxingForIndices = emptyList())
}

private fun KotlinKProperty<*>.isJvmFieldPropertyInCompanionObject(): Boolean {
    val container = container
    if (container !is KClassImpl<*> || container.classKind != ClassKind.COMPANION_OBJECT) return false

    val outerClass = container.java.enclosingClass.kotlin as? KClassImpl<*> ?: return false
    return when {
        outerClass.classKind == ClassKind.INTERFACE || outerClass.classKind == ClassKind.ANNOTATION_CLASS ->
            kmProperty.isMovedFromInterfaceCompanion
        else -> true
    }
}
