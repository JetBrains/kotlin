/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Member
import java.lang.reflect.Modifier
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.Modality
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.calls.Caller

internal abstract class JavaKProperty<out V>(
    container: KDeclarationContainerImpl,
    member: Member,
    rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : JavaKCallable<V>(container, member, rawBoundReceiver, overriddenStorage), ReflectKProperty<V> {
    override val name: String get() = member.name

    override val allParameters: List<KParameter> by lazy(PUBLICATION) {
        computeParameters(this, includeReceivers = true)
    }

    override val parameters: List<KParameter> by lazy(PUBLICATION) {
        if (isBound) computeParameters(this, includeReceivers = false)
        else allParameters
    }

    override val typeParameters: List<KTypeParameter> get() = emptyList()

    override val modality: Modality get() = Modality.FINAL

    override val isLateinit: Boolean get() = false

    abstract override val getter: Getter<V>

    override val caller: Caller<*> get() = getter.caller

    override val callerWithDefaults: Caller<*>? get() = getter.callerWithDefaults

    interface AccessorBase<out PropertyType, out ReturnType> :
        ReflectKCallable<ReturnType>, KProperty.Accessor<PropertyType>, KFunction<ReturnType> {
        abstract override val property: ReflectKProperty<PropertyType>

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

        override fun shallowCopy(
            container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage,
        ): ReflectKCallable<ReturnType> =
            error("Property accessors can only be copied by copying the corresponding property")

        override fun rebind(boundReceiver: Any?): ReflectKCallable<ReturnType> =
            error("Property accessors can only be bound by copying the corresponding property")

        override val annotations: List<Annotation>
            get() = emptyList()

        @ExperimentalCompanionExtensions
        override val companionExtensionClass: KClass<*>?
            get() = null
    }

    interface Accessor<out PropertyType, out ReturnType> : AccessorBase<PropertyType, ReturnType> {
        abstract override val property: JavaKProperty<PropertyType>
    }

    abstract class Getter<out V> : ReflectKCallableImpl<V>(KCallableOverriddenStorage.EMPTY), Accessor<V, V>, KProperty.Getter<V> {
        override val name: String get() = "<get-${property.name}>"

        override val allParameters: List<KParameter> by lazy(PUBLICATION) {
            property.computeParameters(this, includeReceivers = true)
        }

        override val parameters: List<KParameter> by lazy(PUBLICATION) {
            if (isBound) property.computeParameters(this, includeReceivers = false)
            else allParameters
        }

        override val returnType: KType get() = property.returnType

        override fun equals(other: Any?): Boolean = other is KProperty.Getter<*> && property == other.property
        override fun hashCode(): Int = property.hashCode()
        override fun toString(): String = "getter of $property"
    }

    abstract class Setter<V> : ReflectKCallableImpl<Unit>(KCallableOverriddenStorage.EMPTY), Accessor<V, Unit>, KMutableProperty.Setter<V> {
        override val name: String get() = "<set-${property.name}>"

        override val allParameters: List<KParameter> by lazy(PUBLICATION) {
            val propertyParameters = property.computeParameters(this, includeReceivers = true)
            propertyParameters + DefaultSetterValueParameter(property, propertyParameters.size)
        }

        override val parameters: List<KParameter> by lazy(PUBLICATION) {
            if (isBound) {
                val propertyParameters = property.computeParameters(this, includeReceivers = false)
                propertyParameters + DefaultSetterValueParameter(property, propertyParameters.size)
            } else allParameters
        }
        override val returnType: KType get() = StandardKTypes.UNIT_RETURN_TYPE

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

internal fun JavaKProperty<*>.computeParameters(propertyOrAccessor: ReflectKCallable<*>, includeReceivers: Boolean): List<KParameter> {
    if (Modifier.isStatic(member.modifiers) || !includeReceivers) return emptyList()
    return listOf(InstanceParameter(propertyOrAccessor, container as KClass<*>))
}

internal val JavaKProperty.Accessor<*, *>.boundReceiver: Any?
    get() = property.boundReceiver
