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

    private fun Context.getDefaultTypeForSelfType(
        constraints: List<Constraint>,
        typeVariable: TypeVariableMarker,
    ): KotlinTypeMarker? {
        val typeVariableConstructor = typeVariable.freshTypeConstructor()
        val typesForRecursiveTypeParameters = constraints.mapNotNull { constraint ->
            if (constraint.position.from !is DeclaredUpperBoundConstraintPosition<*>) return@mapNotNull null
            val typeParameter = typeVariableConstructor.typeParameter ?: return@mapNotNull null
            extractTypeForGivenRecursiveTypeParameter(constraint.type, typeParameter)
        }.takeIf { it.isNotEmpty() } ?: return null

        return createCapturedStarProjectionForSelfType(typeVariableConstructor, typesForRecursiveTypeParameters)
    }

    private fun Context.getDefaultType(
        direction: ResolveDirection,
        constraints: List<Constraint>,
        typeVariable: TypeVariableMarker,
    ): KotlinTypeMarker {
        getDefaultTypeForSelfType(constraints, typeVariable)?.let { return it }

        return if (direction == ResolveDirection.TO_SUBTYPE) nothingType() else nullableAnyType()
    }

    fun findResultType(c: Context, variableWithConstraints: VariableWithConstraints, direction: ResolveDirection): KotlinTypeMarker {
        findResultTypeOrNull(c, variableWithConstraints, direction)?.let { return it }

        // no proper constraints
        return c.getDefaultType(direction, variableWithConstraints.constraints, variableWithConstraints.typeVariable)
    }

    private fun KotlinTypeMarker.approximateToSuperTypeOrSelf(c: Context, superTypeCandidate: KotlinTypeMarker?): KotlinTypeMarker {
        // In case we have an ILT as the subtype, we approximate it using the upper type as the expected type.
        // This is more precise than always approximating it to Int or UInt.
        // Note, we shouldn't have nested ILTs because they can only appear as a constraint on a type variable
        // that we would have fixed earlier.
        if (typeConstructor(c).isIntegerLiteralTypeConstructor(c)) {
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

    fun findResultTypeOrNull(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        direction: ResolveDirection,
    ): KotlinTypeMarker? {
        val resultTypeFromEqualConstraint = findResultIfThereIsEqualsConstraint(c, variableWithConstraints, isStrictMode = false)
        if (resultTypeFromEqualConstraint?.isAppropriateResultTypeFromEqualityConstraints(c) == true) return resultTypeFromEqualConstraint

        val subType = c.findSubType(variableWithConstraints)
        val superType = c.findSuperType(variableWithConstraints)

        val (preparedSubType, preparedSuperType) = if (c.isK2 && useImprovedCapturedTypeApproximation) {
            c.prepareSubAndSuperTypes(subType, superType, variableWithConstraints)
        } else {
            c.prepareSubAndSuperTypesLegacy(subType, superType, variableWithConstraints)
        }

        val resultTypeFromDirection = if (direction == ResolveDirection.TO_SUBTYPE || direction == ResolveDirection.UNKNOWN) {
            c.resultType(preparedSubType, preparedSuperType, variableWithConstraints)
        } else {
            c.resultType(preparedSuperType, preparedSubType, variableWithConstraints)
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
            with(c) { !resultTypeFromDirection.typeConstructor().isNothingConstructor() } &&
                    AbstractTypeChecker.isSubtypeOf(c, resultTypeFromDirection, resultTypeFromEqualConstraint) -> resultTypeFromDirection
            else -> resultTypeFromEqualConstraint
        }
    }

    private fun KotlinTypeMarker.isAppropriateResultTypeFromEqualityConstraints(
        c: Context,
    ): Boolean = with(c) {
        if (!isK2) return true

        // In K2, we don't allow fixing to a result type from EQ constraints if they contain ILTs
        !contains { type ->
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
    private fun Context.prepareSubAndSuperTypes(
        subType: KotlinTypeMarker?,
        superType: KotlinTypeMarker?,
        variableWithConstraints: VariableWithConstraints,
    ): Pair<KotlinTypeMarker?, KotlinTypeMarker?> {
        val approximatedSubType = subType?.approximateToSuperTypeOrSelf(this, superType)
        val approximatedSuperType = superType?.approximateToSubTypeOrSelf()

        val preparedSubType = when {
            approximatedSubType == null -> null
            shouldBeUsedWithoutApproximation(subType, approximatedSubType, variableWithConstraints, this) -> subType
            else -> approximatedSubType
        }

        val preparedSuperType = when {
            approximatedSuperType == null -> null
            shouldBeUsedWithoutApproximation(superType, approximatedSuperType, variableWithConstraints, this) -> superType
            hasRecursiveTypeParametersWithGivenSelfType(superType.typeConstructor(this)) -> superType
            else -> approximatedSuperType
            // Super type should be the most flexible, sub type should be the least one
        }.makeFlexibleIfNecessary(this, variableWithConstraints.constraints)

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
    private fun shouldBeUsedWithoutApproximation(
        resultType: KotlinTypeMarker,
        approximatedResultType: KotlinTypeMarker,
        variableWithConstraints: VariableWithConstraints,
        c: Context,
    ): Boolean {
        if (resultType === approximatedResultType || c.hasContradiction) return false

        // TODO(related to KT-64802) This if shouldn't be necessary but removing it breaks
        // compiler/testData/diagnostics/tests/unsignedTypes/conversions/inferenceForSignedAndUnsignedTypes.kt
        if (resultType.typeConstructor(c).isIntegerLiteralTypeConstructor(c)) return false

        return !c.isEqualityConstraintCompatible(approximatedResultType, variableWithConstraints.typeVariable.defaultType(c))
    }

    private fun Context.prepareSubAndSuperTypesLegacy(
        subType: KotlinTypeMarker?,
        superType: KotlinTypeMarker?,
        variableWithConstraints: VariableWithConstraints,
    ): Pair<KotlinTypeMarker?, KotlinTypeMarker?> {
        val similarCapturedTypesInK2 = with(this) {
            isK2 && similarOrCloselyBoundCapturedTypes(subType, superType)
        }

        val preparedSubType = when {
            subType == null -> null
            similarCapturedTypesInK2 -> subType
            else -> typeApproximator.approximateToSuperType(subType, TypeApproximatorConfiguration.InternalTypesApproximation) ?: subType
        }

        val preparedSuperType = when {
            superType == null -> null
            similarCapturedTypesInK2 -> superType
            isK2 && hasRecursiveTypeParametersWithGivenSelfType(superType.typeConstructor(this)) -> superType
            else -> typeApproximator.approximateToSubType(superType, TypeApproximatorConfiguration.InternalTypesApproximation) ?: superType
            // Super type should be the most flexible, sub type should be the least one
        }.makeFlexibleIfNecessary(this, variableWithConstraints.constraints)

        return preparedSubType to preparedSuperType
    }

    /**
     * Old heuristic used to determine when result types from lower/upper constraints should be approximated or not.
     *
     * Becomes obsolete after [LanguageFeature.ImprovedCapturedTypeApproximationInInference] is enabled.
     */
    private fun Context.similarOrCloselyBoundCapturedTypes(subType: KotlinTypeMarker?, superType: KotlinTypeMarker?): Boolean {
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
    private fun KotlinTypeMarker?.makeFlexibleIfNecessary(c: Context, constraints: List<Constraint>) = with(c) {
        when (val type = this@makeFlexibleIfNecessary) {
            is RigidTypeMarker -> {
                if (constraints.any { it.type.typeConstructor().isTypeVariable() && it.type.hasFlexibleNullability() }) {
                    createTrivialFlexibleTypeOrSelf(type.makeDefinitelyNotNullOrNotNull())
                } else type
            }
            else -> type
        }
    }

    private fun Context.resultType(
        firstCandidate: KotlinTypeMarker?,
        secondCandidate: KotlinTypeMarker?,
        variableWithConstraints: VariableWithConstraints,
    ): KotlinTypeMarker? {
        if (firstCandidate == null || secondCandidate == null) return firstCandidate ?: secondCandidate

        specialResultForIntersectionType(firstCandidate, secondCandidate)?.let { intersectionWithAlternative ->
            return intersectionWithAlternative
        }

        if (isSuitableType(firstCandidate, variableWithConstraints)) return firstCandidate

        return if (isSuitableType(secondCandidate, variableWithConstraints)) {
            secondCandidate
        } else {
            firstCandidate
        }
    }

    private fun Context.specialResultForIntersectionType(
        firstCandidate: KotlinTypeMarker,
        secondCandidate: KotlinTypeMarker,
    ): KotlinTypeMarker? {
        if (firstCandidate.typeConstructor().isIntersection()) {
            if (!AbstractTypeChecker.isSubtypeOf(this, firstCandidate.toPublicType(), secondCandidate.toPublicType())) {
                return createTypeWithUpperBoundForIntersectionResult(firstCandidate, secondCandidate)
            }
        }

        return null
    }

    private fun KotlinTypeMarker.toPublicType(): KotlinTypeMarker =
        typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes) ?: this

    private fun Context.isSuitableType(resultType: KotlinTypeMarker, variableWithConstraints: VariableWithConstraints): Boolean {
        val filteredConstraints = variableWithConstraints.constraints.filter { isProperConstraint(it) }

        // TODO(KT-68213) this loop is only used for checking of incomptible ILT approximations in K1
        // It shouldn't be necessary in K2
        // but removing it breaks compiler/fir/analysis-tests/testData/resolve/inference/kt53494.kt
        for (constraint in filteredConstraints) {
            if (!checkConstraint(this, constraint.type, constraint.kind, resultType)) return false
        }

        // if resultType is not Nothing
        if (trivialConstraintTypeInferenceOracle.isSuitableResultedType(resultType)) return true

        // Nothing and Nothing? is not allowed for reified parameters
        if (isReified(variableWithConstraints.typeVariable)) return false

        // It's ok to fix result to non-nullable Nothing and parameter is not reified
        if (!resultType.isNullableType()) return true

        return isNullableNothingMayBeConsideredAsSuitableResultType(filteredConstraints)
    }

    private fun Context.isNullableNothingMayBeConsideredAsSuitableResultType(constraints: List<Constraint>): Boolean = when {
        isK2 ->
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

    private fun Context.findSubType(variableWithConstraints: VariableWithConstraints): KotlinTypeMarker? {
        val lowerConstraintTypes = prepareLowerConstraints(variableWithConstraints.constraints)

        if (lowerConstraintTypes.isNotEmpty()) {
            val types = sinkIntegerLiteralTypes(lowerConstraintTypes)
            var commonSuperType = computeCommonSuperType(types)

            if (commonSuperType.contains { it.asRigidType()?.isStubTypeForVariableInSubtyping() == true }) {
                val typesWithoutStubs = types.filter { lowerType ->
                    !lowerType.contains { it.asRigidType()?.isStubTypeForVariableInSubtyping() == true }
                }

                when {
                    typesWithoutStubs.isNotEmpty() -> {
                        commonSuperType = computeCommonSuperType(typesWithoutStubs)
                    }
                    // `typesWithoutStubs.isEmpty()` means that there are no lower constraints without type variables.
                    // It's only possible for the PCLA case, because otherwise none of the constraints would be considered as proper.
                    // So, we just get currently computed `commonSuperType` and substitute all local stub types
                    // with corresponding type variables.
                    outerSystemVariablesPrefixSize > 0 -> {
                        // outerSystemVariablesPrefixSize > 0 only for PCLA (K2)
                        @OptIn(K2Only::class)
                        commonSuperType = createSubstitutionFromSubtypingStubTypesToTypeVariables().safeSubstitute(commonSuperType)
                    }
                }
            }

            return commonSuperType
        }

        return null
    }

    private fun Context.computeCommonSuperType(types: List<KotlinTypeMarker>): KotlinTypeMarker =
        with(NewCommonSuperTypeCalculator) { commonSuperType(types) }

    private fun Context.prepareLowerConstraints(constraints: List<Constraint>): List<KotlinTypeMarker> {
        var atLeastOneProper = false
        var atLeastOneNonProper = false

        val lowerConstraintTypes = mutableListOf<KotlinTypeMarker>()

        for (constraint in constraints) {
            if (constraint.kind != ConstraintKind.LOWER) continue
            if (constraint.isNoInfer) continue

            val type = constraint.type

            lowerConstraintTypes.add(type)

            if (isProperTypeForFixation(type)) {
                atLeastOneProper = true
            } else {
                atLeastOneNonProper = true
            }
        }

        if (!atLeastOneProper) return emptyList()

        // PCLA slow path
        // We only allow using TVs fixation for nested PCLA calls
        if (outerSystemVariablesPrefixSize > 0) {
            val notFixedToStubTypesSubstitutor = buildNotFixedVariablesToStubTypesSubstitutor()
            return lowerConstraintTypes.map { notFixedToStubTypesSubstitutor.safeSubstitute(it) }
        }

        if (!atLeastOneNonProper) return lowerConstraintTypes

        val notFixedToStubTypesSubstitutor = buildNotFixedVariablesToStubTypesSubstitutor()

        return lowerConstraintTypes.map { if (isProperTypeForFixation(it)) it else notFixedToStubTypesSubstitutor.safeSubstitute(it) }
    }

    private fun Context.sinkIntegerLiteralTypes(types: List<KotlinTypeMarker>): List<KotlinTypeMarker> {
        return types.sortedBy { type ->

            val containsILT = type.contains { it.asRigidType()?.isIntegerLiteralType() ?: false }
            if (containsILT) 1 else 0
        }
    }

    private fun Context.computeUpperType(upperConstraints: List<Constraint>): KotlinTypeMarker {
        return if (languageVersionSettings.supportsFeature(LanguageFeature.AllowEmptyIntersectionsInResultTypeResolver)) {
            intersectTypes(upperConstraints.map { it.type })
        } else {
            val intersectionUpperType = intersectTypes(upperConstraints.map { it.type })
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
                if (filteredUpperConstraints.isNotEmpty()) intersectTypes(filteredUpperConstraints) else intersectionUpperType
            } else intersectionUpperType
            upperType
        }
    }

    private fun Context.findSuperType(variableWithConstraints: VariableWithConstraints): KotlinTypeMarker? {
        val upperConstraints = variableWithConstraints.constraints.filter {
            it.kind == ConstraintKind.UPPER && isProperConstraint(it)
        }

        if (upperConstraints.isNotEmpty()) {
            return computeUpperType(upperConstraints)
        }

        return null
    }

    private fun Context.isProperConstraint(constraint: Constraint): Boolean {
        return isProperTypeForFixation(constraint.type) && !constraint.isNoInfer
    }

    private fun Context.isProperTypeForFixation(type: KotlinTypeMarker): Boolean =
        isProperTypeForFixation(type, notFixedTypeVariables.keys) { isProperType(it) }

    fun findResultIfThereIsEqualsConstraint(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        isStrictMode: Boolean,
    ): KotlinTypeMarker? {
        val properEqualityConstraints = variableWithConstraints.constraints.filter {
            it.kind == ConstraintKind.EQUALITY && c.isProperConstraint(it)
        }

        return c.representativeFromEqualityConstraints(properEqualityConstraints, isStrictMode)
    }

    // Discriminate integer literal types as they are less specific than separate integer types (Int, Short...)
    private fun Context.representativeFromEqualityConstraints(
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
