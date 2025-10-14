/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator.ResolveDirection
import org.jetbrains.kotlin.resolve.calls.inference.extractTypeForGivenRecursiveTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.hasRecursiveTypeParametersWithGivenSelfType
import org.jetbrains.kotlin.resolve.calls.inference.isEqualityConstraintCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.*

class ResultTypeResolver(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    private val languageVersionSettings: LanguageVersionSettings,
) {
    interface Context : TypeSystemInferenceExtensionContext, ConstraintSystemBuilder {
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
        val outerSystemVariablesPrefixSize: Int
        fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
        fun isReified(variable: TypeVariableMarker): Boolean
    }

    context(c: Context)
    private fun TypeVariableMarker.getDefaultTypeForSelfType(constraints: List<Constraint>): KotlinTypeMarker? {
        val typeVariableConstructor = freshTypeConstructor()
        val typeParameter = typeVariableConstructor.typeParameter ?: return null

        val typesForRecursiveTypeParameters = constraints.mapNotNull { constraint ->
            if (constraint.position.from !is DeclaredUpperBoundConstraintPosition<*>) return@mapNotNull null
            constraint.type.extractTypeForGivenRecursiveTypeParameter(typeParameter)
        }.takeIf { it.isNotEmpty() } ?: return null

        return c.createCapturedStarProjectionForSelfType(typeVariableConstructor, typesForRecursiveTypeParameters)
    }

    context(c: Context)
    private fun TypeVariableMarker.getDefaultType(direction: ResolveDirection, constraints: List<Constraint>): KotlinTypeMarker {
        getDefaultTypeForSelfType(constraints)?.let { return it }

        return if (direction == ResolveDirection.TO_SUBTYPE) c.nothingType() else c.nullableAnyType()
    }

    context(c: Context)
    fun findResultType(variableWithConstraints: VariableWithConstraints, direction: ResolveDirection): KotlinTypeMarker {
        findResultTypeOrNull(variableWithConstraints, direction)?.let { return it }

        // no proper constraints
        return variableWithConstraints.typeVariable.getDefaultType(direction, variableWithConstraints.constraints)
    }

    context(c: Context)
    private fun KotlinTypeMarker.approximateToSuperTypeOrSelf(superTypeCandidate: KotlinTypeMarker?): KotlinTypeMarker {
        // In case we have an ILT as the subtype, we approximate it using the upper type as the expected type.
        // This is more precise than always approximating it to Int or UInt.
        // Note, we shouldn't have nested ILTs because they can only appear as a constraint on a type variable
        // that we would have fixed earlier.
        if (typeConstructor().isIntegerLiteralTypeConstructor()) {
            return typeApproximator.approximateToSuperType(
                this,
                TypeApproximatorConfiguration.TopLevelIntegerLiteralTypeApproximationWithExpectedType(superTypeCandidate)
            ) ?: this
        }

        return typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.InternalTypesApproximation) ?: this
    }

    private fun KotlinTypeMarker.approximateToSubTypeOrSelf(): KotlinTypeMarker {
        return typeApproximator.approximateToSubType(this, TypeApproximatorConfiguration.InternalTypesApproximation) ?: this
    }

    private val useImprovedCapturedTypeApproximation: Boolean =
        languageVersionSettings.supportsFeature(LanguageFeature.ImprovedCapturedTypeApproximationInInference)

    context(c: Context)
    fun findResultTypeOrNull(
        variableWithConstraints: VariableWithConstraints,
        direction: ResolveDirection,
    ): KotlinTypeMarker? {
        val resultTypeFromEqualConstraint = findResultIfThereIsEqualsConstraint(variableWithConstraints, isStrictMode = false)
        if (resultTypeFromEqualConstraint?.isAppropriateResultTypeFromEqualityConstraints() == true) return resultTypeFromEqualConstraint

        val subType = variableWithConstraints.findSubType()
        val superType = variableWithConstraints.findSuperType()

        val (preparedSubType, preparedSuperType) = if (c.isK2 && useImprovedCapturedTypeApproximation) {
            variableWithConstraints.prepareSubAndSuperTypes(subType, superType)
        } else {
            variableWithConstraints.prepareSubAndSuperTypesLegacy(subType, superType)
        }

        val resultTypeFromDirection = if (direction == ResolveDirection.TO_SUBTYPE || direction == ResolveDirection.UNKNOWN) {
            variableWithConstraints.resultType(preparedSubType, preparedSuperType)
        } else {
            variableWithConstraints.resultType(preparedSuperType, preparedSubType)
        }

        // In the general case, we can have here two types, one from EQUAL constraint which must be ILT-based,
        // and the second one from UPPER/LOWER constraints (subType/superType based)
        // The logic of choice here is:
        // - if one type is null, we return another one
        // - we return type from UPPER/LOWER constraints if it's more precise (in fact, only Int/Short/Byte/Long is allowed here)
        // - otherwise we return ILT-based type
        return when {
            resultTypeFromEqualConstraint == null -> resultTypeFromDirection
            resultTypeFromDirection == null -> resultTypeFromEqualConstraint
            !resultTypeFromDirection.typeConstructor().isNothingConstructor() &&
                    AbstractTypeChecker.isSubtypeOf(c, resultTypeFromDirection, resultTypeFromEqualConstraint) -> resultTypeFromDirection
            else -> resultTypeFromEqualConstraint
        }
    }

    context(c: Context)
    private fun KotlinTypeMarker.isAppropriateResultTypeFromEqualityConstraints(): Boolean {
        if (!c.isK2) return true

        // In K2, we don't allow fixing to a result type from EQ constraints if they contain ILTs
        return !contains { type ->
            type.typeConstructor().isIntegerLiteralConstantTypeConstructor()
        }
    }

    /**
     * The general approach to approximation of resulting types (in K2) is to
     * - always approximate ILTs
     * - always approximate captured types unless this leads to a contradiction.
     * A contradiction can appear if we have some captured type C = CapturedType(*) in the subtype and in the supertype.
     *
     * Example: A<C> <: T <: A<C>
     *
     * If we were to approximate the result type, we would end up with a contradiction
     * A<*> </: A<C>
     *
     * In comparison, types from equality constraints are never approximated because it would always lead to a contradiction.
     * We evaluated a never-approximate approach but found it to be infeasible as it introduces many new errors
     * (type mismatches, REIFIED_TYPE_FORBIDDEN_SUBSTITUTION, TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR, etc.).
     */
    context(c: Context)
    private fun VariableWithConstraints.prepareSubAndSuperTypes(
        subType: KotlinTypeMarker?,
        superType: KotlinTypeMarker?,
    ): Pair<KotlinTypeMarker?, KotlinTypeMarker?> {
        val approximatedSubType = subType?.approximateToSuperTypeOrSelf(superType)
        val approximatedSuperType = superType?.approximateToSubTypeOrSelf()

        val preparedSubType = when {
            approximatedSubType == null -> null
            shouldBeUsedWithoutApproximation(subType, approximatedSubType, this) -> subType
            else -> approximatedSubType
        }

        val preparedSuperType = when {
            approximatedSuperType == null -> null
            shouldBeUsedWithoutApproximation(superType, approximatedSuperType, this) -> superType
            superType.typeConstructor().hasRecursiveTypeParametersWithGivenSelfType() -> superType
            else -> approximatedSuperType
            // Super type should be the most flexible, sub type should be the least one
        }.makeFlexibleIfNecessary(constraints)

        return preparedSubType to preparedSuperType
    }

    /**
     * Returns `true` if using [approximatedResultType] as result type leads to a contradiction.
     *
     * If [resultType] and [approximatedResultType] are referentially equal, it means there is nothing to approximate in the first place.
     * Therefore `false` is returned.
     *
     * Only used when [LanguageFeature.ImprovedCapturedTypeApproximationInInference] is enabled.
     */
    context(c: Context)
    private fun shouldBeUsedWithoutApproximation(
        resultType: KotlinTypeMarker,
        approximatedResultType: KotlinTypeMarker,
        variableWithConstraints: VariableWithConstraints,
    ): Boolean {
        if (resultType === approximatedResultType || c.hasContradiction) return false

        // TODO(related to KT-64802) This if shouldn't be necessary but removing it breaks
        // compiler/testData/diagnostics/tests/unsignedTypes/conversions/inferenceForSignedAndUnsignedTypes.kt
        if (resultType.typeConstructor().isIntegerLiteralTypeConstructor()) return false

        return !c.isEqualityConstraintCompatible(approximatedResultType, variableWithConstraints.typeVariable.defaultType(c))
    }

    context(c: Context)
    private fun VariableWithConstraints.prepareSubAndSuperTypesLegacy(
        subType: KotlinTypeMarker?,
        superType: KotlinTypeMarker?,
    ): Pair<KotlinTypeMarker?, KotlinTypeMarker?> {
        val similarCapturedTypesInK2 = c.isK2 && similarOrCloselyBoundCapturedTypes(subType, superType)

        val preparedSubType = when {
            subType == null -> null
            similarCapturedTypesInK2 -> subType
            else -> typeApproximator.approximateToSuperType(subType, TypeApproximatorConfiguration.InternalTypesApproximation) ?: subType
        }

        val preparedSuperType = when {
            superType == null -> null
            similarCapturedTypesInK2 -> superType
            c.isK2 && superType.typeConstructor().hasRecursiveTypeParametersWithGivenSelfType() -> superType
            else -> typeApproximator.approximateToSubType(superType, TypeApproximatorConfiguration.InternalTypesApproximation) ?: superType
            // Super type should be the most flexible, sub type should be the least one
        }.makeFlexibleIfNecessary(constraints)

        return preparedSubType to preparedSuperType
    }

    /**
     * Old heuristic used to determine when result types from lower/upper constraints should be approximated or not.
     *
     * Becomes obsolete after [LanguageFeature.ImprovedCapturedTypeApproximationInInference] is enabled.
     */
    context(c: Context)
    private fun similarOrCloselyBoundCapturedTypes(subType: KotlinTypeMarker?, superType: KotlinTypeMarker?): Boolean {
        if (subType == null) return false
        if (superType == null) return false
        val subTypeLowerConstructor = subType.lowerBoundIfFlexible().typeConstructor()
        if (!subTypeLowerConstructor.isCapturedTypeConstructor()) return false

        if (superType in subTypeLowerConstructor.supertypes() && superType.contains { it.typeConstructor().isCapturedTypeConstructor() }) {
            return true
        }

        return subTypeLowerConstructor == subType.upperBoundIfFlexible().typeConstructor() &&
                subTypeLowerConstructor == superType.lowerBoundIfFlexible().typeConstructor() &&
                subTypeLowerConstructor == superType.upperBoundIfFlexible().typeConstructor()
    }

    /*
     * We propagate nullness flexibility into the result type from type variables in other constraints
     * to prevent variable fixation into less flexible type.
     *  Constraints:
     *      UPPER(TypeVariable(T)..TypeVariable(T)?)
     *      UPPER(Foo?)
     *  Result type = makeFlexibleIfNecessary(Foo?) = Foo!
     *
     * We don't propagate nullness flexibility in depth as it's non-determined for now (see KT-35534):
     *  CST(Bar<Foo>, Bar<Foo!>) = Bar<Foo!>
     *  CST(Bar<Foo!>, Bar<Foo>) = Bar<Foo>
     * But: CST(Foo, Foo!) = CST(Foo!, Foo) = Foo!
     */
    context(c: Context)
    private fun KotlinTypeMarker?.makeFlexibleIfNecessary(constraints: List<Constraint>) = when (val type = this@makeFlexibleIfNecessary) {
        is RigidTypeMarker -> {
            if (constraints.any { it.type.typeConstructor().isTypeVariable() && it.type.hasFlexibleNullability() }) {
                c.createTrivialFlexibleTypeOrSelf(type.makeDefinitelyNotNullOrNotNull())
            } else type
        }
        else -> type
    }

    context(c: Context)
    private fun VariableWithConstraints.resultType(
        firstCandidate: KotlinTypeMarker?,
        secondCandidate: KotlinTypeMarker?,
    ): KotlinTypeMarker? {
        if (firstCandidate == null || secondCandidate == null) return firstCandidate ?: secondCandidate

        specialResultForIntersectionType(firstCandidate, secondCandidate)?.let { intersectionWithAlternative ->
            return intersectionWithAlternative
        }

        if (isSuitableType(firstCandidate, this)) return firstCandidate

        return if (isSuitableType(secondCandidate, this)) {
            secondCandidate
        } else {
            firstCandidate
        }
    }

    context(c: Context)
    private fun specialResultForIntersectionType(firstCandidate: KotlinTypeMarker, secondCandidate: KotlinTypeMarker): KotlinTypeMarker? {
        if (firstCandidate.typeConstructor().isIntersection()) {
            if (!AbstractTypeChecker.isSubtypeOf(c, firstCandidate.toPublicType(), secondCandidate.toPublicType())) {
                return c.createTypeWithUpperBoundForIntersectionResult(firstCandidate, secondCandidate)
            }
        }

        return null
    }

    private fun KotlinTypeMarker.toPublicType(): KotlinTypeMarker =
        typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes) ?: this

    context(c: Context)
    private fun isSuitableType(resultType: KotlinTypeMarker, variableWithConstraints: VariableWithConstraints): Boolean {
        val filteredConstraints = variableWithConstraints.constraints.filter { it.type.isProperTypeForFixation() }

        // TODO(KT-68213) this loop is only used for checking of incomptible ILT approximations in K1
        // It shouldn't be necessary in K2
        // but removing it breaks compiler/fir/analysis-tests/testData/resolve/inference/kt53494.kt
        for (constraint in filteredConstraints) {
            if (!checkConstraint(constraint.type, constraint.kind, resultType)) return false
        }

        // if resultType is not Nothing
        if (trivialConstraintTypeInferenceOracle.isSuitableResultedType(resultType)) return true

        // Nothing and Nothing? is not allowed for reified parameters
        if (c.isReified(variableWithConstraints.typeVariable)) return false

        // It's ok to fix result to non-nullable Nothing and parameter is not reified
        if (!resultType.isNullableType()) return true

        return isNullableNothingMayBeConsideredAsSuitableResultType(filteredConstraints)
    }

    context(c: Context)
    private fun isNullableNothingMayBeConsideredAsSuitableResultType(constraints: List<Constraint>): Boolean = when {
        c.isK2 ->
            // There might be an assertion for green code that if `allUpperConstraintsAreFromBounds(constraints) == true` then
            // the single `Nothing?` lower bound constraint has Constraint::isNullabilityConstraint is set to false
            // because otherwise we would not start fixing the variable since it has no proper constraints.
            allUpperConstraintsAreFromBounds(constraints)
        else -> !isThereSingleLowerNullabilityConstraint(constraints)
    }

    private fun allUpperConstraintsAreFromBounds(constraints: List<Constraint>): Boolean =
        constraints.all {
            // Actually, at least for green code that should be an assertion that lower constraints (!isUpper) has `Nothing?` type
            // Because otherwise if we had `Nothing? <: T` and `SomethingElse <: T` than it would end with `SomethingElse? <: T`
            !it.kind.isUpper() || isFromTypeParameterUpperBound(it)
        }

    private fun isFromTypeParameterUpperBound(constraint: Constraint): Boolean =
        constraint.position.isFromDeclaredUpperBound || constraint.position.from is DeclaredUpperBoundConstraintPosition<*>

    private fun isThereSingleLowerNullabilityConstraint(constraints: List<Constraint>): Boolean {
        return constraints.singleOrNull { it.kind.isLower() }?.isNullabilityConstraint ?: false
    }

    context(c: Context)
    private fun VariableWithConstraints.findSubType(): KotlinTypeMarker? {
        val lowerConstraintTypes = prepareLowerConstraints(constraints)

        if (lowerConstraintTypes.isNotEmpty()) {
            // CST for a set of type variables is not defined
            if (lowerConstraintTypes.size > 1 &&
                lowerConstraintTypes.all { it.unwrapToSimpleTypeUsingLowerBound().isStubTypeForVariableInSubtypingOrCaptured() }
            ) {
                // This situation is only allowed to happen when semi-fixing for input types for OverloadResolutionByLambdaReturnType
                check(c.allowSemiFixationToOtherTypeVariables) {
                    "Only type-variable built constraints $lowerConstraintTypes found for $typeVariable"
                }
                return null
            }
            val types = sinkIntegerLiteralTypes(lowerConstraintTypes)
            var commonSuperType = NewCommonSuperTypeCalculator.commonSuperType(types)

            if (commonSuperType.contains { it.asRigidType()?.isStubTypeForVariableInSubtyping() == true }) {
                val typesWithoutStubs = types.filter { lowerType ->
                    !lowerType.contains { it.asRigidType()?.isStubTypeForVariableInSubtyping() == true }
                }

                when {
                    typesWithoutStubs.isNotEmpty() -> {
                        commonSuperType = NewCommonSuperTypeCalculator.commonSuperType(typesWithoutStubs)
                    }
                    // `typesWithoutStubs.isEmpty()` means that there are no lower constraints without type variables.
                    // It's only possible for the PCLA case, because otherwise none of the constraints would be considered as proper.
                    // So, we just get currently computed `commonSuperType` and substitute all local stub types
                    // with corresponding type variables.
                    c.outerSystemVariablesPrefixSize > 0 -> {
                        // outerSystemVariablesPrefixSize > 0 only for PCLA (K2)
                        @OptIn(K2Only::class)
                        commonSuperType = c.createSubstitutionFromSubtypingStubTypesToTypeVariables().safeSubstitute(commonSuperType)
                    }
                }
            }

            return commonSuperType
        }

        return null
    }

    context(c: Context)
    private fun prepareLowerConstraints(constraints: List<Constraint>): List<KotlinTypeMarker> {
        var atLeastOneProper = false
        var atLeastOneNonProper = false

        val lowerConstraintTypes = mutableListOf<KotlinTypeMarker>()

        for (constraint in constraints) {
            if (constraint.kind != ConstraintKind.LOWER) continue
            if (constraint.isNoInfer) continue

            val type = constraint.type

            lowerConstraintTypes.add(type)

            if (type.isProperTypeForFixation()) {
                atLeastOneProper = true
            } else {
                atLeastOneNonProper = true
            }
        }

        if (!atLeastOneProper) return emptyList()

        // PCLA slow path
        // We only allow using TVs fixation for nested PCLA calls
        if (c.outerSystemVariablesPrefixSize > 0) {
            val notFixedToStubTypesSubstitutor = c.buildNotFixedVariablesToStubTypesSubstitutor()
            return lowerConstraintTypes.map { notFixedToStubTypesSubstitutor.safeSubstitute(it) }
        }

        if (!atLeastOneNonProper) return lowerConstraintTypes

        val notFixedToStubTypesSubstitutor = c.buildNotFixedVariablesToStubTypesSubstitutor()

        return lowerConstraintTypes.map { if (it.isProperTypeForFixation()) it else notFixedToStubTypesSubstitutor.safeSubstitute(it) }
    }

    context(c: Context)
    private fun sinkIntegerLiteralTypes(types: List<KotlinTypeMarker>): List<KotlinTypeMarker> {
        return types.sortedBy { type ->
            val containsILT = type.contains { it.asRigidType()?.isIntegerLiteralType() ?: false }
            if (containsILT) 1 else 0
        }
    }

    context(c: Context)
    private fun computeUpperType(upperConstraints: List<Constraint>): KotlinTypeMarker {
        return if (languageVersionSettings.supportsFeature(LanguageFeature.AllowEmptyIntersectionsInResultTypeResolver)) {
            c.intersectTypes(upperConstraints.map { it.type })
        } else {
            val intersectionUpperType = c.intersectTypes(upperConstraints.map { it.type })
            val resultIsActuallyIntersection = intersectionUpperType.typeConstructor().isIntersection()

            val isThereUnwantedIntersectedTypes = if (resultIsActuallyIntersection) {
                val intersectionSupertypes = intersectionUpperType.typeConstructor().supertypes()
                val intersectionClasses = intersectionSupertypes.count {
                    it.typeConstructor().isClassTypeConstructor() && !it.typeConstructor().isInterface()
                }
                val areThereIntersectionFinalClasses = intersectionSupertypes.any { it.typeConstructor().isCommonFinalClassConstructor() }
                intersectionClasses > 1 || areThereIntersectionFinalClasses
            } else false

            val upperType = if (isThereUnwantedIntersectedTypes) {
                /*
                 * We shouldn't infer a type variable into the intersection type if there is an explicit expected type,
                 * otherwise it can lead to something like this:
                 *
                 * fun <T : String> materialize(): T = null as T
                 * val bar: Int = materialize() // no errors, T is inferred into String & Int
                 */
                val filteredUpperConstraints = upperConstraints.filterNot { it.isExpectedTypePosition() }.map { it.type }
                if (filteredUpperConstraints.isNotEmpty()) c.intersectTypes(filteredUpperConstraints) else intersectionUpperType
            } else intersectionUpperType
            upperType
        }
    }

    context(c: Context)
    private fun VariableWithConstraints.findSuperType(): KotlinTypeMarker? {
        var hasNotNull = false
        val upperConstraints = constraints.filter {
            if (it.kind != ConstraintKind.UPPER) return@filter false
            if (it.isProperConstraint()) return@filter true
            // Non-proper constraints like recursive upper bounds can still contribute nullability information
            if (!it.type.isNullableType()) hasNotNull = true
            false
        }

        if (upperConstraints.isNotEmpty()) {
            return computeUpperType(upperConstraints).let {
                if (hasNotNull && it.isNullableType() && languageVersionSettings.supportsFeature(LanguageFeature.InferenceEnhancementsIn23)) {
                    it.makeDefinitelyNotNullOrNotNull()
                } else {
                    it
                }
            }
        }

        return null
    }

    context(c: Context)
    private fun Constraint.isProperConstraint(): Boolean {
        return type.isProperTypeForFixation() && !isNoInfer
    }

    context(c: Context)
    private fun KotlinTypeMarker.isProperTypeForFixation(): Boolean =
        isProperTypeForFixation(c.notFixedTypeVariables.keys) { c.isProperType(it) }

    context(c: Context)
    fun findResultIfThereIsEqualsConstraint(variableWithConstraints: VariableWithConstraints, isStrictMode: Boolean): KotlinTypeMarker? {
        val properEqualityConstraints = variableWithConstraints.constraints.filter {
            it.kind == ConstraintKind.EQUALITY && it.isProperConstraint()
        }

        return representativeFromEqualityConstraints(properEqualityConstraints, isStrictMode)
    }

    // Discriminate integer literal types as they are less specific than separate integer types (Int, Short...)
    context(c: Context)
    private fun representativeFromEqualityConstraints(
        constraints: List<Constraint>,
        // Allow only types not-containing ILT and which might work as a representative of other ones from EQ constraints
        // TODO: Consider making it always `true` (see KT-70062)
        isStrictMode: Boolean
    ): KotlinTypeMarker? {
        if (constraints.isEmpty()) return null

        val constraintTypes = constraints.map { it.type }
        val nonLiteralTypes = constraintTypes.filter { constraintType ->
            if (isStrictMode)
                !constraintType.contains { it.typeConstructor().isIntegerLiteralTypeConstructor() }
            else
                !constraintType.typeConstructor().isIntegerLiteralTypeConstructor()
        }

        nonLiteralTypes.singleBestRepresentative()?.let { return it }

        if (isStrictMode) return null

        return constraintTypes.singleBestRepresentative()
            ?: constraintTypes.first() // seems like constraint system has contradiction
    }
}
