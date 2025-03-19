/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.model

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.resolve.checkers.EmptyIntersectionTypeChecker
import org.jetbrains.kotlin.resolve.checkers.EmptyIntersectionTypeInfo
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.Variance
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface KotlinTypeMarker
interface TypeArgumentMarker
interface TypeConstructorMarker
interface TypeParameterMarker

interface RigidTypeMarker : KotlinTypeMarker
interface FlexibleTypeMarker : KotlinTypeMarker
interface DynamicTypeMarker : FlexibleTypeMarker

interface DefinitelyNotNullTypeMarker : RigidTypeMarker
interface SimpleTypeMarker : RigidTypeMarker

interface CapturedTypeMarker : SimpleTypeMarker
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
    fun identicalArguments(a: RigidTypeMarker, b: RigidTypeMarker) = false
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
interface TypeSystemTypeFactoryContext : TypeSystemContext, TypeSystemBuiltInsContext {
    fun createFlexibleType(lowerBound: RigidTypeMarker, upperBound: RigidTypeMarker): KotlinTypeMarker

    fun createTrivialFlexibleTypeOrSelf(lowerBound: KotlinTypeMarker): KotlinTypeMarker {
        if (lowerBound.isFlexible()) return lowerBound
        return createFlexibleType(lowerBound.lowerBoundIfFlexible(), lowerBound.lowerBoundIfFlexible().withNullability(true))
    }

    fun isTriviallyFlexible(flexibleType: FlexibleTypeMarker): Boolean = false

    fun makeLowerBoundDefinitelyNotNullOrNotNull(flexibleType: FlexibleTypeMarker): KotlinTypeMarker {
        return createFlexibleType(
            flexibleType.lowerBound().makeDefinitelyNotNullOrNotNull(),
            flexibleType.upperBound()
        )
    }

    fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean,
        isExtensionFunction: Boolean = false,
        attributes: List<AnnotationMarker>? = null,
    ): SimpleTypeMarker

    fun createTypeArgument(type: KotlinTypeMarker, variance: TypeVariance): TypeArgumentMarker
    fun createStarProjection(typeParameter: TypeParameterMarker): TypeArgumentMarker

    fun createErrorType(debugName: String, delegatedType: RigidTypeMarker?): SimpleTypeMarker
    fun createUninferredType(constructor: TypeConstructorMarker): KotlinTypeMarker
}

/**
 * Factory, that constructs [TypeCheckerState], which defines type-checker behaviour
 * Implementation is recommended to be [TypeSystemContext]
 */
interface TypeCheckerProviderContext {
    fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean,
        dnnTypesEqualToFlexible: Boolean = false,
    ): TypeCheckerState
}

/**
 * Extended type system context, which defines set of operations specific to common super-type calculation
 */
interface TypeSystemCommonSuperTypesContext : TypeSystemContext, TypeSystemTypeFactoryContext, TypeCheckerProviderContext {

    fun KotlinTypeMarker.anySuperTypeConstructor(predicate: (RigidTypeMarker) -> Boolean) =
        newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
            .anySupertype(
                lowerBoundIfFlexible(),
                { predicate(it) },
                { TypeCheckerState.SupertypesPolicy.LowerIfFlexible }
            )

    fun KotlinTypeMarker.canHaveUndefinedNullability(): Boolean

    fun RigidTypeMarker.isExtensionFunction(): Boolean

    // TODO: KT-71905: consider removing all typeDepth() and typeDepthForApproximation() functions
    fun RigidTypeMarker.typeDepth(): Int

    fun KotlinTypeMarker.typeDepth(): Int = when (this) {
        is RigidTypeMarker -> typeDepth()
        is FlexibleTypeMarker -> maxOf(lowerBound().typeDepth(), upperBound().typeDepth())
        else -> error("Type should be rigid or flexible: $this")
    }

    fun KotlinTypeMarker.typeDepthForApproximation(): Int =
        typeDepth()

    fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<RigidTypeMarker>): RigidTypeMarker?

    /*
     * Converts error type constructor to error type
     * Used only in FIR
     */
    fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker

    fun unionTypeAttributes(types: List<KotlinTypeMarker>): List<AnnotationMarker>

    fun KotlinTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): KotlinTypeMarker

    fun supportsImprovedVarianceInCst(): Boolean
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

    fun TypeConstructorMarker.getApproximatedIntegerLiteralType(expectedType: KotlinTypeMarker?): KotlinTypeMarker

    fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean

    fun KotlinTypeMarker.eraseContainingTypeParameters(): KotlinTypeMarker

    fun Collection<KotlinTypeMarker>.singleBestRepresentative(): KotlinTypeMarker?

    fun KotlinTypeMarker.isUnit(): Boolean

    fun KotlinTypeMarker.isBuiltinFunctionTypeOrSubtype(): Boolean

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

    fun RigidTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): RigidTypeMarker
    fun RigidTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): RigidTypeMarker

    fun KotlinTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): KotlinTypeMarker =
        when (this) {
            is RigidTypeMarker -> replaceArguments(replacement)
            is FlexibleTypeMarker -> createFlexibleType(
                lowerBound().replaceArguments(replacement),
                upperBound().replaceArguments(replacement)
            )
            else -> error("sealed")
        }

    fun RigidTypeMarker.replaceArgumentsDeeply(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): RigidTypeMarker {
        return replaceArguments {
            val type = it.getType() ?: return@replaceArguments it
            val newProjection = if (type.argumentsCount() > 0) {
                it.replaceType(type.replaceArgumentsDeeply(replacement))
            } else it

            replacement(newProjection)
        }
    }

    fun KotlinTypeMarker.replaceArgumentsDeeply(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): KotlinTypeMarker {
        return when (this) {
            is RigidTypeMarker -> replaceArgumentsDeeply(replacement)
            is FlexibleTypeMarker -> createFlexibleType(
                lowerBound().replaceArgumentsDeeply(replacement),
                upperBound().replaceArgumentsDeeply(replacement)
            )
            else -> error("sealed")
        }
    }

    fun KotlinTypeMarker.hasExactAnnotation(): Boolean
    fun KotlinTypeMarker.hasNoInferAnnotation(): Boolean

    fun TypeConstructorMarker.isFinalClassConstructor(): Boolean

    fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker

    fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker
    fun CapturedTypeMarker.typeParameter(): TypeParameterMarker?
    fun CapturedTypeMarker.withNotNullProjection(): KotlinTypeMarker

    /**
     * Only for K2.
     */
    fun CapturedTypeMarker.hasRawSuperType(): Boolean

    fun TypeVariableMarker.defaultType(): SimpleTypeMarker

    fun createTypeWithUpperBoundForIntersectionResult(
        firstCandidate: KotlinTypeMarker,
        secondCandidate: KotlinTypeMarker
    ): KotlinTypeMarker

    /**
     * Only for K2
     */
    fun RigidTypeMarker.getUpperBoundForApproximationOfIntersectionType() : KotlinTypeMarker? = null

    fun KotlinTypeMarker.isSpecial(): Boolean

    fun TypeConstructorMarker.isTypeVariable(): Boolean
    fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean

    fun KotlinTypeMarker.isSignedOrUnsignedNumberType(): Boolean

    // ------------- functional type utils -------------

    fun KotlinTypeMarker.isFunctionOrKFunctionWithAnySuspendability(): Boolean

    fun KotlinTypeMarker.functionTypeKind(): FunctionTypeKind?

    fun KotlinTypeMarker.isExtensionFunctionType(): Boolean

    fun KotlinTypeMarker.extractArgumentsForFunctionTypeOrSubtype(): List<KotlinTypeMarker>

    fun KotlinTypeMarker.getFunctionTypeFromSupertypes(): KotlinTypeMarker

    fun getNonReflectFunctionTypeConstructor(parametersNumber: Int, kind: FunctionTypeKind): TypeConstructorMarker

    fun getReflectFunctionTypeConstructor(parametersNumber: Int, kind: FunctionTypeKind): TypeConstructorMarker

    // -------------------------------------------------

    fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker

    private fun <T> KotlinTypeMarker.extractTypeOf(to: MutableSet<T>, getIfApplicable: (TypeConstructorMarker) -> T?) {
        for (i in 0 until argumentsCount()) {
            val argument = getArgument(i)

            val argumentType = argument.getType() ?: continue
            val argumentTypeConstructor = argumentType.typeConstructor()
            val argumentToAdd = getIfApplicable(argumentTypeConstructor)

            if (argumentToAdd != null) {
                to.add(argumentToAdd)
            } else if (argumentType.argumentsCount() != 0) {
                argumentType.extractTypeOf(to, getIfApplicable)
            }
        }
    }

    fun KotlinTypeMarker.extractTypeVariables(): Set<TypeVariableTypeConstructorMarker> =
        buildSet {
            extractTypeOf(this) { it as? TypeVariableTypeConstructorMarker }
        }

    fun KotlinTypeMarker.extractTypeParameters(): Set<TypeParameterMarker> =
        buildSet {
            typeConstructor().getTypeParameterClassifier()?.let(::add)
            extractTypeOf(this) { it.getTypeParameterClassifier() }
        }

    /**
     * For case Foo <: (T..T?) return LowerConstraint for new constraint LowerConstraint <: T
     * In K1, in case nullable it was just Foo?, so constraint was Foo? <: T
     * But it's not 100% correct because prevent having not-nullable upper constraint on T while initial (Foo? <: (T..T?)) is not violated
     *
     * In K2 (with +JavaTypeParameterDefaultRepresentationWithDNN), we try to have a correct one: (Foo & Any..Foo?) <: T
     *
     * The same logic applies for T! <: UpperConstraint, as well
     * In K1, it was reduced to T <: UpperConstraint..UpperConstraint?
     * In K2 (with +JavaTypeParameterDefaultRepresentationWithDNN), we use UpperConstraint & Any..UpperConstraint?
     *
     * In future once we have only K2 (or FE 1.0 behavior is fixed) this method should be inlined to the use-site
     * TODO: Get rid of this function once KT-59138 is fixed and the relevant feature for disabling it will be removed
     */
    fun useRefinedBoundsForTypeVariableInFlexiblePosition(): Boolean

    /**
     * It's only relevant for K2 (and is not expected to be implemented properly in other contexts)
     */
    fun KotlinTypeMarker.convertToNonRaw(): KotlinTypeMarker

    @K2Only
    fun createSubstitutionFromSubtypingStubTypesToTypeVariables(): TypeSubstitutorMarker

    fun createCapturedStarProjectionForSelfType(
        typeVariable: TypeVariableTypeConstructorMarker,
        typesForRecursiveTypeParameters: List<KotlinTypeMarker>,
    ): SimpleTypeMarker? {
        val typeParameter = typeVariable.typeParameter ?: return null
        val starProjection = createStarProjection(typeParameter)
        val superType = intersectTypes(
            typesForRecursiveTypeParameters.map { type ->
                type.replaceArgumentsDeeply {
                    when (val typeConstructor = it.getType()?.typeConstructor()) {
                        typeVariable -> starProjection
                        is TypeVariableTypeConstructorMarker -> createTypeArgument(createUninferredType(typeConstructor), it.getVariance())
                        else -> it
                    }
                }
            }
        )

        return createCapturedType(starProjection, listOf(superType), lowerType = null, CaptureStatus.FROM_EXPRESSION)
    }

    fun createSubstitutorForSuperTypes(baseType: KotlinTypeMarker): TypeSubstitutorMarker?

    fun computeEmptyIntersectionTypeKind(types: Collection<KotlinTypeMarker>): EmptyIntersectionTypeInfo? =
        EmptyIntersectionTypeChecker.computeEmptyIntersectionEmptiness(this, types)

    val isK2: Boolean
}


