/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.FqNameUnsafe
import java.lang.reflect.*
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.types.FlexibleKType
import kotlin.reflect.jvm.internal.types.SimpleKType
import kotlin.reflect.jvm.internal.types.getMutableCollectionKClass
import kotlin.reflect.jvm.jvmErasure

internal fun Type.toKType(
    knownTypeParameters: Map<TypeVariable<*>, KTypeParameter>,
    nullability: TypeNullability = TypeNullability.FLEXIBLE,
    replaceNonArrayArgumentsWithStarProjections: Boolean = false,
): KType {
    val base: SimpleKType = when (this) {
        is Class<*> -> {
            if (allTypeParameters().isNotEmpty() && !replaceNonArrayArgumentsWithStarProjections) {
                return createRawJavaType(this, knownTypeParameters)
            }
            if (isArray) {
                return createJavaSimpleType(
                    this, kotlin, listOf(componentType.toKTypeProjection(knownTypeParameters)), isMarkedNullable = false,
                ).toFlexibleArrayElementVarianceType(this)
            }
            createJavaSimpleType(this, kotlin, allTypeParameters().map { KTypeProjection.STAR }, isMarkedNullable = false)
        }
        is GenericArrayType -> {
            val componentType = genericComponentType.toKTypeProjection(knownTypeParameters)
            val componentClass = componentType.type!!.jvmErasure.java.createArrayType().kotlin
            return createJavaSimpleType(this, componentClass, listOf(componentType), isMarkedNullable = false)
                .toFlexibleArrayElementVarianceType(this)
        }
        is ParameterizedType -> createJavaSimpleType(
            this,
            (rawType as Class<*>).kotlin,
            if (replaceNonArrayArgumentsWithStarProjections)
                collectAllArguments().map { KTypeProjection.STAR }
            else
                collectAllArguments().map { it.toKTypeProjection(knownTypeParameters) },
            isMarkedNullable = false,
        )
        is TypeVariable<*> ->
            createJavaSimpleType(this, findKTypeParameterInContainer(knownTypeParameters), emptyList(), isMarkedNullable = false)
        is WildcardType -> throw KotlinReflectionInternalError("Wildcard type is not possible here: $this")
        else -> throw KotlinReflectionInternalError("Type is not supported: $this (${this::class.java})")
    }

    // We cannot read `@kotlin.annotations.jvm.Mutable/ReadOnly` annotations in kotlin-reflect because they have CLASS retention.
    // Therefore, all collection types in Java are considered mutability-flexible.
    val withMutableFlexibility = run {
        val klass = base.classifier as? KClass<*>
        val mutableFqName = JavaToKotlinClassMap.readOnlyToMutable(klass?.qualifiedName?.let(::FqNameUnsafe))
        if (mutableFqName != null && klass != null) {
            val lowerBound = createJavaSimpleType(
                this, base.classifier, base.arguments, base.isMarkedNullable,
                mutableCollectionClass = getMutableCollectionKClass(mutableFqName, klass),
            )
            FlexibleKType.create(lowerBound, base, isRawType = false) { this }
        } else base
    }

    return when (nullability) {
        TypeNullability.NOT_NULL -> withMutableFlexibility
        TypeNullability.NULLABLE -> withMutableFlexibility.makeNullableAsSpecified(nullable = true)
        else -> FlexibleKType.create(
            lowerBound = withMutableFlexibility.lowerBoundIfFlexible() ?: withMutableFlexibility,
            upperBound = (withMutableFlexibility.upperBoundIfFlexible() ?: withMutableFlexibility).makeNullableAsSpecified(nullable = true),
            isRawType = false,
        ) { this }
    }
}

internal enum class TypeNullability {
    NOT_NULL,
    NULLABLE,
    FLEXIBLE,
}

private fun createJavaSimpleType(
    type: Type,
    classifier: KClassifier,
    arguments: List<KTypeProjection>,
    isMarkedNullable: Boolean,
    mutableCollectionClass: KClass<*>? = null,
): SimpleKType = SimpleKType(
    classifier, arguments, isMarkedNullable,
    annotations = emptyList(),
    abbreviation = null,
    isDefinitelyNotNullType = false,
    isNothingType = false,
    isSuspendFunctionType = false,
    mutableCollectionClass = mutableCollectionClass,
    computeJavaType = { type },
)

