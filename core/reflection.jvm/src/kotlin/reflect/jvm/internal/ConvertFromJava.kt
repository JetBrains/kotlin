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

internal fun Type.toKType(
    nullability: TypeNullability = TypeNullability.FLEXIBLE,
    replaceArgumentsWithStarProjections: Boolean = false,
): KType {
    val base: SimpleKType = when (this) {
        is Class<*> -> {
            if (typeParameters.isNotEmpty()) return createRawJavaType(this)
            createJavaSimpleType(this, kotlin, emptyList(), isMarkedNullable = false)
        }
        is GenericArrayType -> TODO()
        is ParameterizedType -> createJavaSimpleType(
            this,
            (rawType as Class<*>).kotlin,
            if (replaceArgumentsWithStarProjections)
                collectAllArguments().map { KTypeProjection.STAR }
            else
                collectAllArguments().map { it.toKTypeProjection() },
            isMarkedNullable = false,
        )
        is TypeVariable<*> -> createJavaSimpleType(this, toKTypeParameter(), emptyList(), isMarkedNullable = false)
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
            lowerBound = withMutableFlexibility,
            upperBound = withMutableFlexibility.makeNullableAsSpecified(nullable = true),
            isRawType = false,
        ) { this }
    }
}

enum class TypeNullability {
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

private fun createRawJavaType(klass: Class<*>): KType =
    FlexibleKType.create(
        createJavaSimpleType(
            klass, klass.kotlin,
            klass.typeParameters.map {
                // For a lower bound of a raw type, we must take the corresponding bound of each type parameter, but erase their type
                // arguments to star projections. E.g. `T : Comparable<String>` becomes `Comparable<*>`.
                KTypeProjection.invariant(it.bounds.first().toKType(replaceArgumentsWithStarProjections = true))
            },
            isMarkedNullable = false,
        ),
        createJavaSimpleType(klass, klass.kotlin, klass.typeParameters.map { KTypeProjection.STAR }, isMarkedNullable = true),
        isRawType = true,
    ) { klass }

private fun ParameterizedType.collectAllArguments(): List<Type> =
    generateSequence(this) { it.ownerType as? ParameterizedType }.flatMap { it.actualTypeArguments.toList() }.toList()

private fun Type.toKTypeProjection(): KTypeProjection {
    if (this !is WildcardType) {
        return KTypeProjection.invariant(toKType())
    }

    val upperBounds = upperBounds
    val lowerBounds = lowerBounds
    if (upperBounds.size > 1 || lowerBounds.size > 1) {
        throw KotlinReflectionInternalError("Wildcard types with many bounds are not supported: $this")
    }
    return when {
        lowerBounds.size == 1 -> KTypeProjection.contravariant(lowerBounds.single().toKType())
        upperBounds.size == 1 -> KTypeProjection.covariant(upperBounds.single().toKType())
        else -> KTypeProjection.STAR
    }
}

private fun TypeVariable<*>.toKTypeParameter(): KTypeParameter {
    val container = genericDeclaration
    if (container !is Class<*>)
        TODO("Non-class container of a type parameter is not supported: $container ($this)")

    return container.kotlin.typeParameters.singleOrNull { it.name == name }
        ?: throw KotlinReflectionInternalError("Type parameter $name is not found in $container")
}
