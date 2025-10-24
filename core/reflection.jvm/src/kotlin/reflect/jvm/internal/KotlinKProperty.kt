/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Field
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.*
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.ThrowingCaller

internal abstract class KotlinKProperty<out V>(
    override val container: KDeclarationContainerImpl,
    override val signature: String,
    override val rawBoundReceiver: Any?,
    val kmProperty: KmProperty,
) : KotlinKCallable<V>(), ReflectKProperty<V> {
    override val name: String get() = kmProperty.name

    override val allParameters: List<KParameter>
        get() {
            checkLocalDelegatedPropertyOrAccessor()
            return emptyList()
        }

    override val returnType: KType by lazy(PUBLICATION) {
        kmProperty.returnType.toKType(container.jClass.classLoader, typeParameterTable.value)
    }

    override val boundReceiver: Any?
        get() {
            checkLocalDelegatedPropertyOrAccessor()
            return rawBoundReceiver
        }

    private val typeParameterTable: Lazy<TypeParameterTable> = lazy(PUBLICATION) {
        checkLocalDelegatedPropertyOrAccessor()

        // Type parameters of enclosing declarations are copied as new type parameters to local delegated properties
        // (see `FirProperty.copyToFreeProperty`).
        val parent: TypeParameterTable? = null

        TypeParameterTable.create(kmProperty.typeParameters, parent = parent, this, container.jClass.classLoader)
    }

    override val typeParameters: List<KTypeParameter> get() = typeParameterTable.value.ownTypeParameters

    override val visibility: KVisibility? get() = kmProperty.visibility.toKVisibility()
    override val modality: Modality get() = kmProperty.modality
    override val isSuspend: Boolean get() = false
    override val isLateinit: Boolean get() = kmProperty.isLateinit
    override val isConst: Boolean get() = kmProperty.isConst

    abstract override val getter: Getter<V>

    override val javaField: Field?
        get() {
            checkLocalDelegatedPropertyOrAccessor()
            return null
        }

    override val caller: Caller<*> get() = getter.caller

    override val defaultCaller: Caller<*>? get() = getter.defaultCaller

    override val annotations: List<Annotation>
        get() {
            checkLocalDelegatedPropertyOrAccessor()
            // Annotations on local delegated properties are present only in the metadata.
            @OptIn(ExperimentalAnnotationsInMetadata::class)
            return kmProperty.annotations.map { it.toAnnotation(container.jClass.classLoader) }
        }

    abstract class Accessor<out PropertyType, out ReturnType> :
        KotlinKCallable<ReturnType>(), KProperty.Accessor<PropertyType>, KFunction<ReturnType> {
        abstract override val property: KotlinKProperty<PropertyType>

        abstract val accessor: KmPropertyAccessorAttributes?

        override val container: KDeclarationContainerImpl get() = property.container

        override val defaultCaller: Caller<*>? get() = null

        override val rawBoundReceiver: Any? get() = property.rawBoundReceiver

        override val typeParameters: List<KTypeParameter> get() = property.typeParameters

        override val modality: Modality get() = accessor?.modality ?: property.modality
        override val visibility: KVisibility? get() = accessor?.visibility?.toKVisibility() ?: property.visibility
        override val isInline: Boolean get() = accessor?.isInline == true
        override val isExternal: Boolean get() = accessor?.isExternal == true
        override val isOperator: Boolean get() = false
        override val isInfix: Boolean get() = false
        override val isSuspend: Boolean get() = false

        override val annotations: List<Annotation>
            get() {
                checkLocalDelegatedPropertyOrAccessor()
                return emptyList()
            }
    }

    abstract class Getter<out V> : Accessor<V, V>(), KProperty.Getter<V> {
        override val name: String get() = "<get-${property.name}>"

        override val accessor: KmPropertyAccessorAttributes?
            get() = property.kmProperty.getter

        override val allParameters: List<KParameter> get() = property.allParameters

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
            get() {
                checkLocalDelegatedPropertyOrAccessor()
                // Local delegated property setter's parameter is always default. For other properties, we'll need to use
                // `property.kmProperty.setterParameter` and convert it to `KParameter`.
                return property.allParameters + DefaultSetterValueParameter(property)
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

internal fun KotlinKProperty.Accessor<*, *>.computeCallerForAccessor(isGetter: Boolean): Caller<*> {
    checkLocalDelegatedPropertyOrAccessor()

    return ThrowingCaller
}
