/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.model

import org.jetbrains.kotlin.types.AbstractTypeCheckerContext

interface KotlinTypeMarker
interface TypeArgumentMarker
interface TypeConstructorMarker
interface TypeParameterMarker

interface SimpleTypeMarker : KotlinTypeMarker
interface CapturedTypeMarker : SimpleTypeMarker
interface DefinitelyNotNullTypeMarker : SimpleTypeMarker

interface FlexibleTypeMarker : KotlinTypeMarker
interface DynamicTypeMarker : FlexibleTypeMarker
interface RawTypeMarker : FlexibleTypeMarker
interface StubTypeMarker : SimpleTypeMarker

interface TypeArgumentListMarker

interface TypeVariableMarker

interface TypeSubstitutorMarker


enum class TypeVariance(val presentation: String) {
    IN("in"),
    OUT("out"),
    INV("");

    override fun toString(): String = presentation
}


interface TypeSystemOptimizationContext {
    /**
     *  @return true is a.arguments == b.arguments, or false if not supported
     */
    fun identicalArguments(a: SimpleTypeMarker, b: SimpleTypeMarker) = false
}

interface TypeSystemBuiltInsContext {
    fun nullableNothingType(): SimpleTypeMarker
    fun nullableAnyType(): SimpleTypeMarker
    fun nothingType(): SimpleTypeMarker
}

interface TypeSystemTypeFactoryContext {
    fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): KotlinTypeMarker
    fun createSimpleType(constructor: TypeConstructorMarker, arguments: List<TypeArgumentMarker>, nullable: Boolean): SimpleTypeMarker
    fun createTypeArgument(type: KotlinTypeMarker, variance: TypeVariance): TypeArgumentMarker
    fun createStarProjection(typeParameter: TypeParameterMarker): TypeArgumentMarker
}


interface TypeCheckerProviderContext {
    fun newBaseTypeCheckerContext(errorTypesEqualToAnything: Boolean): AbstractTypeCheckerContext
}

interface TypeSystemCommonSuperTypesContext : TypeSystemContext, TypeSystemTypeFactoryContext, TypeCheckerProviderContext {

    fun KotlinTypeMarker.anySuperTypeConstructor(predicate: (TypeConstructorMarker) -> Boolean) =
        newBaseTypeCheckerContext(false).anySupertype(lowerBoundIfFlexible(), {
            predicate(it.typeConstructor())
        }, { AbstractTypeCheckerContext.SupertypesPolicy.LowerIfFlexible })

    fun KotlinTypeMarker.canHaveUndefinedNullability(): Boolean

    fun SimpleTypeMarker.typeDepth(): Int
    fun KotlinTypeMarker.typeDepth(): Int

    fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker?
}

interface TypeSystemInferenceExtensionContextDelegate : TypeSystemInferenceExtensionContext

interface TypeSystemInferenceExtensionContext : TypeSystemContext, TypeSystemBuiltInsContext, TypeSystemCommonSuperTypesContext {
    fun KotlinTypeMarker.contains(predicate: (KotlinTypeMarker) -> Boolean): Boolean

    fun TypeConstructorMarker.isUnitTypeConstructor(): Boolean

    fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): KotlinTypeMarker

    fun Collection<KotlinTypeMarker>.singleBestRepresentative(): KotlinTypeMarker?

    fun KotlinTypeMarker.isUnit(): Boolean

    fun KotlinTypeMarker.withNullability(nullable: Boolean): KotlinTypeMarker


    fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(): KotlinTypeMarker
    fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker

    fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<KotlinTypeMarker>,
        lowerType: KotlinTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker

    fun createStubType(typeVariable: TypeVariableMarker): StubTypeMarker


    fun KotlinTypeMarker.removeAnnotations(): KotlinTypeMarker

    fun SimpleTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): SimpleTypeMarker

    fun KotlinTypeMarker.hasExactAnnotation(): Boolean
    fun KotlinTypeMarker.hasNoInferAnnotation(): Boolean

    fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker


    fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker

    fun KotlinTypeMarker.isNullableType(): Boolean

    fun KotlinTypeMarker.isNullableAny() = this.typeConstructor().isAnyConstructor() && this.isNullableType()
    fun KotlinTypeMarker.isNothing() = this.typeConstructor().isNothingConstructor() && !this.isNullableType()
    fun KotlinTypeMarker.isNullableNothing() = this.typeConstructor().isNothingConstructor() && this.isNullableType()

    fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker

    fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker

    fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker


    fun TypeVariableMarker.defaultType(): SimpleTypeMarker
}


class ArgumentList(initialSize: Int) : ArrayList<TypeArgumentMarker>(initialSize), TypeArgumentListMarker


interface TypeSystemContext : TypeSystemOptimizationContext {
    fun KotlinTypeMarker.asSimpleType(): SimpleTypeMarker?
    fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker?

    fun KotlinTypeMarker.isError(): Boolean

    fun FlexibleTypeMarker.asDynamicType(): DynamicTypeMarker?