class ArgumentList(initialSize: Int) : ArrayList<TypeArgumentMarker>(initialSize), TypeArgumentListMarker

/**
 * Defines common kotlin type operations with types for abstract types
 */
interface TypeSystemContext : TypeSystemOptimizationContext {
    @Deprecated(level = DeprecationLevel.ERROR, message = "This call does effectively nothing, please drop it")
    fun RigidTypeMarker.asRigidType(): RigidTypeMarker = this
    fun KotlinTypeMarker.asRigidType(): RigidTypeMarker?

    @Deprecated(level = DeprecationLevel.ERROR, message = "This call does effectively nothing, please drop it")
    fun FlexibleTypeMarker.asFlexibleType(): FlexibleTypeMarker = this
    fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker?

    fun KotlinTypeMarker.isError(): Boolean
    fun TypeConstructorMarker.isError(): Boolean
    fun KotlinTypeMarker.isUninferredParameter(): Boolean

    @Deprecated(level = DeprecationLevel.ERROR, message = "This call does effectively nothing, please drop it")
    fun DynamicTypeMarker.asDynamicType(): DynamicTypeMarker = this
    fun FlexibleTypeMarker.asDynamicType(): DynamicTypeMarker?

    fun KotlinTypeMarker.isRawType(): Boolean

    fun FlexibleTypeMarker.upperBound(): RigidTypeMarker
    fun FlexibleTypeMarker.lowerBound(): RigidTypeMarker

    @Deprecated(level = DeprecationLevel.ERROR, message = "This call does effectively nothing, please drop it")
    fun CapturedTypeMarker.asCapturedType(): CapturedTypeMarker = this
    fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker?

    @Deprecated(level = DeprecationLevel.ERROR, message = "This call does effectively nothing, please drop it")
    fun CapturedTypeMarker.asCapturedTypeUnwrappingDnn(): CapturedTypeMarker = this
    fun RigidTypeMarker.asCapturedTypeUnwrappingDnn(): CapturedTypeMarker? = originalIfDefinitelyNotNullable().asCapturedType()

