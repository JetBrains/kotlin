/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator.ResolveDirection
import org.jetbrains.kotlin.resolve.calls.inference.extractTypeForGivenRecursiveTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.hasRecursiveTypeParametersWithGivenSelfType
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.*

class ResultTypeResolver(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    private val languageVersionSettings: LanguageVersionSettings
) {
    interface Context : TypeSystemInferenceExtensionContext {
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
        val outerSystemVariablesPrefixSize: Int
        fun isProperType(type: KotlinTypeMarker): Boolean
        fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
        fun isReified(variable: TypeVariableMarker): Boolean
    }

    private val isTypeInferenceForSelfTypesSupported: Boolean
        get() = languageVersionSettings.supportsFeature(LanguageFeature.TypeInferenceOnCallsWithSelfTypes)

    private fun Context.getDefaultTypeForSelfType(
        constraints: List<Constraint>,
        typeVariable: TypeVariableMarker
    ): KotlinTypeMarker? {
        val typeVariableConstructor = typeVariable.freshTypeConstructor() as TypeVariableTypeConstructorMarker
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
        typeVariable: TypeVariableMarker
    ): KotlinTypeMarker {
        if (isTypeInferenceForSelfTypesSupported) {
            getDefaultTypeForSelfType(constraints, typeVariable)?.let { return it }
        }

        return if (direction == ResolveDirection.TO_SUBTYPE) nothingType() else nullableAnyType()
    }

    fun findResultType(c: Context, variableWithConstraints: VariableWithConstraints, direction: ResolveDirection): KotlinTypeMarker {
        findResultTypeOrNull(c, variableWithConstraints, direction)?.let { return it }

        // no proper constraints
        return c.getDefaultType(direction, variableWithConstraints.constraints, variableWithConstraints.typeVariable)
    }

    fun findResultTypeOrNull(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        direction: ResolveDirection,
    ): KotlinTypeMarker? {
        val resultTypeFromEqualConstraint = findResultIfThereIsEqualsConstraint(c, variableWithConstraints)
        if (resultTypeFromEqualConstraint != null) {
            with(c) {
                if (!isK2 || !resultTypeFromEqualConstraint.contains { type ->
                        type.typeConstructor().isIntegerLiteralConstantTypeConstructor()
                    }
                ) {
                    // In K2, we don't return here ILT-based types immediately
                    return resultTypeFromEqualConstraint
                }
            }
        }

        val subType = c.findSubType(variableWithConstraints)
        val superType = c.findSuperType(variableWithConstraints)

        val similarCapturedTypesInK2 = with(c) {
            isK2 && similarOrCloselyBoundCapturedTypes(subType, superType)
        }

        /**
         * The special logic for handling similar captured types in K2 is needed because of the following.
         * Currently, it's allowed to squash LOWER(type) and UPPER(type) constraints with the same type
         * into one EQUALS(type) constraint.
         * E.g. it's possible for constraints like LOWER(SomeCapturedType!) & UPPER(SomeCapturedTypes!).
         * In such a situation [ResultTypeResolver] infers a relevant type variable into SomeCapturedType!,
         * without any approximation.
         * However, complex handling of DNN/non-DNN types sometimes lead to a situation (see e.g. KT-50134 example)
         * when we have a pair of LOWER(SomeCapturedType&Any..SomeCapturedType?) and UPPER(SomeCapturedType!).
         * These constraints use almost the same type, but they cannot be squashed.
         * Moreover, [ResultTypeResolver] approximates captured from expression types when they came from UPPER or LOWER constraint.
         * See [AbstractTypeApproximator.approximateToSuperType] and [AbstractTypeApproximator.approximateToSubType] calls below.
         * As a result, [ResultTypeResolver] infers a relevant type variable to e.g. Any!,
         * despite the fact the situation is almost the same as in case with matched constraints.
         * This can produce unexpected NEW_INFERENCE_ERROR because other constraints are unmatched.
         *
         * To handle this situation, approximation of captured types for this case with similar constraints is disabled in K2.
         */
        // TODO: consider skipping captured types approximation unconditionally (KT-66346)
        val preparedSubType = when {
            subType == null -> null
            similarCapturedTypesInK2 -> subType
            /**
             *
             * fun <T> Array<out T>.intersect(other: Iterable<T>) {
             *      val set = toMutableSet()
             *      set.retainAll(other)
             * }
             * fun <X> Array<out X>.toMutableSet(): MutableSet<X> = ...
             * fun <Y> MutableCollection<in Y>.retainAll(elements: Iterable<Y>) {}
             *
             * Here, when we solve type system for `toMutableSet` we have the following constrains:
             * Array<C(out T)> <: Array<out X> => C(out X) <: T.
             * If we fix it to T = C(out X) then return type of `toMutableSet()` will be `MutableSet<C(out X)>`
             * and type of variable `set` will be `MutableSet<out T>` and the following line will have contradiction.
             *
             * To fix this problem when we fix variable, we will approximate captured types before fixation.
             *
             */
            else -> typeApproximator.approximateToSuperType(subType, TypeApproximatorConfiguration.InternalTypesApproximation) ?: subType
        }

        // TODO: consider skipping captured types approximation unconditionally (KT-66346)
        val preparedSuperType = when {
            superType == null -> null
            similarCapturedTypesInK2 -> superType
            c.isK2 && c.hasRecursiveTypeParametersWithGivenSelfType(superType.typeConstructor(c)) -> superType
            else -> typeApproximator.approximateToSubType(superType, TypeApproximatorConfiguration.InternalTypesApproximation) ?: superType
            // Super type should be the most flexible, sub type should be the least one
        }.makeFlexibleIfNecessary(c, variableWithConstraints.constraints)

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

    /**
     * This function is used to determine if a resulting captured type should be approximated before returning it as a result type.
     *
     * In general, it's good not to approximate resulting captured types at all (see KT-66346).
     * Strictly speaking, such an approximation can build a type which is out of given constraints at all, e.g. (see KT-67221):
     *
     * Given CapturedType(out Generic<CapturedType(*)>) <: T <: Generic<CapturedType(*)>,
     * we can produce just Generic<out Any> and break the constraint system.
     *
     * However, avoiding approximation can also have drawbacks.
     * In cases like CapturedType(out String) <: T <: String (constraint system from KT-54077),
     * we can approximate the result to String safely, and if we keep CapturedType(out String) instead,
     * we get type mismatch error later. Also, extra captured types can break diagnostics like REIFIED_TYPE_FORBIDDEN_SUBSTITUTION.
     *
     * So currently (before full KT-66346 implementation), we are doing something intermediate
     * and this function should return true if approximation is not needed.
     *
     * Currently, it does so for "similar captured types": it's a pair of captured-type based types like
     * CapturedType&Any..CapturedType? and CapturedType..CapturedType?.
     * Type constructors of lower/upper bound of both types should be the same captured types, to get true result here.
     *
     * Also, true is returned for "closely bound captured types":
     * in this case a captured [subType] is inherited from a captured-containing [superType].
     *
     * @return true for similar or closely bound [subType] and [superType] captured types, which aren't approximated after that.
     */
    private fun Context.similarOrCloselyBoundCapturedTypes(subType: KotlinTypeMarker?, superType: KotlinTypeMarker?): Boolean {
        if (subType == null) return false
        if (superType == null) return false
        val subTypeLowerConstructor = subType.lowerBoundIfFlexible().typeConstructor()
        if (!subTypeLowerConstructor.isCapturedTypeConstructor()) return false

        // If two captured types or captured-containing types are already in subtyping relation,
        // we shouldn't do approximation, otherwise this subtyping relation becomes broken
        // E.g. subType = CapturedType(out Generic<CapturedType(*)>), superType = Generic<CapturedType(*)>
        // Important: both superType/subType contain the same captured type inside -- CapturedType(*) for the case above
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
            is SimpleTypeMarker -> {
                if (constraints.any { it.type.typeConstructor().isTypeVariable() && it.type.hasFlexibleNullability() }) {
                    createFlexibleType(type.makeSimpleTypeDefinitelyNotNullOrNotNull(), type.withNullability(true))
                } else type
            }
            else -> type
        }
    }

    private fun Context.resultType(
        firstCandidate: KotlinTypeMarker?,
        secondCandidate: KotlinTypeMarker?,
        variableWithConstraints: VariableWithConstraints
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
                return createTypeWithAlternativeForIntersectionResult(firstCandidate, secondCandidate)
            }
        }

        return null
    }

    private fun KotlinTypeMarker.toPublicType(): KotlinTypeMarker =
        typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes) ?: this

    private fun Context.isSuitableType(resultType: KotlinTypeMarker, variableWithConstraints: VariableWithConstraints): Boolean {
        val filteredConstraints = variableWithConstraints.constraints.filter { isProperTypeForFixation(it.type) }
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
            // TODO Improve handling of flexible types with recursive captured type arguments to not produce giant multi-level-deep types KT-65704
            var commonSuperType = computeCommonSuperType(types)

            if (commonSuperType.contains { it.asSimpleType()?.isStubTypeForVariableInSubtyping() == true }) {
                val typesWithoutStubs = types.filter { lowerType ->
                    !lowerType.contains { it.asSimpleType()?.isStubTypeForVariableInSubtyping() == true }
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

            val containsILT = type.contains { it.asSimpleType()?.isIntegerLiteralType() ?: false }
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
        val upperConstraints =
            variableWithConstraints.constraints.filter { it.kind == ConstraintKind.UPPER && this@findSuperType.isProperTypeForFixation(it.type) }

        if (upperConstraints.isNotEmpty()) {
            return computeUpperType(upperConstraints)
        }

        return null
    }

    private fun Context.isProperTypeForFixation(type: KotlinTypeMarker): Boolean =
        isProperTypeForFixation(type, notFixedTypeVariables.keys) { isProperType(it) }

    private fun findResultIfThereIsEqualsConstraint(c: Context, variableWithConstraints: VariableWithConstraints): KotlinTypeMarker? =
        with(c) {
            val properEqualityConstraints = variableWithConstraints.constraints.filter {
                it.kind == ConstraintKind.EQUALITY && c.isProperTypeForFixation(it.type)
            }

            return c.representativeFromEqualityConstraints(properEqualityConstraints)
        }

    // Discriminate integer literal types as they are less specific than separate integer types (Int, Short...)
    private fun Context.representativeFromEqualityConstraints(constraints: List<Constraint>): KotlinTypeMarker? {
        if (constraints.isEmpty()) return null

        val constraintTypes = constraints.map { it.type }
        val nonLiteralTypes = constraintTypes.filter { !it.typeConstructor().isIntegerLiteralTypeConstructor() }
        return nonLiteralTypes.singleBestRepresentative()
            ?: constraintTypes.singleBestRepresentative()
            ?: constraintTypes.first() // seems like constraint system has contradiction
    }
}
