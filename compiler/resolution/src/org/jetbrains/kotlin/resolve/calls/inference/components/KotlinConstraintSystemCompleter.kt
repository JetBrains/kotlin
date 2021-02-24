/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.components.transformToResolvedLambda
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.model.safeSubstitute
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KotlinConstraintSystemCompleter(
    private val resultTypeResolver: ResultTypeResolver,
    val variableFixationFinder: VariableFixationFinder,
    private val postponedArgumentInputTypesResolver: PostponedArgumentInputTypesResolver,
) {
    fun runCompletion(
        c: ConstraintSystemCompletionContext,
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
        c: ConstraintSystemCompletionContext,
        topLevelType: UnwrappedType,
        topLevelAtoms: List<ResolvedAtom>,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        c.runCompletion(
            completionMode,
            topLevelAtoms,
            topLevelType,
            diagnosticsHolder,
            collectVariablesFromContext = true,
        ) {
            error("Shouldn't be called in complete constraint system mode")
        }
    }

    private fun ConstraintSystemCompletionContext.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        diagnosticsHolder: KotlinDiagnosticsHolder,
        collectVariablesFromContext: Boolean,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        val topLevelTypeVariables = topLevelType.extractTypeVariables()

        completion@ while (true) {
            // TODO
            val postponedArguments = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)
            if (completionMode == ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA && hasLambdaToAnalyze(postponedArguments)) return

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
            val wasBuiltNewExpectedTypeForSomeArgument = postponedArgumentInputTypesResolver.collectParameterTypesAndBuildNewExpectedTypes(
                asConstraintSystemCompletionContext(),
                postponedArgumentsWithRevisableType,
                completionMode,
                dependencyProvider,
                topLevelTypeVariables
            )

            if (wasBuiltNewExpectedTypeForSomeArgument)
                continue

            if (completionMode == ConstraintSystemCompletionMode.FULL) {
                // Stage 3: fix variables for parameter types of all postponed arguments
                for (argument in postponedArguments) {
                    val wasFixedSomeVariable = postponedArgumentInputTypesResolver.fixNextReadyVariableForParameterTypeIfNeeded(
                        asConstraintSystemCompletionContext(),
                        argument,
                        postponedArguments,
                        topLevelType,
                        dependencyProvider,
                    ) {
                        findResolvedAtomBy(it, topLevelAtoms) ?: topLevelAtoms.firstOrNull()
                    }

                    if (wasFixedSomeVariable)
                        continue@completion
                }

                // Stage 4: create atoms with revised expected types if needed
                for (argument in postponedArgumentsWithRevisableType) {
                    val wasTransformedSomeArgument = transformToAtomWithNewFunctionalExpectedType(
                        asConstraintSystemCompletionContext(), argument, diagnosticsHolder
                    )

                    if (wasTransformedSomeArgument)
                        continue@completion
                }
            }

            // Stage 5: analyze the next ready postponed argument
            if (analyzeNextReadyPostponedArgument(postponedArguments, completionMode, analyze))
                continue

            // Stage 6: fix type variables – fix if possible or report not enough information (if completion mode is full)
            val wasFixedSomeVariable = fixVariablesOrReportNotEnoughInformation(
                completionMode, topLevelAtoms, topLevelType, collectVariablesFromContext, postponedArguments, diagnosticsHolder
            )
            if (wasFixedSomeVariable)
                continue

            // Stage 7: force analysis of remaining not analyzed postponed arguments and rerun stages if there are
            if (completionMode == ConstraintSystemCompletionMode.FULL) {
                if (analyzeRemainingNotAnalyzedPostponedArgument(postponedArguments, analyze))
                    continue
            }

            break
        }
    }

    private fun transformToAtomWithNewFunctionalExpectedType(
        c: ConstraintSystemCompletionContext,
        argument: PostponedAtomWithRevisableExpectedType,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): Boolean = with(c) {
        val revisedExpectedType: UnwrappedType =
            argument.revisedExpectedType?.takeIf { it.isFunctionOrKFunctionWithAnySuspendability() }?.cast() ?: return false

        when (argument) {
            is PostponedCallableReferenceAtom ->
                CallableReferenceWithRevisedExpectedTypeAtom(argument.atom, revisedExpectedType).also {
                    argument.setAnalyzedResults(null, listOf(it))
                }
            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                argument.transformToResolvedLambda(c.getBuilder(), diagnosticsHolder, revisedExpectedType)
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }

        return true
    }

    private fun ConstraintSystemCompletionContext.analyzeArgumentWithFixedParameterTypes(
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

    private fun ConstraintSystemCompletionContext.analyzeNextReadyPostponedArgument(
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

    fun prepareLambdaAtomForFactoryPattern(
        atom: ResolvedLambdaAtom,
        candidate: KotlinResolutionCandidate,
        diagnosticsHolder: KotlinDiagnosticsHolder,
    ): ResolvedLambdaAtom {
        val returnVariable = TypeVariableForLambdaReturnType(candidate.callComponents.builtIns, "_R")
        val csBuilder = candidate.csBuilder
        csBuilder.registerVariable(returnVariable)
        val functionalType: KotlinType = csBuilder.buildCurrentSubstitutor().safeSubstitute(candidate.getSystem().asConstraintSystemCompleterContext(), atom.expectedType!!) as KotlinType
        val expectedType = KotlinTypeFactory.simpleType(
            functionalType.annotations,
            functionalType.constructor,
            functionalType.arguments.dropLast(1) + returnVariable.defaultType.asTypeProjection(),
            functionalType.isMarkedNullable
        )
        csBuilder.addSubtypeConstraint(
            expectedType,
            functionalType,
            ArgumentConstraintPositionImpl(atom.atom)
        )
        return atom.transformToResolvedLambda(csBuilder, diagnosticsHolder, expectedType, returnVariable)
    }

    private fun ConstraintSystemCompletionContext.hasLambdaToAnalyze(
        postponedArguments: List<PostponedResolvedAtom>
    ): Boolean {
        return analyzeArgumentWithFixedParameterTypes(postponedArguments) {}
    }

    private fun findPostponedArgumentWithRevisableExpectedType(postponedArguments: List<PostponedResolvedAtom>) =
        postponedArguments.firstOrNull { argument -> argument is PostponedAtomWithRevisableExpectedType }

    private fun ConstraintSystemCompletionContext.findPostponedArgumentWithFixedOrPostponedInputTypes(postponedArguments: List<PostponedResolvedAtom>) =
        postponedArguments.firstOrNull { argument -> argument.inputTypes.all { containsOnlyFixedOrPostponedVariables(it) } }

    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ResolvedAtom>
    ) {
        fixVariable(c, variableWithConstraints, TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN, topLevelAtoms)
    }

    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection,
        topLevelAtoms: List<ResolvedAtom>
    ) {
        val resultType = resultTypeResolver.findResultType(c, variableWithConstraints, direction)
        val variable = variableWithConstraints.typeVariable
        val resolvedAtom = findResolvedAtomBy(variable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()
        c.fixVariable(variable, resultType, FixVariableConstraintPositionImpl(variable, resolvedAtom))
    }

    private fun ConstraintSystemCompletionContext.fixVariablesOrReportNotEnoughInformation(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        collectVariablesFromContext: Boolean,
        postponedArguments: List<PostponedResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): Boolean {
        while (true) {
            val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
                this,
                getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
                postponedArguments,
                completionMode,
                topLevelType,
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
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        val typeVariable = variableWithConstraints.typeVariable
        val resolvedAtom = findResolvedAtomBy(typeVariable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()

        if (resolvedAtom != null) {
            c.addError(NotEnoughInformationForTypeParameterImpl(typeVariable, resolvedAtom))
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

        c.fixVariable(typeVariable, resultErrorType, FixVariableConstraintPositionImpl(typeVariable, resolvedAtom))
    }

    private fun ConstraintSystemCompletionContext.getOrderedAllTypeVariables(
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

    private fun ConstraintSystemCompletionContext.isThereAnyReadyForFixationVariable(
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
        topLevelType,
    ) != null

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
