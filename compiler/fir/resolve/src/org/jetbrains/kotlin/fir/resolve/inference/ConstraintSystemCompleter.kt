/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.processCandidatesAndPostponedAtoms
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class ConstraintSystemCompleter(components: BodyResolveComponents) {
    private val inferenceComponents = components.session.inferenceComponents
    private val variableFixationFinder = inferenceComponents.variableFixationFinder
    private val postponedArgumentsInputTypesResolver = inferenceComponents.postponedArgumentInputTypesResolver
    private val languageVersionSettings = components.session.languageVersionSettings

    fun interface PostponedAtomAnalyzer {
        fun analyze(postponedResolvedAtom: ConePostponedResolvedAtom, withPCLASession: Boolean)
    }

    fun complete(
        c: ConstraintSystemCompletionContext,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ConeResolutionAtom>,
        candidateReturnType: ConeKotlinType,
        context: ResolutionContext,
        analyzer: PostponedAtomAnalyzer,
    ) {
        c.runCompletion(completionMode, topLevelAtoms, candidateReturnType, context, analyzer)
    }

    private fun ConstraintSystemCompletionContext.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ConeResolutionAtom>,
        topLevelType: ConeKotlinType,
        context: ResolutionContext,
        analyzer: PostponedAtomAnalyzer,
    ) {
        val topLevelTypeVariables = topLevelType.extractTypeVariables()
        context.session.constraintsLogger?.logStage("Call Completion", this)

        completion@ while (true) {
            if (completionMode.shouldForkPointConstraintsBeResolved) {
                resolveForkPointsConstraints()
            }

            // TODO: This is very slow, KT-59680
            val postponedArguments = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)

            if (completionMode.isUntilFirstLambda() && hasLambdaToAnalyze(postponedArguments)) return

            // Stage 1: analyze postponed arguments with fixed parameter types
            if (analyzeArgumentWithFixedParameterTypes(postponedArguments) {
                    analyzer.analyze(it, withPCLASession = false)
                }
            ) continue

            val variableForFixation = findFirstVariableForFixation(
                topLevelAtoms,
                postponedArguments,
                completionMode,
                topLevelType
            )
            val isThereAnyReadyForFixationVariable = variableForFixation != null

            // If there aren't any postponed arguments and ready for fixation variables, then completion isn't needed: nothing to do
            if (postponedArguments.isEmpty() && !isThereAnyReadyForFixationVariable)
                break

            /**
             * Two inheritors of sealed [ConePostponedResolvedAtom] are "postponed atoms with revisable expected type"
             * They are initially created in a situation when we have a type variable expected type for
             * a lambda [ConeLambdaWithTypeVariableAsExpectedTypeAtom] or a callable reference [ConeResolvedCallableReferenceAtom]
             * The idea of introducing them: later we could revise this expected type to a more specific (stage 2)
             * and replace an atom with revisable expected type with an atom without it (stage 4).
             * In fact, [ConeLambdaWithTypeVariableAsExpectedTypeAtom] is replaced with [ConeResolvedLambdaAtom],
             * but [ConeResolvedCallableReferenceAtom] isn't replaced and the logic of its type revision looks currently unclear.
             * Later (see KT-74021) we could make callable references behave as lambdas from this point of view
             */
            val postponedArgumentsWithRevisableType = postponedArguments.filterIsInstance<PostponedAtomWithRevisableExpectedType>()
            val dependencyProvider =
                TypeVariableDependencyInformationProvider(
                    notFixedTypeVariables, postponedArguments, topLevelType, this,
                    languageVersionSettings,
                )

            // Stage 2: collect parameter types for postponed arguments
            val wasBuiltNewExpectedTypeForSomeArgument = postponedArgumentsInputTypesResolver.collectParameterTypesAndBuildNewExpectedTypes(
                this,
                postponedArgumentsWithRevisableType,
                completionMode,
                dependencyProvider,
                topLevelTypeVariables
            )

            if (wasBuiltNewExpectedTypeForSomeArgument)
                continue

            val postponedAtomsDependingOnFunctionType = postponedArguments.filter { it is ConeFunctionTypeRelatedPostponedResolvedAtom }

            if (completionMode.allLambdasShouldBeAnalyzed) {
                // Stage 3: fix variables for parameter types of all postponed arguments
                for (argument in postponedAtomsDependingOnFunctionType) {
                    val variableWasFixed = postponedArgumentsInputTypesResolver.fixNextReadyVariableForParameterTypeIfNeeded(
                        this,
                        argument,
                        postponedArguments,
                        topLevelType,
                        dependencyProvider,
                    ) {
                        // NB: FE 1.0 calls findResolvedAtomBy here
                        // atom provided here is used only inside constraint positions, omitting right now
                        null
                    }

                    if (variableWasFixed)
                        continue@completion
                }

                // Stage 4: create atoms with revised expected types if needed
                for (argument in postponedArgumentsWithRevisableType) {
                    val argumentWasTransformed = transformToAtomWithNewFunctionExpectedType(
                        this, context, argument
                    )

                    if (argumentWasTransformed)
                        continue@completion
                }
            }

            // Stage 5: analyze the next ready postponed argument
            if (analyzeNextReadyPostponedArgument(postponedArguments, completionMode) {
                    analyzer.analyze(it, withPCLASession = false)
                }
            ) continue

            // Stage 6: fix the next ready type variable with proper constraints
            if (variableForFixation != null && fixNextReadyVariable(variableForFixation))
                continue

            // Stage 7: try to complete call with the builder inference if there are uninferred type variables
            val areThereAppearedProperConstraintsForSomeVariable = tryToCompleteWithPCLA(
                completionMode, postponedArguments, analyzer,
            )

            if (areThereAppearedProperConstraintsForSomeVariable)
                continue

            if (completionMode.fixNotInferredTypeVariablesToErrorType) {
                // Currently, it's for FULL and UNTIL_FIRST_LAMBDA, but probably should be left only to FULL
                // Stage 8: report "not enough information" for uninferred type variables
                reportNotEnoughTypeInformation(
                    completionMode, topLevelAtoms, topLevelType, postponedArguments
                )
            }

            // Stage 9: force analysis of remaining not analyzed postponed arguments and rerun stages if there are
            // It's either FULL or PCLA_POSTPONED_CALL modes (see `Forcing lambda analysis` at docs/fir/pcla.md)
            if (completionMode.allLambdasShouldBeAnalyzed) {
                if (analyzeRemainingNotAnalyzedPostponedArgument(postponedAtomsDependingOnFunctionType) {
                        analyzer.analyze(it, withPCLASession = false)
                    }
                ) continue
            }

            // Force analysis of remaining not analyzed not-lambda-like postponed arguments
            // FULL mode only
            if (completionMode.allPostponedAtomsShouldBeAnalyzed) {
                if (analyzeRemainingNotAnalyzedPostponedArgument(postponedArguments) {
                        analyzer.analyze(it, withPCLASession = false)
                    }
                ) continue
            }

            break
        }
        if (variableFixationFinder.provideFixationLogs && completionMode == ConstraintSystemCompletionMode.FULL) {
            with(variableFixationFinder) { logFixedTo() }
        }
    }

    private fun ConstraintSystemCompletionContext.findFirstVariableForFixation(
        topLevelAtoms: List<ConeResolutionAtom>,
        postponedArguments: List<ConePostponedResolvedAtom>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: ConeKotlinType,
    ): VariableFixationFinder.VariableForFixation? {
        val allTypeVariables = getOrderedAllTypeVariables(topLevelAtoms)
        return variableFixationFinder.findFirstVariableForFixation(
            this,
            allTypeVariables,
            postponedArguments,
            completionMode,
            topLevelType
        )
    }

    /**
     * General documentation for PCLA is located at `/docs/fir/pcla.md`
     *
     * This function checks if any of the postponed arguments are suitable for PCLA, and performs it for all eligible lambda arguments
     * @return true if we got new proper constraints after PCLA
     */
    private fun ConstraintSystemCompletionContext.tryToCompleteWithPCLA(
        completionMode: ConstraintSystemCompletionMode,
        postponedArguments: List<ConePostponedResolvedAtom>,
        analyzer: PostponedAtomAnalyzer,
    ): Boolean {
        if (!completionMode.allLambdasShouldBeAnalyzed) return false

        val lambdaArguments = postponedArguments.filterIsInstance<ConeResolvedLambdaAtom>().takeIf { it.isNotEmpty() } ?: return false

        var anyAnalyzed = false
        for (argument in lambdaArguments) {
            val notFixedInputTypeVariables = argument.inputTypes.flatMap { it.extractTypeVariables() }.filter { it !in fixedTypeVariables }

            if (notFixedInputTypeVariables.isEmpty()) continue
            analyzer.analyze(argument, withPCLASession = true)

            anyAnalyzed = true
        }

        return anyAnalyzed
    }

    private fun transformToAtomWithNewFunctionExpectedType(
        c: ConstraintSystemCompletionContext,
        resolutionContext: ResolutionContext,
        argument: PostponedAtomWithRevisableExpectedType,
    ): Boolean = with(c) {
        val revisedExpectedType = argument.revisedExpectedType
            ?.takeIf { it.isFunctionOrKFunctionWithAnySuspendability() } as ConeKotlinType?
            ?: return false

        when (argument) {
            is ConeResolvedCallableReferenceAtom -> {
                // When resolution isn't needed, reviseExpectedType changes nothing in fact
                if (!argument.needsResolution) return false
                // It looks like this line actually does not influence any tests.
                // There is a suggestion it replaces the revised type just by itself. See KT-74021
                argument.reviseExpectedType(revisedExpectedType)
            }
            is ConeLambdaWithTypeVariableAsExpectedTypeAtom ->
                argument.transformToResolvedLambda(c.getBuilder(), resolutionContext, revisedExpectedType)
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }

        return true
    }

    private fun ConstraintSystemCompletionContext.fixNextReadyVariable(
        variableForFixation: VariableFixationFinder.VariableForFixation,
    ): Boolean {
        val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
        if (!variableForFixation.isReady) return false

        fixVariable(this, variableWithConstraints)

        return true
    }

    private fun ConstraintSystemCompletionContext.reportNotEnoughTypeInformation(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ConeResolutionAtom>,
        topLevelType: ConeKotlinType,
        postponedArguments: List<ConePostponedResolvedAtom>,
    ) {
        while (true) {
            val variableForFixation =
                findFirstVariableForFixation(topLevelAtoms, postponedArguments, completionMode, topLevelType)
                    ?: break
            assert(!variableForFixation.isReady) {
                "At this stage there should be no remaining variables with proper constraints"
            }

            val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
            processVariableWhenNotEnoughInformation(variableWithConstraints, topLevelAtoms)
        }
    }

    private fun ConstraintSystemCompletionContext.processVariableWhenNotEnoughInformation(
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ConeResolutionAtom>,
    ) {
        val typeVariable = variableWithConstraints.typeVariable
        val resolvedAtom = findStatementOfFirstAtomWithVariable(typeVariable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()?.expression

        if (resolvedAtom != null) {
            addError(
                NotEnoughInformationForTypeParameter(typeVariable, resolvedAtom, couldBeResolvedWithUnrestrictedBuilderInference())
            )
        }

        val resultErrorType = when (typeVariable) {
            is ConeTypeParameterBasedTypeVariable ->
                createCannotInferErrorType(
                    typeVariable.typeParameterSymbol,
                    "Cannot infer argument for type parameter ${typeVariable.typeParameterSymbol.name}",
                    isUninferredParameter = true,
                )

            is ConeTypeVariableForLambdaParameterType -> createCannotInferErrorType(
                typeParameterSymbol = null,
                message = "Cannot infer lambda parameter type"
            )
            else -> createCannotInferErrorType(typeParameterSymbol = null, "Cannot infer type variable $typeVariable")
        }

        fixVariable(typeVariable, resultErrorType, ConeFixVariableConstraintPosition(typeVariable))
    }

    private fun ConstraintSystemCompletionContext.getOrderedAllTypeVariables(
        topLevelAtoms: List<ConeResolutionAtom>
    ): List<TypeConstructorMarker> {
        val result = LinkedHashSet<TypeConstructorMarker>(notFixedTypeVariables.size)
        fun ConeTypeVariable?.toTypeConstructor(): TypeConstructorMarker? =
            this?.typeConstructor?.takeIf { it in notFixedTypeVariables.keys }

        fun PostponedAtomWithRevisableExpectedType.collectNotFixedVariables() {
            revisedExpectedType?.lowerBoundIfFlexible()?.asArgumentList()?.let { typeArgumentList ->
                for (typeArgument in typeArgumentList) {
                    val constructor = typeArgument.getType()?.typeConstructor() ?: continue
                    if (constructor in notFixedTypeVariables) {
                        result.add(constructor)
                    }
                }
            }
        }

        fun ConeResolutionAtom.collectAllTypeVariables() {
            processCandidatesAndPostponedAtoms(
                candidateProcessor = { candidate ->
                    candidate.freshVariables.mapNotNullTo(result) { typeVariable ->
                        typeVariable.toTypeConstructor()
                    }
                }
            ) { postponedAtom ->
                when (postponedAtom) {
                    is ConeResolvedLambdaAtom -> {
                        result.addIfNotNull(postponedAtom.typeVariableForLambdaReturnType.toTypeConstructor())
                    }
                    is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> {
                        postponedAtom.collectNotFixedVariables()
                    }
                    is ConeResolvedCallableReferenceAtom -> {
                        if (postponedAtom.needsResolution) {
                            postponedAtom.collectNotFixedVariables()
                        }
                    }
                    is ConeSimpleNameForContextSensitiveResolution -> {
                        // No type variables for yet unresolved reference
                        // And after resolution, the candidate type variables are integrated into
                    }
                }
            }
        }

        for (topLevelAtom in topLevelAtoms) {
            topLevelAtom.collectAllTypeVariables()
        }

        return result.toList()
    }

    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
    ) {
        val resultType = inferenceComponents.resultTypeResolver.findResultType(
            c,
            variableWithConstraints,
            TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
        )

        val variable = variableWithConstraints.typeVariable
        c.fixVariable(variable, resultType, ConeFixVariableConstraintPosition(variable))
    }

    companion object {
        internal fun getOrderedNotAnalyzedPostponedArguments(candidate: Candidate): List<ConePostponedResolvedAtom> {
            val callSite = candidate.callInfo.callSite as FirExpression
            return getOrderedNotAnalyzedPostponedArguments(listOf(ConeAtomWithCandidate(callSite, candidate)))
        }

        private fun getOrderedNotAnalyzedPostponedArguments(topLevelAtoms: List<ConeResolutionAtom>): List<ConePostponedResolvedAtom> {
            val notAnalyzedArguments = arrayListOf<ConePostponedResolvedAtom>()
            for (topLevelAtom in topLevelAtoms) {
                val isPostponedAtomFoundForAssertion = runIf(AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                    mutableMapOf<ConePostponedResolvedAtom, Boolean>()
                }

                topLevelAtom.processCandidatesAndPostponedAtoms(
                    candidateProcessor = { candidate ->
                        if (isPostponedAtomFoundForAssertion != null) {
                            for (atom in candidate.postponedAtoms) {
                                isPostponedAtomFoundForAssertion.computeIfAbsent(atom) { false }
                            }
                        }
                    },
                    postponedAtomsProcessor = { atom ->
                        notAnalyzedArguments.addIfNotNull(atom.takeUnless { it.analyzed })
                        isPostponedAtomFoundForAssertion?.put(atom, true)
                    }
                )

                check(isPostponedAtomFoundForAssertion == null || isPostponedAtomFoundForAssertion.values.all { it }) {
                    "Some postponed atoms were not collected."
                }
            }

            return notAnalyzedArguments
        }

        private fun findStatementOfFirstAtomWithVariable(
            typeVariable: TypeVariableMarker,
            topLevelAtoms: List<ConeResolutionAtom>,
        ): FirStatement? {

            fun ConeResolutionAtom.findFirstStatementContainingVariable(): FirStatement? {

                var result: FirStatement? = null

                fun suggestElement(element: FirElement) {
                    if (result == null && element is FirStatement) {
                        result = element
                    }
                }

                this@findFirstStatementContainingVariable.processCandidatesAndPostponedAtoms(
                    candidateProcessor = { candidate ->
                        if (typeVariable in candidate.freshVariables) {
                            suggestElement(candidate.callInfo.callSite)
                        }
                    },
                    postponedAtomsProcessor = { postponedAtom ->
                        if (postponedAtom is ConeResolvedLambdaAtom) {
                            if (postponedAtom.typeVariableForLambdaReturnType == typeVariable) {
                                suggestElement(postponedAtom.anonymousFunction)
                            }
                        }
                    }
                )

                return result
            }

            return topLevelAtoms.firstNotNullOfOrNull(ConeResolutionAtom::findFirstStatementContainingVariable)
        }

        private fun createCannotInferErrorType(
            typeParameterSymbol: FirTypeParameterSymbol?,
            message: String,
            isUninferredParameter: Boolean = false,
        ): ConeErrorType {
            val diagnostic = when (typeParameterSymbol) {
                null -> ConeSimpleDiagnostic(message, DiagnosticKind.CannotInferParameterType)
                else -> ConeCannotInferTypeParameterType(
                    typeParameterSymbol,
                    message,
                )
            }
            return ConeErrorType(diagnostic, isUninferredParameter)
        }


    }
}

val Candidate.csBuilder: NewConstraintSystemImpl get() = system.getBuilder()
