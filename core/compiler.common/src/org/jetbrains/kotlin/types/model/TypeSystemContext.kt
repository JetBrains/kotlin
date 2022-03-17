/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.model

import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.Variance
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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
interface TypeVariableTypeConstructorMarker : TypeConstructorMarker

interface CapturedTypeConstructorMarker : TypeConstructorMarker

interface IntersectionTypeConstructorMarker : TypeConstructorMarker

interface TypeSubstitutorMarker

interface AnnotationMarker

enum class TypeVariance(val presentation: String) {
    IN("in"),
    OUT("out"),
    INV("");

    override fun toString(): String = presentation
}

fun Variance.convertVariance(): TypeVariance {
    return when (this) {
        Variance.INVARIANT -> TypeVariance.INV
        Variance.IN_VARIANCE -> TypeVariance.IN
        Variance.OUT_VARIANCE -> TypeVariance.OUT
    }
}

interface TypeSystemOptimizationContext {
    /**
     *  @return true is a.arguments == b.arguments, or false if not supported
     */
    fun identicalArguments(a: SimpleTypeMarker, b: SimpleTypeMarker) = false
}

/**
 * Context that allow type-impl agnostic access to common types
 */
interface TypeSystemBuiltInsContext {
    fun nullableNothingType(): SimpleTypeMarker
    fun nullableAnyType(): SimpleTypeMarker
    fun nothingType(): SimpleTypeMarker
    fun anyType(): SimpleTypeMarker
}

/**
 * Context that allow construction of types
 */
interface TypeSystemTypeFactoryContext: TypeSystemBuiltInsContext {
    fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): KotlinTypeMarker
    fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean,
        isExtensionFunction: Boolean = false,
        attributes: List<AnnotationMarker>? = null
    ): SimpleTypeMarker

    fun createTypeArgument(type: KotlinTypeMarker, variance: TypeVariance): TypeArgumentMarker
    fun createStarProjection(typeParameter: TypeParameterMarker): TypeArgumentMarker

    fun createErrorType(debugName: String): SimpleTypeMarker
    fun createUninferredType(constructor: TypeConstructorMarker): KotlinTypeMarker
}

/**
 * Factory, that constructs [TypeCheckerState], which defines type-checker behaviour
 * Implementation is recommended to be [TypeSystemContext]
 */
interface TypeCheckerProviderContext {
    fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): TypeCheckerState
}

/**
 * Extended type system context, which defines set of operations specific to common super-type calculation
 */
interface TypeSystemCommonSuperTypesContext : TypeSystemContext, TypeSystemTypeFactoryContext, TypeCheckerProviderContext {

    fun KotlinTypeMarker.anySuperTypeConstructor(predicate: (TypeConstructorMarker) -> Boolean) =
        newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
            .anySupertype(
                lowerBoundIfFlexible(),
                { predicate(it.typeConstructor()) },
                { TypeCheckerState.SupertypesPolicy.LowerIfFlexible }
            )

    fun KotlinTypeMarker.canHaveUndefinedNullability(): Boolean

    fun SimpleTypeMarker.isExtensionFunction(): Boolean

    fun SimpleTypeMarker.typeDepth(): Int

    fun KotlinTypeMarker.typeDepth(): Int = when (this) {
        is SimpleTypeMarker -> typeDepth()
        is FlexibleTypeMarker -> maxOf(lowerBound().typeDepth(), upperBound().typeDepth())
        else -> error("Type should be simple or flexible: $this")
    }

    fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker?

    /*
     * Converts error type constructor to error type
     * Used only in FIR
     */
    fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker

    fun unionTypeAttributes(types: List<KotlinTypeMarker>): List<AnnotationMarker>

    fun KotlinTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): KotlinTypeMarker
}

// This interface is only used to declare that implementing class is supposed to be used as a TypeSystemInferenceExtensionContext component
// Otherwise clash happens during DI container initialization: there are a lot of components that extend TypeSystemInferenceExtensionContext
// but they only has it among supertypes to bring additional receiver into their scopes, i.e. they are not intended to be used as
// component implementation for TypeSystemInferenceExtensionContext
interface TypeSystemInferenceExtensionContextDelegate : TypeSystemInferenceExtensionContext

/**
 * Extended type system context, which defines set of type operations specific for type inference
 */
interface TypeSystemInferenceExtensionContext : TypeSystemContext, TypeSystemBuiltInsContext, TypeSystemCommonSuperTypesContext {
    fun KotlinTypeMarker.contains(predicate: (KotlinTypeMarker) -> Boolean): Boolean

    fun TypeConstructorMarker.isUnitTypeConstructor(): Boolean

    fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): KotlinTypeMarker

    fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean

    fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean

    fun Collection<KotlinTypeMarker>.singleBestRepresentative(): KotlinTypeMarker?

    fun KotlinTypeMarker.isUnit(): Boolean

    fun KotlinTypeMarker.isBuiltinFunctionalTypeOrSubtype(): Boolean

    fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<KotlinTypeMarker>,
        lowerType: KotlinTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker

    fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker
    fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker

    fun KotlinTypeMarker.removeAnnotations(): KotlinTypeMarker
    fun KotlinTypeMarker.removeExactAnnotation(): KotlinTypeMarker

    fun SimpleTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): SimpleTypeMarker
    fun SimpleTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): SimpleTypeMarker

    fun KotlinTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): KotlinTypeMarker =
        when (this) {
            is SimpleTypeMarker -> replaceArguments(replacement)
            is FlexibleTypeMarker -> createFlexibleType(
                lowerBound().replaceArguments(replacement),
                upperBound().replaceArguments(replacement)
            )
            else -> error("sealed")
        }

    fun SimpleTypeMarker.replaceArgumentsDeeply(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): SimpleTypeMarker {
        return replaceArguments {
            if (it.isStarProjection()) return@replaceArguments it

            val type = it.getType()
            val newProjection = if (type.argumentsCount() > 0) {
                it.replaceType(type.replaceArgumentsDeeply(replacement))
            } else it

            replacement(newProjection)
        }
    }

    fun KotlinTypeMarker.replaceArgumentsDeeply(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): KotlinTypeMarker {
        return when (this) {
            is SimpleTypeMarker -> replaceArgumentsDeeply(replacement)
            is FlexibleTypeMarker -> createFlexibleType(
                lowerBound().replaceArgumentsDeeply(replacement),
                upperBound().replaceArgumentsDeeply(replacement)
            )
            else -> error("sealed")
        }
    }

    fun KotlinTypeMarker.hasExactAnnotation(): Boolean
    fun KotlinTypeMarker.hasNoInferAnnotation(): Boolean

    fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker

    fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker
    fun CapturedTypeMarker.typeParameter(): TypeParameterMarker?
    fun CapturedTypeMarker.withNotNullProjection(): KotlinTypeMarker

    fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker
    fun createEmptySubstitutor(): TypeSubstitutorMarker

    fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker


    fun TypeVariableMarker.defaultType(): SimpleTypeMarker

    fun createTypeWithAlternativeForIntersectionResult(
        firstCandidate: KotlinTypeMarker,
        secondCandidate: KotlinTypeMarker
    ): KotlinTypeMarker

    fun KotlinTypeMarker.isSpecial(): Boolean

    fun TypeConstructorMarker.isTypeVariable(): Boolean
    fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean

    fun KotlinTypeMarker.isSignedOrUnsignedNumberType(): Boolean

    fun KotlinTypeMarker.isFunctionOrKFunctionWithAnySuspendability(): Boolean

    fun KotlinTypeMarker.isSuspendFunctionTypeOrSubtype(): Boolean

    fun KotlinTypeMarker.isExtensionFunctionType(): Boolean

    fun KotlinTypeMarker.extractArgumentsForFunctionalTypeOrSubtype(): List<KotlinTypeMarker>

    fun KotlinTypeMarker.getFunctionalTypeFromSupertypes(): KotlinTypeMarker

    fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker

    fun getFunctionTypeConstructor(parametersNumber: Int, isSuspend: Boolean): TypeConstructorMarker

    fun getKFunctionTypeConstructor(parametersNumber: Int, isSuspend: Boolean): TypeConstructorMarker

    private fun KotlinTypeMarker.extractTypeVariables(to: MutableSet<TypeVariableTypeConstructorMarker>) {
        for (i in 0 until argumentsCount()) {
            val argument = getArgument(i)

            if (argument.isStarProjection()) continue

            val argumentType = argument.getType()
            val argumentTypeConstructor = argumentType.typeConstructor()
            if (argumentTypeConstructor is TypeVariableTypeConstructorMarker) {
                to.add(argumentTypeConstructor)
            } else if (argumentType.argumentsCount() != 0) {
                argumentType.extractTypeVariables(to)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun KotlinTypeMarker.extractTypeVariables() = buildSet { extractTypeVariables(this) }

    /**
     * For case Foo <: (T..T?) return LowerBound for new constraint LowerBound <: T
     * In FE 1.0, in case nullable it was just Foo?, so constraint was Foo? <: T
     * But it's not 100% correct because prevent having not-nullable upper constraint on T while initial (Foo? <: (T..T?)) is not violated
     *
     * In FIR, we try to have a correct one: (Foo!!..Foo?) <: T
     *
     * In future once we have only FIR (or FE 1.0 behavior is fixed) this method should be inlined to the use-site
     */
    fun SimpleTypeMarker.createConstraintPartForLowerBoundAndFlexibleTypeVariable(): KotlinTypeMarker

    fun createCapturedStarProjectionForSelfType(
        typeVariable: TypeVariableTypeConstructorMarker,
        typesForRecursiveTypeParameters: List<KotlinTypeMarker>,
    ): SimpleTypeMarker? {
        val typeParameter = typeVariable.typeParameter ?: return null
        val starProjection = createStarProjection(typeParameter)
        val superType = intersectTypes(
            typesForRecursiveTypeParameters.map { type ->
                type.replaceArgumentsDeeply {
                    val constructor = it.getType().typeConstructor()
                    if (constructor is TypeVariableTypeConstructorMarker && constructor == typeVariable) starProjection else it
                }
            }
        )

        return createCapturedType(starProjection, listOf(superType), lowerType = null, CaptureStatus.FROM_EXPRESSION)
    }
}


class ArgumentList(initialSize: Int) : ArrayList<TypeArgumentMarker>(initialSize), TypeArgumentListMarker

/**
 * Defines common kotlin type operations with types for abstract types
 */
interface TypeSystemContext : TypeSystemOptimizationContext {
    fun KotlinTypeMarker.asSimpleType(): SimpleTypeMarker?
    fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker?

    fun KotlinTypeMarker.isError(): Boolean
    fun TypeConstructorMarker.isError(): Boolean
    fun KotlinTypeMarker.isUninferredParameter(): Boolean
    fun FlexibleTypeMarker.asDynamicType(): DynamicTypeMarker?

    fun FlexibleTypeMarker.asRawType(): RawTypeMarker?
    fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker

    fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker
    fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker?

    fun KotlinTypeMarker.isCapturedType() = asSimpleType()?.asCapturedType() != null

    fun SimpleTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker?
    fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker
    fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(): KotlinTypeMarker
    fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker
    fun SimpleTypeMarker.isMarkedNullable(): Boolean
    fun KotlinTypeMarker.isMarkedNullable(): Boolean =
        this is SimpleTypeMarker && isMarkedNullable()

    fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker
    fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker
    fun KotlinTypeMarker.withNullability(nullable: Boolean): KotlinTypeMarker

    fun CapturedTypeMarker.isOldCapturedType(): Boolean
    fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker
    fun CapturedTypeMarker.captureStatus(): CaptureStatus
    fun CapturedTypeMarker.isProjectionNotNull(): Boolean
    fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker

    fun KotlinTypeMarker.argumentsCount(): Int
    fun KotlinTypeMarker.getArgument(index: Int): TypeArgumentMarker
    fun KotlinTypeMarker.getArguments(): List<TypeArgumentMarker>

    fun SimpleTypeMarker.getArgumentOrNull(index: Int): TypeArgumentMarker? {
        if (index in 0 until argumentsCount()) return getArgument(index)
        return null
    }

    fun SimpleTypeMarker.isStubType(): Boolean
    fun SimpleTypeMarker.isStubTypeForVariableInSubtyping(): Boolean
    fun SimpleTypeMarker.isStubTypeForBuilderInference(): Boolean
    fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker

    fun KotlinTypeMarker.asTypeArgument(): TypeArgumentMarker

    fun CapturedTypeMarker.lowerType(): KotlinTypeMarker?

    fun TypeArgumentMarker.isStarProjection(): Boolean
    fun TypeArgumentMarker.getVariance(): TypeVariance
    fun TypeArgumentMarker.getType(): KotlinTypeMarker
    fun TypeArgumentMarker.replaceType(newType: KotlinTypeMarker): TypeArgumentMarker

    fun TypeConstructorMarker.parametersCount(): Int
    fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker
    fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker>
    fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker>
    fun TypeConstructorMarker.isIntersection(): Boolean
    fun TypeConstructorMarker.isClassTypeConstructor(): Boolean
    fun TypeConstructorMarker.isInterface(): Boolean
    fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean
    fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean
    fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean
    fun TypeConstructorMarker.isLocalType(): Boolean
    fun TypeConstructorMarker.isAnonymous(): Boolean
    fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker?

    val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?

    fun TypeParameterMarker.getVariance(): TypeVariance
    fun TypeParameterMarker.upperBoundCount(): Int
    fun TypeParameterMarker.getUpperBound(index: Int): KotlinTypeMarker
    fun TypeParameterMarker.getUpperBounds(): List<KotlinTypeMarker>
    fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker
    fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker? = null): Boolean

    fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean

    fun TypeConstructorMarker.isDenotable(): Boolean

    fun KotlinTypeMarker.lowerBoundIfFlexible(): SimpleTypeMarker = this.asFlexibleType()?.lowerBound() ?: this.asSimpleType()!!
    fun KotlinTypeMarker.upperBoundIfFlexible(): SimpleTypeMarker = this.asFlexibleType()?.upperBound() ?: this.asSimpleType()!!

    fun KotlinTypeMarker.isFlexible(): Boolean = asFlexibleType() != null

    fun KotlinTypeMarker.isDynamic(): Boolean = asFlexibleType()?.asDynamicType() != null
    fun KotlinTypeMarker.isCapturedDynamic(): Boolean =
        asSimpleType()?.asCapturedType()?.typeConstructor()?.projection()?.takeUnless { it.isStarProjection() }
            ?.getType()?.isDynamic() == true

    fun KotlinTypeMarker.isDefinitelyNotNullType(): Boolean = asSimpleType()?.asDefinitelyNotNullType() != null

    // This kind of types is obsolete (expected to be removed at 1.7) and shouldn't be used further in a new code
    // Now, such types are being replaced with definitely non-nullable types
    @ObsoleteTypeKind
    fun KotlinTypeMarker.isNotNullTypeParameter(): Boolean = false

    fun KotlinTypeMarker.hasFlexibleNullability() =
        lowerBoundIfFlexible().isMarkedNullable() != upperBoundIfFlexible().isMarkedNullable()

    fun KotlinTypeMarker.typeConstructor(): TypeConstructorMarker =
        (asSimpleType() ?: lowerBoundIfFlexible()).typeConstructor()

    fun KotlinTypeMarker.isNullableType(): Boolean

    fun KotlinTypeMarker.isNullableAny() = this.typeConstructor().isAnyConstructor() && this.isNullableType()
    fun KotlinTypeMarker.isNothing() = this.typeConstructor().isNothingConstructor() && !this.isNullableType()
    fun KotlinTypeMarker.isFlexibleNothing() =
        this is FlexibleTypeMarker && lowerBound().isNothing() && upperBound().isNullableNothing()

    fun KotlinTypeMarker.isNullableNothing() = this.typeConstructor().isNothingConstructor() && this.isNullableType()

    fun SimpleTypeMarker.isClassType(): Boolean = typeConstructor().isClassTypeConstructor()

    fun SimpleTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<SimpleTypeMarker>? = null

    fun SimpleTypeMarker.isIntegerLiteralType(): Boolean = typeConstructor().isIntegerLiteralTypeConstructor()

    fun SimpleTypeMarker.possibleIntegerTypes(): Collection<KotlinTypeMarker>

    fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean

    fun captureFromArguments(
        type: SimpleTypeMarker,
        status: CaptureStatus
    ): SimpleTypeMarker?

    fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker?

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

    operator fun TypeArgumentListMarker.iterator() = object : Iterator<TypeArgumentMarker> {
        private var argumentIndex: Int = 0

        override fun hasNext(): Boolean = argumentIndex < size()

        override fun next(): TypeArgumentMarker {
            val argument = get(argumentIndex)
            argumentIndex += 1
            return argument
        }
    }

    fun TypeConstructorMarker.isAnyConstructor(): Boolean
    fun TypeConstructorMarker.isNothingConstructor(): Boolean

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

    fun KotlinTypeMarker.isSimpleType(): Boolean = asSimpleType() != null

    fun SimpleTypeMarker.isPrimitiveType(): Boolean

    fun KotlinTypeMarker.getAttributes(): List<AnnotationMarker>

    fun KotlinTypeMarker.hasCustomAttributes(): Boolean

    fun KotlinTypeMarker.getCustomAttributes(): List<AnnotationMarker>

    fun substitutionSupertypePolicy(type: SimpleTypeMarker): TypeCheckerState.SupertypesPolicy

    fun KotlinTypeMarker.isTypeVariableType(): Boolean
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

@OptIn(ExperimentalContracts::class)
fun requireOrDescribe(condition: Boolean, value: Any?) {
    contract {
        returns() implies condition
    }
    require(condition) {
        val typeInfo = if (value != null) {
            ", type = '${value::class}'"
        } else ""
        "Unexpected: value = '$value'$typeInfo"
    }
}

@RequiresOptIn("This kinds of type is obsolete and should not be used until you really need it")
annotation class ObsoleteTypeKind
