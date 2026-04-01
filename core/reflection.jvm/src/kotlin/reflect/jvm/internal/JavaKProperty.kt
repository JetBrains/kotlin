/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.Modality
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.CallerImpl

internal abstract class JavaKProperty<out V>(
    container: KDeclarationContainerImpl,
    field: Field,
    rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : JavaKCallable<V>(container, field, rawBoundReceiver, overriddenStorage), ReflectKProperty<V> {
    val jField: Field get() = member as Field

    override val name: String get() = jField.name

    override val signature: String
        get() = jField.jvmSignature

    override val allParameters: List<KParameter> by lazy(PUBLICATION) {
        computeParameters(includeReceivers = true)
    }

    override val parameters: List<KParameter> by lazy(PUBLICATION) {
        if (isBound) computeParameters(includeReceivers = false)
        else allParameters
    }

    override val returnType: KType by lazy(PUBLICATION) {
        jField.genericType.toKType(emptyMap())
    }

    override val typeParameters: List<KTypeParameter> get() = emptyList()

    override val modality: Modality get() = Modality.FINAL

    override val isConst: Boolean
        get() = Modifier.isFinal(jField.modifiers) && Modifier.isStatic(jField.modifiers) &&
                (jField.type.isPrimitive || jField.type == String::class.java)

    override val isLateinit: Boolean get() = false

    override val javaField: Field? get() = jField

    abstract override val getter: Getter<V>

    override val caller: Caller<*> get() = getter.caller

    override val callerWithDefaults: Caller<*>? get() = getter.callerWithDefaults

    abstract class Accessor<out PropertyType, out ReturnType> :
        ReflectKCallableImpl<ReturnType>(KCallableOverriddenStorage.EMPTY), KProperty.Accessor<PropertyType>, KFunction<ReturnType> {
        abstract override val property: JavaKProperty<PropertyType>

        override val container: KDeclarationContainerImpl get() = property.container

        override val callerWithDefaults: Caller<*>? get() = null

        override val rawBoundReceiver: Any? get() = property.rawBoundReceiver

        override val typeParameters: List<KTypeParameter> get() = emptyList()

        override val modality: Modality get() = property.modality
        override val visibility: KVisibility? get() = property.visibility
        override val isInline: Boolean get() = false
        override val isExternal: Boolean get() = false
        override val isOperator: Boolean get() = false
        override val isInfix: Boolean get() = false
        override val isSuspend: Boolean get() = false

        override val isPackagePrivate: Boolean get() = property.isPackagePrivate

        final override fun shallowCopy(
            container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage,
        ): ReflectKCallable<ReturnType> =
            error("Property accessors can only be copied by copying the corresponding property")

        override val annotations: List<Annotation>
            get() = emptyList()
    }

    abstract class Getter<out V> : Accessor<V, V>(), KProperty.Getter<V> {
        override val name: String get() = "<get-${property.name}>"

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
        override val allParameters: List<KParameter>
            get() = property.allParameters + setterParameter.value
        override val parameters: List<KParameter>
            get() = property.parameters + setterParameter.value

        private val setterParameter: Lazy<KParameter> = lazy(PUBLICATION) {
            DefaultSetterValueParameter(property)
        }

        override val returnType: KType get() = StandardKTypes.UNIT_RETURN_TYPE

        override val caller: Caller<*> by lazy(PUBLICATION) {
            computeCallerForAccessor(isGetter = false)
        }

        override fun equals(other: Any?): Boolean = other is Setter<*> && property == other.property
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

private fun JavaKProperty<*>.computeParameters(includeReceivers: Boolean): List<KParameter> {
    require(Modifier.isStatic(jField.modifiers)) {
        "Only static properties are supported for now: $jField"
    }

    return emptyList()
}

internal val JavaKProperty.Accessor<*, *>.boundReceiver: Any?
    get() = property.boundReceiver

private fun JavaKProperty.Accessor<*, *>.computeCallerForAccessor(isGetter: Boolean): Caller<*> {
    val field = property.jField
    return when {
        !Modifier.isStatic(field.modifiers) ->
            if (isGetter)
                if (isBound) CallerImpl.FieldGetter.BoundInstance(field, boundReceiver)
                else CallerImpl.FieldGetter.Instance(field)
            else
                if (isBound) CallerImpl.FieldSetter.BoundInstance(field, notNull = false, boundReceiver)
                else CallerImpl.FieldSetter.Instance(field, notNull = false)
        else ->
            if (isGetter) CallerImpl.FieldGetter.Static(field)
            else CallerImpl.FieldSetter.Static(field, notNull = false)
    }
}
