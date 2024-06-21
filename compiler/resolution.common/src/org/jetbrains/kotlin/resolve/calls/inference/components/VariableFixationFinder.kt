/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.inference.ForkPointData
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode.PARTIAL
import org.jetbrains.kotlin.resolve.calls.inference.hasRecursiveTypeParametersWithGivenSelfType
import org.jetbrains.kotlin.resolve.calls.inference.isRecursiveTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.*

class VariableFixationFinder(
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    private val languageVersionSettings: LanguageVersionSettings,
) {
    interface Context : TypeSystemInferenceExtensionContext {
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
        val fixedTypeVariables: Map<TypeConstructorMarker, KotlinTypeMarker>
        val postponedTypeVariables: List<TypeVariableMarker>
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        /**
         * See [org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage.outerSystemVariablesPrefixSize]
         */
        val outerSystemVariablesPrefixSize: Int

        val outerTypeVariables: Set<TypeConstructorMarker>?
            get() =
                when {
                    outerSystemVariablesPrefixSize > 0 -> allTypeVariables.keys.take(outerSystemVariablesPrefixSize).toSet()
                    else -> null
                }

        /**
         * If not null, that property means that we should assume temporary them all as proper types when fixating some variables.
         *
         * By default, if that property is null, we assume all `allTypeVariables` as not proper.
         *
         * Currently, that is only used for `provideDelegate` resolution, see
         * [org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.fixInnerVariablesForProvideDelegateIfNeeded]
         */
        val typeVariablesThatAreCountedAsProperTypes: Set<TypeConstructorMarker>?

        fun isReified(variable: TypeVariableMarker): Boolean
    }

    class VariableForFixation(
        val variable: TypeConstructorMarker,
        private val hasProperConstraint: Boolean,
        private val hasDependencyOnOuterTypeVariable: Boolean = false,
    ) {
        val isReady: Boolean get() = hasProperConstraint && !hasDependencyOnOuterTypeVariable
    }

    fun findFirstVariableForFixation(
        c: Context,
        allTypeVariables: List<TypeConstructorMarker>,
        postponedKtPrimitives: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: KotlinTypeMarker,
    ): VariableForFixation? =
        c.findTypeVariableForFixation(allTypeVariables, postponedKtPrimitives, completionMode, topLevelType)

    class TypeVariableFixationReadiness(
        val main: TypeVariableFixationReadinessFactor,
        val additional: List<TypeVariableFixationReadinessFactor>? = null,
    ) : Comparable<TypeVariableFixationReadiness> {

        companion object Singletons {
            val FORBIDDEN =
                TypeVariableFixationReadiness(TypeVariableFixationReadinessFactor.FORBIDDEN)
            val WITHOUT_PROPER_ARGUMENT_CONSTRAINT =
                TypeVariableFixationReadiness(TypeVariableFixationReadinessFactor.WITHOUT_PROPER_ARGUMENT_CONSTRAINT)
            val OUTER_TYPE_VARIABLE_DEPENDENCY =
                TypeVariableFixationReadiness(TypeVariableFixationReadinessFactor.OUTER_TYPE_VARIABLE_DEPENDENCY)

            val READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES =
                TypeVariableFixationReadiness(TypeVariableFixationReadinessFactor.READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES)

            val READY_FOR_FIXATION_UPPER =
                TypeVariableFixationReadiness(TypeVariableFixationReadinessFactor.READY_FOR_FIXATION_UPPER)
            val READY_FOR_FIXATION_LOWER =
                TypeVariableFixationReadiness(TypeVariableFixationReadinessFactor.READY_FOR_FIXATION_LOWER)
            val READY_FOR_FIXATION =
                TypeVariableFixationReadiness(TypeVariableFixationReadinessFactor.READY_FOR_FIXATION)
            val READY_FOR_FIXATION_REIFIED =
                TypeVariableFixationReadiness(TypeVariableFixationReadinessFactor.READY_FOR_FIXATION_REIFIED)
        }

        override fun compareTo(other: TypeVariableFixationReadiness): Int {
            if (main != other.main) return main.compareTo(other.main)
            if (additional == null && other.additional == null) return 0

            val thisAdditional = additional.orEmpty()
            val otherAdditional = other.additional.orEmpty()

            var i = 0
            while (true) {
                // this has shorter additional list => this has greater readiness => returning positive
                if (i == thisAdditional.size) return otherAdditional.size - i
                // i < thisAdditional.size => this has some more additional factors => other has greater readiness => return -1
                if (i == otherAdditional.size) return -1

                val aV = thisAdditional[i]
                val bV = otherAdditional[i]
                if (aV != bV) {
                    return aV.compareTo(bV)
                }

                i++
            }
        }
    }

    enum class TypeVariableFixationReadinessFactor {
        FORBIDDEN,
        WITHOUT_PROPER_ARGUMENT_CONSTRAINT, // proper constraint from arguments -- not from upper bound for type parameters
        OUTER_TYPE_VARIABLE_DEPENDENCY,

        // Starting from here, there's at least some proper constraint, and the type variable is not related to any outer one.
        // Thus, it means that this variable is not forbidden to fix.
        // In resulting readiness, there might be more than one of those factors.
        READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES,
        WITH_COMPLEX_DEPENDENCY, // if type variable T has constraint with non fixed type variable inside (non-top-level): T <: Foo<S>
        ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER, // proper trivial constraint from arguments, Nothing <: T
        RELATED_TO_ANY_OUTPUT_TYPE,
        FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND,

        // The variable is totally ready to be fixed (has proper non-trivial constraints non-relevant to output types or upper bounds)
        // Only one of the factors below is assumed to be used at once.
        READY_FOR_FIXATION_UPPER,
        READY_FOR_FIXATION_LOWER,
        READY_FOR_FIXATION,
        READY_FOR_FIXATION_REIFIED,
    }

    private val inferenceCompatibilityModeEnabled: Boolean
        get() = languageVersionSettings.supportsFeature(LanguageFeature.InferenceCompatibility)

    private val isTypeInferenceForSelfTypesSupported: Boolean
        get() = languageVersionSettings.supportsFeature(LanguageFeature.TypeInferenceOnCallsWithSelfTypes)

    private fun Context.getTypeVariableReadiness(
        variable: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): TypeVariableFixationReadiness {

        val resultingSetOfFactors = mutableListOf<TypeVariableFixationReadinessFactor>()

        when {
            !notFixedTypeVariables.contains(variable) || dependencyProvider.isVariableRelatedToTopLevelType(variable) ||
                    variableHasUnprocessedConstraintsInForks(variable) ->
                return TypeVariableFixationReadiness.FORBIDDEN
            isTypeInferenceForSelfTypesSupported && areAllProperConstraintsSelfTypeBased(variable) ->
                resultingSetOfFactors.add(TypeVariableFixationReadinessFactor.READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES)
            !variableHasProperArgumentConstraints(variable) -> return TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT
            dependencyProvider.isRelatedToOuterTypeVariable(variable) -> return TypeVariableFixationReadiness.OUTER_TYPE_VARIABLE_DEPENDENCY
        }

        if (!isK2 && resultingSetOfFactors.isNotEmpty()) return TypeVariableFixationReadiness.READY_FOR_FIXATION_DECLARED_UPPER_BOUND_WITH_SELF_TYPES

        if (hasDependencyToOtherTypeVariables(variable)) {
            resultingSetOfFactors.add(TypeVariableFixationReadinessFactor.WITH_COMPLEX_DEPENDENCY)
        }

        if (allConstraintsTrivialOrNonProper(variable)) {
            resultingSetOfFactors.add(TypeVariableFixationReadinessFactor.ALL_CONSTRAINTS_TRIVIAL_OR_NON_PROPER)
        }

        if (dependencyProvider.isVariableRelatedToAnyOutputType(variable)) {
            resultingSetOfFactors.add(TypeVariableFixationReadinessFactor.RELATED_TO_ANY_OUTPUT_TYPE)
        }

        if (variableHasOnlyIncorporatedConstraintsFromDeclaredUpperBound(variable)) {
            resultingSetOfFactors.add(TypeVariableFixationReadinessFactor.FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND)
        }

        if (resultingSetOfFactors.isNotEmpty()) {
            if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                check(resultingSetOfFactors.sorted() == resultingSetOfFactors)
            }

            if (!isK2) return TypeVariableFixationReadiness(resultingSetOfFactors.first())

            return TypeVariableFixationReadiness(resultingSetOfFactors.first(), resultingSetOfFactors.drop(1))
        }

        return when {
            isReified(variable) -> TypeVariableFixationReadiness.READY_FOR_FIXATION_REIFIED
            inferenceCompatibilityModeEnabled -> {
                when {
                    variableHasLowerNonNothingProperConstraint(variable) -> TypeVariableFixationReadiness.READY_FOR_FIXATION_LOWER
                    else -> TypeVariableFixationReadiness.READY_FOR_FIXATION_UPPER
                }
            }
            else -> TypeVariableFixationReadiness.READY_FOR_FIXATION
        }
    }

    private fun Context.variableHasUnprocessedConstraintsInForks(variableConstructor: TypeConstructorMarker): Boolean {
        if (constraintsFromAllForkPoints.isEmpty()) return false

        for ((_, forkPointData) in constraintsFromAllForkPoints) {
            for (constraints in forkPointData) {
                for ((typeVariableFromConstraint, constraint) in constraints) {
                    if (typeVariableFromConstraint.freshTypeConstructor() == variableConstructor) return true
                    if (containsTypeVariable(constraint.type, variableConstructor)) return true
                }
            }
        }

        return false
    }

    fun isTypeVariableHasProperConstraint(
        context: Context,
        typeVariable: TypeConstructorMarker,
    ): Boolean {
        return with(context) {
            val dependencyProvider = TypeVariableDependencyInformationProvider(
                notFixedTypeVariables, emptyList(), topLevelType = null, context
            )
            when (getTypeVariableReadiness(typeVariable, dependencyProvider)) {
                TypeVariableFixationReadiness.FORBIDDEN, TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT -> false
                else -> true
            }
        }
    }

    private fun Context.allConstraintsTrivialOrNonProper(variable: TypeConstructorMarker): Boolean {
        return notFixedTypeVariables[variable]?.constraints?.all { constraint ->
            isLowerNothingOrNullableNothing(constraint) || !isProperArgumentConstraint(constraint)
        } ?: false
    }

    // The idea is to add knowledge that constraint `Nothing(?) <: T` is quite useless and
    // it's totally fine to go and resolve postponed argument without fixation T to Nothing(?).
    // In other words, constraint `Nothing(?) <: T` is *not* proper
    private fun Context.isLowerNothingOrNullableNothing(constraint: Constraint): Boolean =
        constraint.kind == ConstraintKind.LOWER && constraint.type.typeConstructor().isNothingConstructor()

    private fun Context.variableHasOnlyIncorporatedConstraintsFromDeclaredUpperBound(variable: TypeConstructorMarker): Boolean {
        val constraints = notFixedTypeVariables[variable]?.constraints ?: return false

        return constraints.filter { isProperArgumentConstraint(it) }.all { it.position.isFromDeclaredUpperBound }
    }

    private fun Context.findTypeVariableForFixation(
        allTypeVariables: List<TypeConstructorMarker>,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: KotlinTypeMarker,
    ): VariableForFixation? {
        if (allTypeVariables.isEmpty()) return null

        val dependencyProvider = TypeVariableDependencyInformationProvider(
            notFixedTypeVariables, postponedArguments, topLevelType.takeIf { completionMode == PARTIAL }, this,
        )

        val candidate =
            allTypeVariables.maxByOrNull { getTypeVariableReadiness(it, dependencyProvider) } ?: return null

        return when (getTypeVariableReadiness(candidate, dependencyProvider)) {
            TypeVariableFixationReadiness.FORBIDDEN -> null
            TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT -> VariableForFixation(candidate, false)
            TypeVariableFixationReadiness.OUTER_TYPE_VARIABLE_DEPENDENCY ->
                VariableForFixation(candidate, hasProperConstraint = true, hasDependencyOnOuterTypeVariable = true)

            else -> VariableForFixation(candidate, true)
        }
    }

    private fun Context.hasDependencyToOtherTypeVariables(typeVariable: TypeConstructorMarker): Boolean {
        for (constraint in notFixedTypeVariables[typeVariable]?.constraints ?: return false) {
            val dependencyPresenceCondition = { type: KotlinTypeMarker ->
                type.typeConstructor() != typeVariable && notFixedTypeVariables.containsKey(type.typeConstructor())
            }
            if (constraint.type.lowerBoundIfFlexible().argumentsCount() != 0 && constraint.type.contains(dependencyPresenceCondition))
                return true
        }
        return false
    }

    private fun Context.variableHasProperArgumentConstraints(variable: TypeConstructorMarker): Boolean {
        val constraints = notFixedTypeVariables[variable]?.constraints ?: return false
        // temporary hack to fail calls which contain callable references resolved though OI with uninferred type parameters
        val areThereConstraintsWithUninferredTypeParameter = constraints.any { c -> c.type.contains { it.isUninferredParameter() } }
        return constraints.any { isProperArgumentConstraint(it) } && !areThereConstraintsWithUninferredTypeParameter
    }

    private fun Context.isProperArgumentConstraint(c: Constraint) =
        isProperType(c.type)
                && c.position.initialConstraint.position !is DeclaredUpperBoundConstraintPosition<*>
                && !c.isNullabilityConstraint

    private fun Context.isProperType(type: KotlinTypeMarker): Boolean =
        isProperTypeForFixation(type, notFixedTypeVariables.keys) { t -> !t.contains { isNotFixedRelevantVariable(it) } }

    private fun Context.isNotFixedRelevantVariable(it: KotlinTypeMarker): Boolean {
        val key = it.typeConstructor()
        if (!notFixedTypeVariables.containsKey(key)) return false
        if (typeVariablesThatAreCountedAsProperTypes?.contains(key) == true) return false
        return true
    }

    private fun Context.isReified(variable: TypeConstructorMarker): Boolean =
        notFixedTypeVariables[variable]?.typeVariable?.let { isReified(it) } ?: false

    private fun Context.variableHasLowerNonNothingProperConstraint(variable: TypeConstructorMarker): Boolean {
        val constraints = notFixedTypeVariables[variable]?.constraints ?: return false

        return constraints.any {
            it.kind.isLower() && isProperArgumentConstraint(it) && !it.type.typeConstructor().isNothingConstructor()
        }
    }

    private fun Context.isSelfTypeConstraint(constraint: Constraint): Boolean {
        val typeConstructor = constraint.type.typeConstructor()
        return constraint.position.from is DeclaredUpperBoundConstraintPosition<*>
                && (hasRecursiveTypeParametersWithGivenSelfType(typeConstructor) || isRecursiveTypeParameter(typeConstructor))
    }

    private fun Context.areAllProperConstraintsSelfTypeBased(variable: TypeConstructorMarker): Boolean {
        val constraints = notFixedTypeVariables[variable]?.constraints?.takeIf { it.isNotEmpty() } ?: return false

        var hasSelfTypeConstraint = false
        var hasOtherProperConstraint = false

        for (constraint in constraints) {
            if (isSelfTypeConstraint(constraint)) {
                hasSelfTypeConstraint = true
            }
            if (isProperArgumentConstraint(constraint)) {
                hasOtherProperConstraint = true
            }
            if (hasSelfTypeConstraint && hasOtherProperConstraint) break
        }

        return hasSelfTypeConstraint && !hasOtherProperConstraint
    }
}

