/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType.METHOD_RETURN_TYPE
import java.lang.reflect.Field
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.Modality
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.JavaKProperty.AccessorBase
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.types.AbstractKType

/**
 * A special instance of KProperty which is created in Java classes, when get-/set-methods override property accessors from a Kotlin class:
 *
 *     // FILE: A.kt
 *     abstract class A {
 *         abstract val member: A?
 *     }
 *     // FILE: X.java
 *     public interface X {
 *         X getMember();
 *     }
 *
 * In this example, `X` contains a `JavaForKotlinOverrideKProperty` with return type `X?`.
 *
 * Note that there are numerous problems with these properties in the old K1-based implementation, so they aren't fully supported in the
 * new implementation either: KT-87863. In particular, only **non-extension non-contextual properties** are supported.
 */
internal abstract class JavaForKotlinOverrideKProperty<out V>(
    override val container: KDeclarationContainerImpl,
    override val rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
    protected val getterMethod: ReflectKFunction,
    protected val setterMethod: ReflectKFunction?,
    protected val overriddenProperty: ReflectKProperty<*>,
) : ReflectKCallableImpl<V>(overriddenStorage), ReflectKProperty<V> {
    override val signature: String get() = overriddenProperty.signature
    override val name: String get() = overriddenProperty.name

    override val visibility: KVisibility? get() = getterMethod.visibility
    override val modality: Modality get() = getterMethod.modality
    override val isSuspend: Boolean get() = overriddenProperty.isSuspend
    override val isLateinit: Boolean get() = overriddenProperty.isLateinit
    override val isConst: Boolean get() = overriddenProperty.isConst
    override val isPackagePrivate: Boolean get() = getterMethod.isPackagePrivate

    @ExperimentalCompanionExtensions
    final override val companionExtensionClass: KClass<*>? get() = null

    override val allParameters: List<KParameter> by lazy(PUBLICATION) {
        computeParameters(this, includeReceivers = true)
    }

    override val parameters: List<KParameter> by lazy(PUBLICATION) {
        if (isBound) computeParameters(this, includeReceivers = false)
        else allParameters
    }

    override val returnType: KType by lazy(PUBLICATION) {
        with(ReflectSignatureParts(METHOD_RETURN_TYPE)) {
            val originalReturnType = getterMethod.returnType as AbstractKType
            val qualifiers = originalReturnType.computeIndexedQualifiers(
                listOf(overriddenProperty.returnType as AbstractKType), null,
            )
            substituteType(originalReturnType.enhance(qualifiers))
        }
    }

    override val typeParameters: List<KTypeParameter>
        // TODO (KT-87863): support generic member extension / contextual properties.
        get() = emptyList()

    override val annotations: List<Annotation> get() = emptyList()

    override val javaField: Field? get() = null

    override val caller: Caller<*> get() = getterMethod.caller
    override val callerWithDefaults: Caller<*>? get() = getterMethod.callerWithDefaults

    interface Accessor<out PropertyType, out ReturnType> : AccessorBase<PropertyType, ReturnType> {
        abstract override val property: JavaForKotlinOverrideKProperty<PropertyType>
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

        override val caller: Caller<*> get() = property.getterMethod.caller

        override val annotations: List<Annotation> get() = property.getterMethod.annotations

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

        override val caller: Caller<*> get() = property.setterMethod!!.caller

        override val annotations: List<Annotation> get() = property.setterMethod!!.annotations

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

internal fun JavaForKotlinOverrideKProperty<*>.computeParameters(
    propertyOrAccessor: ReflectKCallable<*>, includeReceivers: Boolean,
): List<KParameter> {
    if (!includeReceivers) return emptyList()
    return listOf(InstanceParameter(propertyOrAccessor, container as KClass<*>))
}