    fun FlexibleTypeMarker.asRawType(): RawTypeMarker?
    fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker

    fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker
    fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker?

    fun KotlinTypeMarker.isCapturedType() = asSimpleType()?.asCapturedType() != null

    fun SimpleTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker?
    fun SimpleTypeMarker.isMarkedNullable(): Boolean
    fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker
    fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker

    fun SimpleTypeMarker.argumentsCount(): Int
    fun SimpleTypeMarker.getArgument(index: Int): TypeArgumentMarker

    fun SimpleTypeMarker.getArgumentOrNull(index: Int): TypeArgumentMarker? {
        if (index in 0 until argumentsCount()) return getArgument(index)
        return null
    }

    fun SimpleTypeMarker.isStubType(): Boolean

    fun KotlinTypeMarker.asTypeArgument(): TypeArgumentMarker

    fun CapturedTypeMarker.lowerType(): KotlinTypeMarker?

    fun TypeArgumentMarker.isStarProjection(): Boolean
    fun TypeArgumentMarker.getVariance(): TypeVariance
    fun TypeArgumentMarker.getType(): KotlinTypeMarker

    fun TypeConstructorMarker.parametersCount(): Int
    fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker
    fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker>
    fun TypeConstructorMarker.isIntersection(): Boolean
    fun TypeConstructorMarker.isClassTypeConstructor(): Boolean
    fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean

    fun TypeParameterMarker.getVariance(): TypeVariance
    fun TypeParameterMarker.upperBoundCount(): Int
    fun TypeParameterMarker.getUpperBound(index: Int): KotlinTypeMarker
    fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker

    fun isEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean

    fun TypeConstructorMarker.isDenotable(): Boolean

    fun KotlinTypeMarker.lowerBoundIfFlexible(): SimpleTypeMarker = this.asFlexibleType()?.lowerBound() ?: this.asSimpleType()!!
    fun KotlinTypeMarker.upperBoundIfFlexible(): SimpleTypeMarker = this.asFlexibleType()?.upperBound() ?: this.asSimpleType()!!

    fun KotlinTypeMarker.isDynamic(): Boolean = asFlexibleType()?.asDynamicType() != null
    fun KotlinTypeMarker.isDefinitelyNotNullType(): Boolean = asSimpleType()?.asDefinitelyNotNullType() != null

    fun KotlinTypeMarker.hasFlexibleNullability() =
        lowerBoundIfFlexible().isMarkedNullable() != upperBoundIfFlexible().isMarkedNullable()

    fun KotlinTypeMarker.typeConstructor(): TypeConstructorMarker =
        (asSimpleType() ?: lowerBoundIfFlexible()).typeConstructor()

    fun SimpleTypeMarker.isClassType(): Boolean = typeConstructor().isClassTypeConstructor()

    fun SimpleTypeMarker.isIntegerLiteralType(): Boolean = typeConstructor().isIntegerLiteralTypeConstructor()

    fun SimpleTypeMarker.possibleIntegerTypes(): Collection<KotlinTypeMarker>

    fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean

    fun captureFromArguments(
        type: SimpleTypeMarker,
        status: CaptureStatus
    ): SimpleTypeMarker?

    fun SimpleTypeMarker.asArgumentList(): TypeArgumentListMarker

    operator fun TypeArgumentListMarker.get(index: Int): TypeArgumentMarker {
        return when (this) {
            is SimpleTypeMarker -> getArgument(index)
            is ArgumentList -> get(index)
            else -> error("unknown type argument list type: $this, ${this::class}")
        }
    }

    fun TypeArgumentListMarker.size(): Int {
        return when (this) {
            is SimpleTypeMarker -> argumentsCount()
            is ArgumentList -> size
            else -> error("unknown type argument list type: $this, ${this::class}")
        }
    }

    fun TypeConstructorMarker.isAnyConstructor(): Boolean
    fun TypeConstructorMarker.isNothingConstructor(): Boolean

    fun KotlinTypeMarker.isNotNullNothing(): Boolean

    /**
     *
     * SingleClassifierType is one of the following types:
     *  - classType
     *  - type for type parameter
     *  - captured type
     *
     * Such types can contains error types in our arguments, but type constructor isn't errorTypeConstructor
     */
    fun SimpleTypeMarker.isSingleClassifierType(): Boolean

    fun intersectTypes(types: List<KotlinTypeMarker>): KotlinTypeMarker
    fun intersectTypes(types: List<SimpleTypeMarker>): SimpleTypeMarker

    fun KotlinTypeMarker.isSimpleType() = asSimpleType() != null

    fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker
}

enum class CaptureStatus {
    FOR_SUBTYPING,
    FOR_INCORPORATION,
    FROM_EXPRESSION
}

inline fun TypeArgumentListMarker.all(
    context: TypeSystemContext,
    crossinline predicate: (TypeArgumentMarker) -> Boolean
): Boolean = with(context) {
    repeat(size()) { index ->
        if (!predicate(get(index))) return false
    }
    return true
}