/**
 * Returns `false` for fixed type variables types even if `isProper(type) == true`
 * Thus allowing only non-TVs types to be used for fixation on top level.
 * While this limitation is important, it doesn't really limit final results because when we have a constraint like T <: E or E <: T
 * and we're going to fix T into E, we assume that if E has some other constraints, they are being incorporated to T, so we would choose
 * them instead of E itself.
 */
inline fun TypeSystemInferenceExtensionContext.isProperTypeForFixation(
    type: KotlinTypeMarker,
    notFixedTypeVariables: Set<TypeConstructorMarker>,
    isProper: (KotlinTypeMarker) -> Boolean
): Boolean {
    // We don't allow fixing T into any top-level TV type, like T := F or T := F & Any
    // Even if F is considered as a proper by `isProper` (e.g., it belongs to an outer CS)
    // But at the same time, we don't forbid fixing into T := MutableList<F>
    if (type.typeConstructor() in notFixedTypeVariables) return false

    return isProper(type) && extractProjectionsForAllCapturedTypes(type).all(isProper)
}

fun TypeSystemInferenceExtensionContext.extractProjectionsForAllCapturedTypes(baseType: KotlinTypeMarker): Set<KotlinTypeMarker> {
    if (baseType.isFlexible()) {
        val flexibleType = baseType.asFlexibleType()!!
        return buildSet {
            addAll(extractProjectionsForAllCapturedTypes(flexibleType.lowerBound()))
            addAll(extractProjectionsForAllCapturedTypes(flexibleType.upperBound()))
        }
    }
    val simpleBaseType = baseType.asSimpleType()?.originalIfDefinitelyNotNullable()

    return buildSet {
        val projectionType = if (simpleBaseType is CapturedTypeMarker) {
            val typeArgument = simpleBaseType.typeConstructorProjection().takeIf { !it.isStarProjection() } ?: return@buildSet
            typeArgument.getType().also(::add)
        } else baseType
        val argumentsCount = projectionType.argumentsCount().takeIf { it != 0 } ?: return@buildSet

        for (i in 0 until argumentsCount) {
            val typeArgument = projectionType.getArgument(i).takeIf { !it.isStarProjection() } ?: continue
            addAll(extractProjectionsForAllCapturedTypes(typeArgument.getType()))
        }
    }
}

fun TypeSystemInferenceExtensionContext.containsTypeVariable(type: KotlinTypeMarker, typeVariable: TypeConstructorMarker): Boolean {
    if (type.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable }) return true

    val typeProjections = extractProjectionsForAllCapturedTypes(type)

    return typeProjections.any { typeProjectionsType ->
        typeProjectionsType.contains { it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable }
    }
}
