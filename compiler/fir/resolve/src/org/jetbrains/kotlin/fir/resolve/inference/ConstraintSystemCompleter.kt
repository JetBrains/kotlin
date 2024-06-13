/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.unwrapArgument
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
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.filterIsInstanceWithChecker
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class ConstraintSystemCompleter(components: BodyResolveComponents) {
    private val inferenceComponents = components.session.inferenceComponents
    private val variableFixationFinder = inferenceComponents.variableFixationFinder
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
        analyzer: PostponedAtomAnalyzer,
    ) {
        c.runCompletion(completionMode, topLevelAtoms, candidateReturnType, context, analyzer)
    }

    private fun ConstraintSystemCompletionContext.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<FirStatement>,
        topLevelType: ConeKotlinType,
        context: ResolutionContext,
        analyzer: PostponedAtomAnalyzer,
    ) {
        val topLevelTypeVariables = topLevelType.extractTypeVariables()

        completion@ while (true) {
            if (completionMode.shouldForkPointConstraintsBeResolved) {
                resolveForkPointsConstraints()
            }

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
            if (fixNextReadyVariable(completionMode, topLevelAtoms, topLevelType, postponedArguments))
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
        topLevelAtoms: List<FirStatement>,
        postponedArguments: List<PostponedResolvedAtom>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: ConeKotlinType,
    ): VariableFixationFinder.VariableForFixation? {
        return variableFixationFinder.findFirstVariableForFixation(
            this,
            getOrderedAllTypeVariables(
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
        postponedArguments: List<PostponedResolvedAtom>,
    ): Boolean {
        val variableForFixation = findFirstVariableForFixation(
            topLevelAtoms, postponedArguments, completionMode, topLevelType
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
        topLevelAtoms: List<FirStatement>,
    ): List<TypeConstructorMarker> {
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
            this.processCandidatesAndPostponedAtomsInOrder(
                candidateProcessor = { candidate ->
                    candidate.freshVariables.mapNotNullTo(result) { typeVariable ->
                        typeVariable.toTypeConstructor()
                    }
                }
            ) { postponedAtom ->
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
                val postponedAtomsForAssertion = runIf(AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                    mutableSetOf<PostponedResolvedAtom>()
                }

                primitive.processCandidatesAndPostponedAtomsInOrder(
                    candidateProcessor = { candidate ->
                        postponedAtomsForAssertion?.addAll(candidate.postponedAtoms)
                    },
                    postponedAtomsProcessor = { atom ->
                        notAnalyzedArguments.addIfNotNull(atom.takeUnless { it.analyzed })
                        postponedAtomsForAssertion?.remove(atom)
                    }
                )

                check(postponedAtomsForAssertion.isNullOrEmpty()) { "Some postponed atoms were not collected." }
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

                this@findFirstAtomContainingVariable.processCandidatesAndPostponedAtomsInOrder(
                    candidateProcessor = { candidate ->
                        if (typeVariable in candidate.freshVariables) {
                            suggestElement(candidate.callInfo.callSite)
                        }
                    },
                    postponedAtomsProcessor = { postponedAtom ->
                        if (postponedAtom is ResolvedLambdaAtom) {
                            if (postponedAtom.typeVariableForLambdaReturnType == typeVariable) {
                                suggestElement(postponedAtom.atom)
                            }
                        }
                    }
                )

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

fun FirStatement.processPostponedAtomsInOrder(processor: (PostponedResolvedAtom) -> Unit) {
    processCandidatesAndPostponedAtomsInOrderImpl(topLevelCandidate = null, postponedAtomsProcessor = processor)
}

fun FirStatement.processCandidatesAndPostponedAtomsInOrder(
    candidateProcessor: (Candidate) -> Unit,
    postponedAtomsProcessor: (PostponedResolvedAtom) -> Unit,
) {
    processCandidatesAndPostponedAtomsInOrderImpl(topLevelCandidate = null, candidateProcessor, postponedAtomsProcessor)
}

/**
 * Processes candidates and postponed atoms in their syntactical order.
 *
 * It is required that recursive calls pass the outermost candidate as [topLevelCandidate] because some postponed atoms are added
 * during completion to it instead of some nested candidate.
 *
 * TODO(KT-68998) this function and the helper functions below should be simplified a lot after the refactoring of postponed atoms.
 */
private fun FirStatement.processCandidatesAndPostponedAtomsInOrderImpl(
    topLevelCandidate: Candidate?,
    candidateProcessor: ((Candidate) -> Unit)? = null,
    postponedAtomsProcessor: ((PostponedResolvedAtom) -> Unit)? = null,
) {
    when (this) {
        is FirResolvable -> {
            val candidate = (calleeReference as? FirNamedReferenceWithCandidate)?.candidate ?: return
            candidateProcessor?.invoke(candidate)
            val visited = mutableSetOf<FirStatement>()

            fun process(arg: FirExpression) {
                arg.processCandidatesAndPostponedAtomsInOrderImpl(
                    topLevelCandidate ?: candidate,
                    candidateProcessor,
                    postponedAtomsProcessor
                )

                for (atom in arg.getPostponedAtoms(candidate, topLevelCandidate)) {
                    postponedAtomsProcessor?.invoke(atom)
                    if (atom is ResolvedLambdaAtom && atom.analyzed) {
                        for (it in atom.returnStatements) {
                            visited += it
                            process(it)
                        }
                    }
                }
            }

            // Iterate postponed atoms in the order of their appearance, depth first.
            for (arg in candidate.callInfo.arguments) {
                process(arg)
            }

            for (call in candidate.postponedPCLACalls) {
                if (!visited.add(call)) continue
                call.processCandidatesAndPostponedAtomsInOrderImpl(topLevelCandidate, candidateProcessor, postponedAtomsProcessor)
            }
        }

        is FirSafeCallExpression -> {
            this.selector.processCandidatesAndPostponedAtomsInOrderImpl(topLevelCandidate, candidateProcessor, postponedAtomsProcessor)
        }

        is FirVariableAssignment -> {
            lValue.processCandidatesAndPostponedAtomsInOrderImpl(topLevelCandidate, candidateProcessor, postponedAtomsProcessor)
            rValue.processCandidatesAndPostponedAtomsInOrderImpl(topLevelCandidate, candidateProcessor, postponedAtomsProcessor)
        }

        is FirWrappedArgumentExpression -> {
            this.expression.processCandidatesAndPostponedAtomsInOrderImpl(topLevelCandidate, candidateProcessor, postponedAtomsProcessor)
        }

        is FirErrorExpression -> {
            this.expression?.processCandidatesAndPostponedAtomsInOrderImpl(topLevelCandidate, candidateProcessor, postponedAtomsProcessor)
        }

        is FirBlock -> {
            this.returnExpressions().forEach {
                it.processCandidatesAndPostponedAtomsInOrderImpl(
                    topLevelCandidate,
                    candidateProcessor,
                    postponedAtomsProcessor
                )
            }
        }
    }
}

/**
 * Returns all postponed atoms associated with the receiver [FirExpression].
 *
 * In the case of lambda against type variable, the result might contain both the [LambdaWithTypeVariableAsExpectedTypeAtom] and
 * [ResolvedLambdaAtom] created during completion.
 */
private fun FirExpression.getPostponedAtoms(candidate: Candidate, topLevelCandidate: Candidate?): List<PostponedResolvedAtom> {
    val postponedAtomStatement = when (val unwrapped = unwrapArgument()) {
        is FirBlock -> unwrapped.statements.lastOrNull()
        is FirErrorExpression -> unwrapped.expression
        else -> unwrapped
    } ?: return emptyList()

    return buildList {
        addPostponedAtoms(postponedAtomStatement, candidate)
        if (topLevelCandidate != null) {
            addPostponedAtoms(postponedAtomStatement, topLevelCandidate)
        }
    }
}

private fun MutableList<PostponedResolvedAtom>.addPostponedAtoms(element: FirElement, candidate: Candidate) {
    candidate.postponedAtomsByFir[element]?.let(this::add)

    // ResolvedLambdaAtom uses the function as key, other implementations use the expression.
    if (element is FirAnonymousFunctionExpression) {
        candidate.postponedAtomsByFir[element.anonymousFunction]?.let(this::add)
    }
}

val Candidate.csBuilder: NewConstraintSystemImpl get() = system.getBuilder()
