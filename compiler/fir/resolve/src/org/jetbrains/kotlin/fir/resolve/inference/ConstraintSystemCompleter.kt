/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.filterIsInstanceWithChecker

class ConstraintSystemCompleter(components: BodyResolveComponents) {
    private val inferenceComponents = components.session.inferenceComponents
    val variableFixationFinder = inferenceComponents.variableFixationFinder
    private val postponedArgumentsInputTypesResolver = inferenceComponents.postponedArgumentInputTypesResolver
    private val languageVersionSettings = components.session.languageVersionSettings

    fun interface PostponedAtomAnalyzer {
        fun analyze(postponedResolvedAtom: PostponedResolvedAtom, withPCLASession: Boolean)
    }

    fun complete(
        c: ConstraintSystemCompletionContext,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<FirStatement>,
        candidateReturnType: ConeKotlinType,
        context: ResolutionContext,
        collectVariablesFromContext: Boolean = false,
        analyzer: PostponedAtomAnalyzer,
    ) = c.runCompletion(completionMode, topLevelAtoms, candidateReturnType, context, collectVariablesFromContext, analyzer)

    private fun ConstraintSystemCompletionContext.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<FirStatement>,
        topLevelType: ConeKotlinType,
        context: ResolutionContext,
        collectVariablesFromContext: Boolean = false,
        analyzer: PostponedAtomAnalyzer,
    ) {
        val topLevelTypeVariables = topLevelType.extractTypeVariables()

        if (completionMode == ConstraintSystemCompletionMode.FULL || completionMode == ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA) {
            resolveForkPointsConstraints()
        }

        completion@ while (true) {
            // TODO: This is very slow, KT-59680
            val postponedArguments = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)

            if (completionMode == ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA && hasLambdaToAnalyze(
                    languageVersionSettings,
                    postponedArguments
                )
            ) return

            // Stage 1: analyze postponed arguments with fixed parameter types
            if (analyzeArgumentWithFixedParameterTypes(languageVersionSettings, postponedArguments) {
                    analyzer.analyze(it, withPCLASession = false)
                }
            ) continue

            val isThereAnyReadyForFixationVariable = findFirstVariableForFixation(
                collectVariablesFromContext,
                topLevelAtoms,
                postponedArguments,
                completionMode,
                topLevelType
            ) != null

            // If there aren't any postponed arguments and ready for fixation variables, then completion isn't needed: nothing to do
            if (postponedArguments.isEmpty() && !isThereAnyReadyForFixationVariable)
                break

            val postponedArgumentsWithRevisableType = postponedArguments
                .filterIsInstanceWithChecker<PostponedAtomWithRevisableExpectedType> {
                    // NB: FE 1.0 does not perform this check
                    it.revisedExpectedType == null
                }
            val dependencyProvider =
                TypeVariableDependencyInformationProvider(notFixedTypeVariables, postponedArguments, topLevelType, this)

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

            if (completionMode.allLambdasShouldBeAnalyzed) {
                // Stage 3: fix variables for parameter types of all postponed arguments
                for (argument in postponedArguments) {
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
            if (analyzeNextReadyPostponedArgument(languageVersionSettings, postponedArguments, completionMode) {
                    analyzer.analyze(it, withPCLASession = false)
                }
            ) continue

            // Stage 6: fix next ready type variable with proper constraints
            if (fixNextReadyVariable(completionMode, topLevelAtoms, topLevelType, collectVariablesFromContext, postponedArguments))
                continue

            // Stage 7: try to complete call with the builder inference if there are uninferred type variables
            val areThereAppearedProperConstraintsForSomeVariable = tryToCompleteWithPCLA(
                completionMode, postponedArguments, analyzer,
            )

            if (areThereAppearedProperConstraintsForSomeVariable)
                continue

            if (completionMode == ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL) {
                // Complete all lambdas, maybe with fixing type variables used as top-level input types.
                // It's necessary because we need to process all data-flow info before going to the next statement.
                if (analyzeRemainingNotAnalyzedPostponedArgument(postponedArguments) {
                        analyzer.analyze(it, withPCLASession = false)
                    }
                ) continue
                reportNotEnoughInformationForTypeVariablesRequiredForInputTypesOfLambdas(
                    postponedArguments, topLevelType, dependencyProvider, topLevelAtoms
                )
            } else if (completionMode != ConstraintSystemCompletionMode.PARTIAL) {
                // Stage 8: report "not enough information" for uninferred type variables
                reportNotEnoughTypeInformation(
                    completionMode, topLevelAtoms, topLevelType, postponedArguments
                )
            }

            // Stage 9: force analysis of remaining not analyzed postponed arguments and rerun stages if there are
            if (completionMode.allLambdasShouldBeAnalyzed) {
                if (analyzeRemainingNotAnalyzedPostponedArgument(postponedArguments) {
                        analyzer.analyze(it, withPCLASession = false)
                    }
                ) continue
            }

            break
        }
    }

    private fun ConstraintSystemCompletionContext.reportNotEnoughInformationForTypeVariablesRequiredForInputTypesOfLambdas(
        postponedArguments: List<PostponedResolvedAtom>,
        topLevelType: ConeKotlinType,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        topLevelAtoms: List<FirStatement>,
    ) {
        for (argument in postponedArguments) {
            val variableForFixation = postponedArgumentsInputTypesResolver.findNextVariableForReportingNotInferredInputType(
                this,
                argument,
                postponedArguments,
                topLevelType,
                dependencyProvider,
            ) ?: continue

            assert(!variableForFixation.isReady) {
                "At this stage there should be no remaining variables with proper constraints from input types"
            }

            val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
            processVariableWhenNotEnoughInformation(variableWithConstraints, topLevelAtoms)
        }
    }

    private fun ConstraintSystemCompletionContext.findFirstVariableForFixation(
        collectVariablesFromContext: Boolean,
        topLevelAtoms: List<FirStatement>,
        postponedArguments: List<PostponedResolvedAtom>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: ConeKotlinType,
    ): VariableFixationFinder.VariableForFixation? {
        return variableFixationFinder.findFirstVariableForFixation(
            this,
            getOrderedAllTypeVariables(
                collectVariablesFromContext,
                topLevelAtoms
            ),
            postponedArguments,
            completionMode,
            topLevelType
        )
    }

    /**
     * General documentation for builder inference algorithm is located at `/docs/fir/builder_inference.md`
     *
     * This function checks if any of the postponed arguments are suitable for builder inference, and performs it for all eligible lambda arguments
     * @return true if we got new proper constraints after builder inference
     */
    private fun ConstraintSystemCompletionContext.tryToCompleteWithPCLA(
        completionMode: ConstraintSystemCompletionMode,
        postponedArguments: List<PostponedResolvedAtom>,
        analyzer: PostponedAtomAnalyzer,
    ): Boolean {
        if (!completionMode.allLambdasShouldBeAnalyzed) return false

        // If we use the builder inference anyway (if the annotation is presented), then we are already analysed builder inference lambdas
        if (!languageVersionSettings.supportsFeature(LanguageFeature.UseBuilderInferenceOnlyIfNeeded)) return false

        val lambdaArguments = postponedArguments.filterIsInstance<ResolvedLambdaAtom>().takeIf { it.isNotEmpty() } ?: return false

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
            is ResolvedCallableReferenceAtom ->
                argument.reviseExpectedType(revisedExpectedType)
            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                argument.transformToResolvedLambda(c.getBuilder(), resolutionContext, revisedExpectedType)
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }

        return true
    }

    private fun ConstraintSystemCompletionContext.fixNextReadyVariable(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<FirStatement>,
        topLevelType: ConeKotlinType,
        collectVariablesFromContext: Boolean,
        postponedArguments: List<PostponedResolvedAtom>,
    ): Boolean {
        val variableForFixation = findFirstVariableForFixation(
            collectVariablesFromContext, topLevelAtoms, postponedArguments, completionMode, topLevelType
        ) ?: return false

        val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
        if (!variableForFixation.isReady) return false

        fixVariable(this, variableWithConstraints)

        return true
    }

    private fun ConstraintSystemCompletionContext.reportNotEnoughTypeInformation(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<FirStatement>,
        topLevelType: ConeKotlinType,
        postponedArguments: List<PostponedResolvedAtom>,
    ) {
        while (true) {
            val variableForFixation =
                findFirstVariableForFixation(false, topLevelAtoms, postponedArguments, completionMode, topLevelType)
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
        topLevelAtoms: List<FirStatement>,
    ) {
        val typeVariable = variableWithConstraints.typeVariable
        val resolvedAtom = findResolvedAtomBy(typeVariable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()

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
        collectVariablesFromContext: Boolean,
        topLevelAtoms: List<FirStatement>,
    ): List<TypeConstructorMarker> {
        if (collectVariablesFromContext) {
            return notFixedTypeVariables.keys.toList()
        }
        val result = LinkedHashSet<TypeConstructorMarker>(notFixedTypeVariables.size)
        fun ConeTypeVariable?.toTypeConstructor(): TypeConstructorMarker? =
            this?.typeConstructor?.takeIf { it in notFixedTypeVariables.keys }

        fun PostponedAtomWithRevisableExpectedType.collectNotFixedVariables() {
            revisedExpectedType?.lowerBoundIfFlexible()?.asArgumentList()?.let { typeArgumentList ->
                for (typeArgument in typeArgumentList) {
                    val constructor = typeArgument.getType().typeConstructor()
                    if (constructor in notFixedTypeVariables) {
                        result.add(constructor)
                    }
                }
            }
        }

        fun FirStatement.collectAllTypeVariables() {
            this.processAllContainingCallCandidates(processBlocks = true) { candidate ->
                candidate.freshVariables.mapNotNullTo(result) { typeVariable ->
                    typeVariable.toTypeConstructor()
                }

                for (postponedAtom in candidate.postponedAtoms) {
                    when (postponedAtom) {
                        is ResolvedLambdaAtom -> {
                            result.addIfNotNull(postponedAtom.typeVariableForLambdaReturnType.toTypeConstructor())
                        }
                        is LambdaWithTypeVariableAsExpectedTypeAtom -> {
                            postponedAtom.collectNotFixedVariables()
                        }
                        is ResolvedCallableReferenceAtom -> {
                            if (postponedAtom.mightNeedAdditionalResolution) {
                                postponedAtom.collectNotFixedVariables()
                            }
                        }
                        // ResolvedCallAtom?
                        // ResolvedCallableReferenceArgumentAtom?
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
        internal fun getOrderedNotAnalyzedPostponedArguments(candidate: Candidate): List<PostponedResolvedAtom> {
            val callSite = candidate.callInfo.callSite as? FirStatement ?: return emptyList()
            return getOrderedNotAnalyzedPostponedArguments(listOf(callSite))
        }

        private fun getOrderedNotAnalyzedPostponedArguments(topLevelAtoms: List<FirStatement>): List<PostponedResolvedAtom> {
            val notAnalyzedArguments = arrayListOf<PostponedResolvedAtom>()
            for (primitive in topLevelAtoms) {
                primitive.processAllContainingCallCandidates(
                    // TODO: remove this argument and relevant parameter, KT-59679
                    // Currently, it's used because otherwise problem happens with a lambda in a try-block (see tryWithLambdaInside test)
                    processBlocks = true
                ) { candidate ->
                    candidate.postponedAtoms.forEach { atom ->
                        notAnalyzedArguments.addIfNotNull(atom.takeUnless { it.analyzed })
                    }
                }
            }

            return notAnalyzedArguments
        }

        private fun findResolvedAtomBy(
            typeVariable: TypeVariableMarker,
            topLevelAtoms: List<FirStatement>,
        ): FirStatement? {

            fun FirStatement.findFirstAtomContainingVariable(): FirStatement? {

                var result: FirStatement? = null

                fun suggestElement(element: FirElement) {
                    if (result == null && element is FirStatement) {
                        result = element
                    }
                }

                this@findFirstAtomContainingVariable.processAllContainingCallCandidates(processBlocks = true) { candidate ->
                    if (typeVariable in candidate.freshVariables) {
                        suggestElement(candidate.callInfo.callSite)
                    }

                    for (postponedAtom in candidate.postponedAtoms) {
                        if (postponedAtom is ResolvedLambdaAtom) {
                            if (postponedAtom.typeVariableForLambdaReturnType == typeVariable) {
                                suggestElement(postponedAtom.atom)
                            }
                        }
                    }
                }

                return result
            }

            return topLevelAtoms.firstNotNullOfOrNull(FirStatement::findFirstAtomContainingVariable)
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

fun FirStatement.processAllContainingCallCandidates(processBlocks: Boolean, processor: (Candidate) -> Unit) {
    when (this) {
        is FirFunctionCall -> {
            processCandidateIfApplicable(processor, processBlocks)
            this.arguments.forEach { it.processAllContainingCallCandidates(processBlocks, processor) }
        }

        is FirSafeCallExpression -> {
            this.selector.processAllContainingCallCandidates(processBlocks, processor)
        }

        is FirWhenExpression -> {
            processCandidateIfApplicable(processor, processBlocks)
            this.branches.forEach { it.result.processAllContainingCallCandidates(processBlocks, processor) }
        }

        is FirTryExpression -> {
            processCandidateIfApplicable(processor, processBlocks)
            tryBlock.processAllContainingCallCandidates(processBlocks, processor)
            catches.forEach { it.block.processAllContainingCallCandidates(processBlocks, processor) }
        }

        is FirCheckNotNullCall -> {
            processCandidateIfApplicable(processor, processBlocks)
            this.arguments.forEach { it.processAllContainingCallCandidates(processBlocks, processor) }
        }

        is FirQualifiedAccessExpression -> {
            processCandidateIfApplicable(processor, processBlocks)
        }

        is FirVariableAssignment -> {
            (lValue as? FirResolvable)?.processCandidateIfApplicable(processor, processBlocks)
            rValue.processAllContainingCallCandidates(processBlocks, processor)
        }

        is FirWrappedArgumentExpression -> this.expression.processAllContainingCallCandidates(processBlocks, processor)
        is FirBlock -> {
            if (processBlocks) {
                this.returnExpressions().forEach { it.processAllContainingCallCandidates(processBlocks, processor) }
            }
        }

        is FirDelegatedConstructorCall -> {
            processCandidateIfApplicable(processor, processBlocks)
            this.arguments.forEach { it.processAllContainingCallCandidates(processBlocks, processor) }
        }

        is FirElvisExpression -> {
            processCandidateIfApplicable(processor, processBlocks)
            lhs.processAllContainingCallCandidates(processBlocks, processor)
            rhs.processAllContainingCallCandidates(processBlocks, processor)
        }

        is FirAnnotationCall -> {
            processCandidateIfApplicable(processor, processBlocks)
            arguments.forEach { it.processAllContainingCallCandidates(processBlocks, processor) }
        }
    }
}

private fun FirResolvable.processCandidateIfApplicable(
    processor: (Candidate) -> Unit,
    processBlocks: Boolean,
) {
    val candidate = (calleeReference as? FirNamedReferenceWithCandidate)?.candidate ?: return
    processor(candidate)

    val visited = mutableSetOf<FirStatement>()

    for (atom in candidate.postponedAtoms) {
        if (atom !is ResolvedLambdaAtom || !atom.analyzed) continue

        atom.returnStatements.forEach {
            visited += it
            it.processAllContainingCallCandidates(processBlocks, processor)
        }
    }

    for (call in candidate.postponedPCLACalls) {
        if (!visited.add(call)) continue
        call.processAllContainingCallCandidates(processBlocks, processor)
    }
}

val Candidate.csBuilder: NewConstraintSystemImpl get() = system.getBuilder()