    fun KotlinTypeMarker.isCapturedType() = asRigidType()?.asCapturedTypeUnwrappingDnn() != null

    @Deprecated(level = DeprecationLevel.ERROR, message = "This call does effectively nothing, please drop it")
    fun DefinitelyNotNullTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker = this
    fun RigidTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker?
    fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker

    @Deprecated(level = DeprecationLevel.ERROR, message = "This call does effectively nothing, please drop it")
    fun SimpleTypeMarker.originalIfDefinitelyNotNullable(): SimpleTypeMarker = this
    fun RigidTypeMarker.originalIfDefinitelyNotNullable(): SimpleTypeMarker =
        asDefinitelyNotNullType()?.original() ?: this as SimpleTypeMarker

    @Deprecated(level = DeprecationLevel.ERROR, message = "This call does effectively nothing, please drop it")
    fun DefinitelyNotNullTypeMarker.makeDefinitelyNotNullOrNotNull(): DefinitelyNotNullTypeMarker = this
    fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(): KotlinTypeMarker = makeDefinitelyNotNullOrNotNull(preserveAttributes = false)
    fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(preserveAttributes: Boolean): KotlinTypeMarker
    fun RigidTypeMarker.makeDefinitelyNotNullOrNotNull(): RigidTypeMarker

    fun KotlinTypeMarker.isMarkedNullable(): Boolean

    fun RigidTypeMarker.withNullability(nullable: Boolean): RigidTypeMarker
    fun RigidTypeMarker.typeConstructor(): TypeConstructorMarker
    fun KotlinTypeMarker.withNullability(nullable: Boolean): KotlinTypeMarker

    fun CapturedTypeMarker.isOldCapturedType(): Boolean
    fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker
    fun CapturedTypeMarker.captureStatus(): CaptureStatus
    fun CapturedTypeMarker.isProjectionNotNull(): Boolean
    fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker

    fun KotlinTypeMarker.argumentsCount(): Int
    fun KotlinTypeMarker.getArgument(index: Int): TypeArgumentMarker
    fun KotlinTypeMarker.getArguments(): List<TypeArgumentMarker>

    fun RigidTypeMarker.getArgumentOrNull(index: Int): TypeArgumentMarker? {
        if (index in 0 until argumentsCount()) return getArgument(index)
        return null
    }

    fun RigidTypeMarker.isStubType(): Boolean
    fun RigidTypeMarker.isStubTypeForVariableInSubtyping(): Boolean
    fun RigidTypeMarker.isStubTypeForBuilderInference(): Boolean
    fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker

    fun KotlinTypeMarker.asTypeArgument(): TypeArgumentMarker

    fun CapturedTypeMarker.lowerType(): KotlinTypeMarker?

    fun TypeArgumentMarker.isStarProjection(): Boolean
    fun TypeArgumentMarker.getVariance(): TypeVariance

    /**
     * Returns the type of the [TypeArgumentMarker] or `null` if it's a star projection.
     */
    fun TypeArgumentMarker.getType(): KotlinTypeMarker?
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
    fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean

    val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?

