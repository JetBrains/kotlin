/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

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

    override val allParameters: List<KParameter> by lazy(PUBLICATION) {
        computeParameters(
            kmProperty.contextParameters, kmProperty.receiverParameterType, valueParameters = emptyList(), typeParameterTable.value,
            includeReceivers = true,
        )
    }

    override val parameters: List<KParameter> by lazy(PUBLICATION) {
        if (isBound) computeParameters(
            kmProperty.contextParameters, kmProperty.receiverParameterType, valueParameters = emptyList(), typeParameterTable.value,
            includeReceivers = false,
        )
        else allParameters
    }

    override val returnType: KType by lazy(PUBLICATION) {
        kmProperty.returnType.toKType(container.jClass.classLoader, typeParameterTable.value, if (isLocalDelegated) null else fun(): Type {
            return caller.returnType
        })
    }

    val typeParameterTable: Lazy<TypeParameterTable> = lazy(PUBLICATION) {
        val parent = (container as? KClassImpl<*>)?.typeParameterTable
        TypeParameterTable.create(kmProperty.typeParameters, parent, this, container.jClass.classLoader)
    }

    override val typeParameters: List<KTypeParameter> get() = typeParameterTable.value.ownTypeParameters

    override val visibility: KVisibility? get() = kmProperty.visibility.toKVisibility()
    override val modality: Modality get() = kmProperty.modality
    override val isSuspend: Boolean get() = false
    override val isLateinit: Boolean get() = kmProperty.isLateinit
    override val isConst: Boolean get() = kmProperty.isConst

    abstract override val getter: Getter<V>

    override val javaField: Field? by lazy(PUBLICATION) {
        if (isLocalDelegated) return@lazy null
        val fieldSignature = kmProperty.fieldSignature ?: return@lazy null
        require(container is KPackageImpl) { "javaField is only supported for top-level properties for now: $this" }
        val owner = container.jClass
        try {
            owner.getDeclaredField(fieldSignature.name)
        } catch (_: NoSuchFieldException) {
            null
        }
    }

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
            if (isLocalDelegated) {
                // Annotations on local delegated properties are present only in the metadata.
                @OptIn(ExperimentalAnnotationsInMetadata::class)
                return kmProperty.annotations.map { it.toAnnotation(container.jClass.classLoader) }
            }

            // For annotations in classes, we should also support $annotations methods in DefaultImpls, and properties in companion objects.
            require(container is KPackageImpl) { "Annotations are only supported for top-level properties for now: $this" }

            val syntheticMethod = kmProperty.syntheticMethodForAnnotations ?: return emptyList()
            val annotations = container.findMethodBySignature(syntheticMethod.name, syntheticMethod.descriptor)?.annotations?.toList()
                ?: throw KotlinReflectionInternalError("No synthetic method found: $this")
            return annotations.unwrapKotlinRepeatableAnnotations()
        }

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

        final override fun replaceContainerForFakeOverride(
            container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage,
        ): ReflectKCallable<ReturnType> =
            error("Property accessors can only be copied by copying the corresponding property")

        override val annotations: List<Annotation>
            get() =
                if (property.isLocalDelegated) emptyList()
                else (caller.member as? Method)?.annotations?.toList().orEmpty().unwrapKotlinRepeatableAnnotations()
    }

    abstract class Getter<out V> : Accessor<V, V>(), KProperty.Getter<V> {
        override val name: String get() = "<get-${property.name}>"

        override val accessor: KmPropertyAccessorAttributes?
            get() = property.kmProperty.getter

        override val allParameters: List<KParameter> get() = property.allParameters
        override val parameters: List<KParameter> get() = property.parameters

        override val returnType: KType get() = property.returnType

        override val caller: Caller<*> by lazy(PUBLICATION) {
            computeCallerForAccessor(isGetter = true)
        }

        override fun equals(other: Any?): Boolean = other is Getter<*> && property == other.property
        override fun hashCode(): Int = property.hashCode()
        override fun toString(): String = "getter of $property"
    }

    abstract class Setter<V> : Accessor<V, Unit>(), KMutableProperty.Setter<V> {
        override val name: String get() = "<set-${property.name}>"

        override val accessor: KmPropertyAccessorAttributes?
            get() = property.kmProperty.setter

        override val allParameters: List<KParameter>
            get() = property.allParameters + setterParameter.value
        override val parameters: List<KParameter>
            get() = property.parameters + setterParameter.value

        private val setterParameter: Lazy<KParameter> = lazy(PUBLICATION) {
            property.kmProperty.setterParameter?.let {
                KotlinKParameter(this, it, property.allParameters.size, KParameter.Kind.VALUE, property.typeParameterTable.value)
            } ?: DefaultSetterValueParameter(property)
        }

        override val returnType: KType get() = StandardKTypes.UNIT_RETURN_TYPE

        override val caller: Caller<*> by lazy(PUBLICATION) {
            computeCallerForAccessor(isGetter = false)
        }

        override fun equals(other: Any?): Boolean = other is Setter<*> && property == other.property
        override fun hashCode(): Int = property.hashCode()
        override fun toString(): String = "setter of $property"

        class DefaultSetterValueParameter(override val callable: KotlinKProperty<*>) : ReflectKParameter() {
            override val index: Int get() = 0
            override val name: String? get() = null
            override val type: KType get() = callable.returnType
            override val kind: KParameter.Kind get() = KParameter.Kind.VALUE
            override val isOptional: Boolean get() = false
            override val isVararg: Boolean get() = false
            override val declaresDefaultValue: Boolean get() = false

            override val annotations: List<Annotation>
                // As long as there's at least one annotation, the setter would no longer be default.
                get() = emptyList()
        }
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

    fun isJvmStaticProperty(): Boolean {
        // For class properties, we'll need to check if the synthetic `$annotations` method contains `@JvmStatic`.
        require(container is KPackageImpl) { "Only top-level properties are supported for now: $this" }
        return false
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

    val kmProperty = property.kmProperty
    val accessorSignature = if (isGetter) kmProperty.getterSignature else kmProperty.setterSignature
    val accessor = accessorSignature?.let { signature ->
        property.container.findMethodBySignature(signature.name, signature.descriptor)
    }
    return when {
        accessor == null -> {
            if (property.isUnderlyingPropertyOfValueClass() && property.visibility == KVisibility.INTERNAL) {
                val unboxMethod = property.parameters.single().type.toInlineClass()?.getInlineClassUnboxMethod(property)
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