private fun createRawJavaType(klass: Class<*>, knownTypeParameters: Map<TypeVariable<*>, KTypeParameter>): KType =
    FlexibleKType.create(
        createJavaSimpleType(
            klass, klass.kotlin,
            klass.allTypeParameters().map { typeParameter ->
                // When creating a lower bound for a raw type, we must take the corresponding bound of each type parameter, but erase their
                // type arguments to star projections. E.g. `T : Comparable<String>` becomes `Comparable<*>`. We have to be very careful not
                // to translate the bound's own type parameters because it will lead to stack overflow in cases like `class A<T extends A>`.
                // Since a type parameter's upper bound may be another type parameter, we need to unwrap it until we end up with anything
                // but the type parameter (`Class` or `ParameterizedType`).
                // Note that this is still not exactly how the compiler translates raw types
                // (see `JavaClassifierType.toConeKotlinTypeForFlexibleBound` in K2, or `JavaTypeResolver.computeRawTypeArguments` in K1),
                // but it's a good enough approximation.
                val upperBound = generateSequence(typeParameter) { it.bounds.first() as? TypeVariable<*> }.last().bounds.first()
                KTypeProjection.invariant(upperBound.toKType(knownTypeParameters, replaceNonArrayArgumentsWithStarProjections = true))
            },
            isMarkedNullable = false,
        ),
        createJavaSimpleType(klass, klass.kotlin, klass.allTypeParameters().map { KTypeProjection.STAR }, isMarkedNullable = true),
        isRawType = true,
    ) { klass }

internal fun Class<*>.allTypeParameters(): List<TypeVariable<*>> =
    generateSequence(this) {
        if (!Modifier.isStatic(it.modifiers)) it.declaringClass else null
    }.flatMap { it.typeParameters.asSequence() }.toList()

private fun ParameterizedType.collectAllArguments(): List<Type> =
    generateSequence(this) { it.ownerType as? ParameterizedType }.flatMap { it.actualTypeArguments.toList() }.toList()

private fun Type.toKTypeProjection(knownTypeParameters: Map<TypeVariable<*>, KTypeParameter>): KTypeProjection {
    if (this !is WildcardType) {
        return KTypeProjection.invariant(toKType(knownTypeParameters))
    }

    val upperBounds = upperBounds
    val lowerBounds = lowerBounds
    if (upperBounds.size > 1 || lowerBounds.size > 1) {
        throw KotlinReflectionInternalError("Wildcard types with many bounds are not supported: $this")
    }
    return when {
        lowerBounds.size == 1 -> KTypeProjection.contravariant(lowerBounds.single().toKType(knownTypeParameters))
        upperBounds.size == 1 -> KTypeProjection.covariant(upperBounds.single().toKType(knownTypeParameters))
        else -> KTypeProjection.STAR
    }
}

private val TypeVariable<*>.kotlinContainer: KTypeParameterOwnerImpl
    get() {
        val container = genericDeclaration
        // TODO (KT-80384): support type parameters of Java callables in new implementation
        if (container !is Class<*>)
            throw KotlinReflectionInternalError("Non-class container of a type parameter is not supported: $container ($this)")
        return container.kotlin as KClassImpl<*>
    }

// The map `knownTypeParameters` is needed because when we're computing upper bounds of type parameters for a Java class, there's a moment
// when the KTypeParameter instances are already created, but not yet stored in `KClass.typeParameters`.
private fun TypeVariable<*>.findKTypeParameterInContainer(knownTypeParameters: Map<TypeVariable<*>, KTypeParameter>): KTypeParameter =
    knownTypeParameters[this]
        ?: kotlinContainer.typeParameters.singleOrNull { it.name == name }
        ?: throw KotlinReflectionInternalError("Type parameter $name is not found in $kotlinContainer")

internal fun Array<out TypeVariable<*>>.toKTypeParameters(): List<KTypeParameter> {
    val kTypeParameters = this.associateWith {
        KTypeParameterImpl(it.kotlinContainer, it.name, KVariance.INVARIANT, isReified = false)
    }
    for ((typeVariable, kTypeParameter) in kTypeParameters) {
        kTypeParameter.upperBounds = typeVariable.bounds.map { it.toKType(kTypeParameters) }
    }
    return kTypeParameters.values.toList()
}

private fun SimpleKType.toFlexibleArrayElementVarianceType(javaType: Type): FlexibleKType =
    FlexibleKType.create(
        lowerBound = this,
        upperBound = createJavaSimpleType(
            javaType, classifier, arguments.map { it.type?.let(KTypeProjection::covariant) ?: it }, isMarkedNullable = true,
        ),
        isRawType = false,
        computeJavaType = { javaType },
    ) as FlexibleKType