    fun TypeParameterMarker.getVariance(): TypeVariance
    fun TypeParameterMarker.upperBoundCount(): Int
    fun TypeParameterMarker.getUpperBound(index: Int): KotlinTypeMarker
    fun TypeParameterMarker.getUpperBounds(): List<KotlinTypeMarker>
    fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker
    fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker? = null): Boolean

    fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean

    fun TypeConstructorMarker.isDenotable(): Boolean

    @Deprecated(level = DeprecationLevel.ERROR, message = "This call does effectively nothing, please drop it")
    fun RigidTypeMarker.lowerBoundIfFlexible(): RigidTypeMarker = this
    fun KotlinTypeMarker.lowerBoundIfFlexible(): RigidTypeMarker = this.asFlexibleType()?.lowerBound() ?: this.asRigidType()!!

    @Deprecated(level = DeprecationLevel.ERROR, message = "This call does effectively nothing, please drop it")
    fun RigidTypeMarker.upperBoundIfFlexible(): RigidTypeMarker = this
    fun KotlinTypeMarker.upperBoundIfFlexible(): RigidTypeMarker = this.asFlexibleType()?.upperBound() ?: this.asRigidType()!!

    fun KotlinTypeMarker.isFlexibleWithDifferentTypeConstructors(): Boolean =
        lowerBoundIfFlexible().typeConstructor() != upperBoundIfFlexible().typeConstructor()

    fun KotlinTypeMarker.isFlexible(): Boolean = asFlexibleType() != null

    fun KotlinTypeMarker.isDynamic(): Boolean = asFlexibleType()?.asDynamicType() != null
    fun KotlinTypeMarker.isCapturedDynamic(): Boolean =
        asRigidType()?.asCapturedTypeUnwrappingDnn()?.typeConstructor()?.projection()?.getType()?.isDynamic() == true

    fun KotlinTypeMarker.isDefinitelyNotNullType(): Boolean = asRigidType()?.asDefinitelyNotNullType() != null
    fun RigidTypeMarker.isDefinitelyNotNullType(): Boolean = asDefinitelyNotNullType() != null

    // This kind of types is obsolete (expected to be removed at 1.7) and shouldn't be used further in a new code
    // Now, such types are being replaced with definitely non-nullable types
    @ObsoleteTypeKind
    fun KotlinTypeMarker.isNotNullTypeParameter(): Boolean = false

    fun KotlinTypeMarker.hasFlexibleNullability() =
        lowerBoundIfFlexible().isMarkedNullable() != upperBoundIfFlexible().isMarkedNullable()

    fun KotlinTypeMarker.typeConstructor(): TypeConstructorMarker =
        (asRigidType() ?: lowerBoundIfFlexible()).typeConstructor()

    fun KotlinTypeMarker.isNullableType(): Boolean

    fun KotlinTypeMarker.isNullableAny() = this.typeConstructor().isAnyConstructor() && this.isNullableType()
    fun KotlinTypeMarker.isNothing() = this.typeConstructor().isNothingConstructor() && !this.isNullableType()
    fun KotlinTypeMarker.isFlexibleNothing() =
        this is FlexibleTypeMarker && lowerBound().isNothing() && upperBound().isNullableNothing()

    fun KotlinTypeMarker.isNullableNothing() = this.typeConstructor().isNothingConstructor() && this.isNullableType()

    fun RigidTypeMarker.isClassType(): Boolean = typeConstructor().isClassTypeConstructor()

    fun RigidTypeMarker.fastCorrespondingSupertypes(constructor: TypeConstructorMarker): List<SimpleTypeMarker>? = null

    fun RigidTypeMarker.isIntegerLiteralType(): Boolean = typeConstructor().isIntegerLiteralTypeConstructor()

    fun RigidTypeMarker.possibleIntegerTypes(): Collection<KotlinTypeMarker>

    fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean

    fun captureFromArguments(
        type: RigidTypeMarker,
        status: CaptureStatus
    ): RigidTypeMarker?

    fun captureFromExpression(type: KotlinTypeMarker): KotlinTypeMarker?

    fun RigidTypeMarker.asArgumentList(): TypeArgumentListMarker

    operator fun TypeArgumentListMarker.get(index: Int): TypeArgumentMarker {
        return when (this) {
            is SimpleTypeMarker -> getArgument(index)
            is ArgumentList -> get(index)
            else -> error("unknown type argument list type: $this, ${this::class}")
        }
    }

    fun TypeArgumentListMarker.size(): Int {
        return when (this) {
            is RigidTypeMarker -> argumentsCount()
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
    fun TypeConstructorMarker.isArrayConstructor(): Boolean

    /**
     *
     * SingleClassifierType is one of the following types:
     *  - classType
     *  - type for type parameter
     *  - captured type
     *
     * Such types can contains error types in our arguments, but type constructor isn't errorTypeConstructor
     */
    fun RigidTypeMarker.isSingleClassifierType(): Boolean

    fun intersectTypes(types: Collection<KotlinTypeMarker>): KotlinTypeMarker
    fun intersectTypes(types: Collection<SimpleTypeMarker>): SimpleTypeMarker

    fun KotlinTypeMarker.isRigidType(): Boolean = asRigidType() != null

    fun RigidTypeMarker.isPrimitiveType(): Boolean = (this as? SimpleTypeMarker)?.isPrimitiveType() == true
    fun SimpleTypeMarker.isPrimitiveType(): Boolean

    fun KotlinTypeMarker.getAttributes(): List<AnnotationMarker>

    fun substitutionSupertypePolicy(type: RigidTypeMarker): TypeCheckerState.SupertypesPolicy

    fun KotlinTypeMarker.isTypeVariableType(): Boolean

    fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, KotlinTypeMarker>): TypeSubstitutorMarker
    fun createEmptySubstitutor(): TypeSubstitutorMarker

    /**
     * @returns substituted type or [type] if there were no substitution
     */
    fun TypeSubstitutorMarker.safeSubstitute(type: KotlinTypeMarker): KotlinTypeMarker
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

@RequiresOptIn
annotation class K2Only
