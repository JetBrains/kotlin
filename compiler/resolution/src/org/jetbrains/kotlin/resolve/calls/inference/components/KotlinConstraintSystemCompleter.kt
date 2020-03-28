/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.collections.LinkedHashSet

class KotlinConstraintSystemCompleter(
    private val resultTypeResolver: ResultTypeResolver,
    val variableFixationFinder: VariableFixationFinder,
) {
    private val postponedArgumentInputTypesResolver = PostponedArgumentInputTypesResolver(resultTypeResolver, variableFixationFinder)

    enum class ConstraintSystemCompletionMode {
        FULL,
        PARTIAL
    }

    interface Context : VariableFixationFinder.Context, ResultTypeResolver.Context {
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>
        override val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
        override val postponedTypeVariables: List<TypeVariableMarker>

        fun getBuilder(): ConstraintSystemBuilder

        // type can be proper if it not contains not fixed type variables
        fun canBeProper(type: KotlinTypeMarker): Boolean

        fun containsOnlyFixedOrPostponedVariables(type: KotlinTypeMarker): Boolean

        // mutable operations
        fun addError(error: KotlinCallDiagnostic)

        fun fixVariable(variable: TypeVariableMarker, resultType: KotlinTypeMarker, atom: ResolvedAtom?)

        fun asPostponedArgumentInputTypesResolverContext(): PostponedArgumentInputTypesResolver.Context
    }

    fun runCompletion(
        c: Context,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        diagnosticsHolder: KotlinDiagnosticsHolder,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        c.runCompletion(
            completionMode,
            topLevelAtoms,
            topLevelType,
            diagnosticsHolder,
            collectVariablesFromContext = false,
            analyze = analyze
        )
    }

    fun completeConstraintSystem(
        c: Context,
        topLevelType: UnwrappedType,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        c.runCompletion(
            ConstraintSystemCompletionMode.FULL,
            topLevelAtoms,
            topLevelType,
            diagnosticsHolder,
            collectVariablesFromContext = true,
        ) {
            error("Shouldn't be called in complete constraint system mode")
        }
    }

    // TODO: investigate type variables fixation order and incorporation depth (see `diagnostics/tests/inference/completion/postponedArgumentsAnalysis/lackOfDeepIncorporation.kt`)
    private fun Context.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        diagnosticsHolder: KotlinDiagnosticsHolder,
        collectVariablesFromContext: Boolean,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        completion@ while (true) {
            val postponedArguments = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)

            // Stage 1: analyze postponed arguments with fixed parameter types
            if (analyzeArgumentWithFixedParameterTypes(postponedArguments, analyze))
                continue

            val isThereAnyReadyForFixationVariable = isThereAnyReadyForFixationVariable(
                completionMode, topLevelAtoms, topLevelType, collectVariablesFromContext, postponedArguments
            )

            // If there aren't any postponed arguments and ready for fixation variables, then completion isn't needed: nothing to do
            if (postponedArguments.isEmpty() && !isThereAnyReadyForFixationVariable)
                break

            val postponedArgumentsWithRevisableType = postponedArguments.filterIsInstance<PostponedAtomWithRevisableExpectedType>()
            val dependencyProvider =
                TypeVariableDependencyInformationProvider(notFixedTypeVariables, postponedArguments, topLevelType, this)

            // Stage 2: collect parameter types for postponed arguments
            postponedArgumentInputTypesResolver.collectParameterTypesAndBuildNewExpectedTypes(
                asPostponedArgumentInputTypesResolverContext(), postponedArgumentsWithRevisableType, completionMode, dependencyProvider
            )

            if (completionMode == ConstraintSystemCompletionMode.FULL) {
                // Stage 3: fix variables for parameter types of all postponed arguments
                for (argument in postponedArguments) {
                    val wasFixedSomeVariable = postponedArgumentInputTypesResolver.fixNextReadyVariableForParameterTypeIfNeeded(
                        asPostponedArgumentInputTypesResolverContext(),
                        argument,
                        postponedArguments,
                        topLevelType,
                        topLevelAtoms,
                        dependencyProvider
                    )

                    if (wasFixedSomeVariable)
                        continue@completion
                }

                // Stage 4: create atoms with revised expected types if needed
                for (argument in postponedArgumentsWithRevisableType) {
                    postponedArgumentInputTypesResolver.transformToAtomWithNewFunctionalExpectedType(
                        asPostponedArgumentInputTypesResolverContext(), argument, diagnosticsHolder
                    )
                }
            }

            /*
             * We should get not analyzed postponed arguments again because they can be changed by
             * the stage of fixation type variables for parameters or analysing postponed arguments with fixed parameter types (see stage #1 and #4)
             */
            val revisedPostponedArguments = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)

            // Stage 5: analyze the next ready postponed argument
            if (analyzeNextReadyPostponedArgument(revisedPostponedArguments, completionMode, analyze))
                continue

            // Stage 6: fix type variables – fix if possible or report not enough information (if completion mode is full)
            val wasFixedSomeVariable = fixVariablesOrReportNotEnoughInformation(
                completionMode, topLevelAtoms, topLevelType, collectVariablesFromContext, revisedPostponedArguments, diagnosticsHolder
            )
            if (wasFixedSomeVariable)
                continue

            // Stage 7: force analysis of remaining not analyzed postponed arguments and rerun stages if there are
            if (completionMode == ConstraintSystemCompletionMode.FULL) {
                if (analyzeRemainingNotAnalyzedPostponedArgument(revisedPostponedArguments, analyze))
                    continue
            }

            break
        }
    }

    private fun Context.analyzeArgumentWithFixedParameterTypes(
        postponedArguments: List<PostponedResolvedAtom>,
        analyze: (PostponedResolvedAtom) -> Unit
    ): Boolean {
        val argumentWithFixedOrPostponedInputTypes = findPostponedArgumentWithFixedOrPostponedInputTypes(postponedArguments)

        if (argumentWithFixedOrPostponedInputTypes != null) {
            analyze(argumentWithFixedOrPostponedInputTypes)
            return true
        }

        return false
    }

    private fun Context.analyzeNextReadyPostponedArgument(
        postponedArguments: List<PostponedResolvedAtom>,
        completionMode: ConstraintSystemCompletionMode,
        analyze: (PostponedResolvedAtom) -> Unit
    ): Boolean {
        if (completionMode == ConstraintSystemCompletionMode.FULL) {
            val argumentWithTypeVariableAsExpectedType = findPostponedArgumentWithRevisableExpectedType(postponedArguments)

            if (argumentWithTypeVariableAsExpectedType != null) {
                analyze(argumentWithTypeVariableAsExpectedType)
                return true
            }
        }

        return analyzeArgumentWithFixedParameterTypes(postponedArguments, analyze)
    }

    private fun analyzeRemainingNotAnalyzedPostponedArgument(
        postponedArguments: List<PostponedResolvedAtom>,
        analyze: (PostponedResolvedAtom) -> Unit
    ): Boolean {
        val remainingNotAnalyzedPostponedArgument = postponedArguments.firstOrNull { !it.analyzed }

        if (remainingNotAnalyzedPostponedArgument != null) {
            analyze(remainingNotAnalyzedPostponedArgument)
            return true
        }

        return false
    }

    private fun findPostponedArgumentWithRevisableExpectedType(postponedArguments: List<PostponedResolvedAtom>) =
        postponedArguments.firstOrNull { argument -> argument is PostponedAtomWithRevisableExpectedType }

    private fun Context.findPostponedArgumentWithFixedOrPostponedInputTypes(postponedArguments: List<PostponedResolvedAtom>) =
        postponedArguments.firstOrNull { argument -> argument.inputTypes.all { containsOnlyFixedOrPostponedVariables(it) } }

    private fun fixVariable(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ResolvedAtom>
    ) {
        fixVariable(c, variableWithConstraints, TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN, topLevelAtoms)
    }

    private fun fixVariable(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection,
        topLevelAtoms: List<ResolvedAtom>
    ) {
        val resultType = resultTypeResolver.findResultType(c, variableWithConstraints, direction)
        val resolvedAtom = findResolvedAtomBy(variableWithConstraints.typeVariable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()
        c.fixVariable(variableWithConstraints.typeVariable, resultType, resolvedAtom)
    }

    private fun Context.fixVariablesOrReportNotEnoughInformation(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        collectVariablesFromContext: Boolean,
        postponedArguments: List<PostponedResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): Boolean {
        while (true) {
            val variableForFixation = getVariableReadyForFixation(
                completionMode,
                topLevelAtoms,
                topLevelType,
                collectVariablesFromContext,
                postponedArguments
            ) ?: break

            if (!variableForFixation.hasProperConstraint && completionMode == ConstraintSystemCompletionMode.PARTIAL)
                break

            val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)

            if (variableForFixation.hasProperConstraint) {
                fixVariable(this, variableWithConstraints, topLevelAtoms)
                return true
            } else {
                processVariableWhenNotEnoughInformation(this, variableWithConstraints, topLevelAtoms, diagnosticsHolder)
            }
        }

        return false
    }

    private fun processVariableWhenNotEnoughInformation(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        val typeVariable = variableWithConstraints.typeVariable
        val resolvedAtom = findResolvedAtomBy(typeVariable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()

        if (resolvedAtom != null) {
            c.addError(NotEnoughInformationForTypeParameter(typeVariable, resolvedAtom))
        }

        val resultErrorType = when {
            typeVariable is TypeVariableFromCallableDescriptor ->
                ErrorUtils.createUninferredParameterType(typeVariable.originalTypeParameter)
            typeVariable is TypeVariableForLambdaParameterType && typeVariable.atom is LambdaKotlinCallArgument -> {
                diagnosticsHolder.addDiagnostic(
                    NotEnoughInformationForLambdaParameter(typeVariable.atom, typeVariable.index)
                )
                ErrorUtils.createErrorType("Cannot infer lambda parameter type")
            }
            else -> ErrorUtils.createErrorType("Cannot infer type variable $typeVariable")
        }

        c.fixVariable(typeVariable, resultErrorType, resolvedAtom)
    }

    private fun Context.getOrderedAllTypeVariables(
        collectVariablesFromContext: Boolean,
        topLevelAtoms: List<ResolvedAtom>
    ): List<TypeConstructorMarker> {
        if (collectVariablesFromContext)
            return notFixedTypeVariables.keys.toList()

        fun getVariablesFromRevisedExpectedType(revisedExpectedType: KotlinType?) =
            revisedExpectedType?.arguments?.map { it.type.constructor }?.filterIsInstance<TypeVariableTypeConstructor>()

        fun ResolvedAtom.process(to: LinkedHashSet<TypeConstructor>) {
            val typeVariables = when (this) {
                is LambdaWithTypeVariableAsExpectedTypeAtom -> getVariablesFromRevisedExpectedType(revisedExpectedType).orEmpty()
                is ResolvedCallAtom -> freshVariablesSubstitutor.freshVariables.map { it.freshTypeConstructor }
                is PostponedCallableReferenceAtom ->
                    getVariablesFromRevisedExpectedType(revisedExpectedType).orEmpty() +
                            candidate?.freshSubstitutor?.freshVariables?.map { it.freshTypeConstructor }.orEmpty()
                is ResolvedCallableReferenceAtom -> candidate?.freshSubstitutor?.freshVariables?.map { it.freshTypeConstructor }.orEmpty()
                is ResolvedLambdaAtom -> listOfNotNull(typeVariableForLambdaReturnType?.freshTypeConstructor)
                else -> emptyList()
            }

            typeVariables.mapNotNullTo(to) {
                it.takeIf { notFixedTypeVariables.containsKey(it) }
            }

            /*
             * Hack for completing error candidates in delegate resolve
             */
            if (this is StubResolvedAtom && typeVariable in notFixedTypeVariables) {
                to += typeVariable
            }

            if (analyzed) {
                subResolvedAtoms?.forEach { it.process(to) }
            }
        }

        // Note that it's important to use Set here, because several atoms can share the same type variable
        val result = linkedSetOf<TypeConstructor>()
        for (primitive in topLevelAtoms) {
            primitive.process(result)
        }

        assert(result.size == notFixedTypeVariables.size) {
            val notFoundTypeVariables = notFixedTypeVariables.keys.toMutableSet().apply { removeAll(result) }
            "Not all type variables found: $notFoundTypeVariables"
        }

        return result.toList()
    }

    private fun Context.getVariableReadyForFixation(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        collectVariablesFromContext: Boolean,
        postponedArguments: List<PostponedResolvedAtom>
    ) = variableFixationFinder.findFirstVariableForFixation(
        this,
        getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
        postponedArguments,
        completionMode,
        topLevelType
    )

    private fun Context.isThereAnyReadyForFixationVariable(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        collectVariablesFromContext: Boolean,
        postponedArguments: List<PostponedResolvedAtom>
    ) = getVariableReadyForFixation(completionMode, topLevelAtoms, topLevelType, collectVariablesFromContext, postponedArguments) != null

    companion object {
        fun getOrderedNotAnalyzedPostponedArguments(topLevelAtoms: List<ResolvedAtom>): List<PostponedResolvedAtom> {
            fun ResolvedAtom.process(to: MutableList<PostponedResolvedAtom>) {
                to.addIfNotNull(this.safeAs<PostponedResolvedAtom>()?.takeUnless { it.analyzed })

                if (analyzed) {
                    subResolvedAtoms?.forEach { it.process(to) }
                }
            }

            val notAnalyzedArguments = arrayListOf<PostponedResolvedAtom>()
            for (primitive in topLevelAtoms) {
                primitive.process(notAnalyzedArguments)
            }

            return notAnalyzedArguments
        }

        fun findResolvedAtomBy(typeVariable: TypeVariableMarker, topLevelAtoms: List<ResolvedAtom>): ResolvedAtom? {
            fun ResolvedAtom.check(): ResolvedAtom? {
                val suitableCall = when (this) {
                    is ResolvedCallAtom -> typeVariable in freshVariablesSubstitutor.freshVariables
                    is ResolvedCallableReferenceAtom -> candidate?.freshSubstitutor?.freshVariables?.let { typeVariable in it } ?: false
                    is ResolvedLambdaAtom -> typeVariable == typeVariableForLambdaReturnType
                    else -> false
                }

                if (suitableCall) {
                    return this
                }

                subResolvedAtoms?.forEach { subResolvedAtom ->
                    subResolvedAtom.check()?.let { result -> return@check result }
                }

                return null
            }

            for (topLevelAtom in topLevelAtoms) {
                topLevelAtom.check()?.let { return it }
            }

            return null
        }
    }
}
