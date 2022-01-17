/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDependencyInformationProvider
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ConstraintSystemCompleter(components: BodyResolveComponents, private val context: BodyResolveContext) {
    private val inferenceComponents = components.session.inferenceComponents
    val variableFixationFinder = inferenceComponents.variableFixationFinder
    private val postponedArgumentsInputTypesResolver = inferenceComponents.postponedArgumentInputTypesResolver
    private val languageVersionSettings = components.session.languageVersionSettings

    fun complete(
        c: ConstraintSystemCompletionContext,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<FirStatement>,
        candidateReturnType: ConeKotlinType,
        context: ResolutionContext,
        collectVariablesFromContext: Boolean = false,
        analyze: (PostponedResolvedAtom) -> Unit
    ) = c.runCompletion(completionMode, topLevelAtoms, candidateReturnType, context, collectVariablesFromContext, analyze)

    private fun ConstraintSystemCompletionContext.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<FirStatement>,
        topLevelType: ConeKotlinType,
        context: ResolutionContext,
        collectVariablesFromContext: Boolean = false,
        analyze: (PostponedResolvedAtom) -> Unit
    ) {
        val topLevelTypeVariables = topLevelType.extractTypeVariables()

        // NB: it's called in ConstraintSystemForks resolution stage by FE 1.0
        processForkConstraints()

        completion@ while (true) {
            // TODO: This is very slow
            val postponedArguments = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)

            if (completionMode == ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA && hasLambdaToAnalyze(
                    languageVersionSettings,
                    postponedArguments
                )
            ) return

            // Stage 1: analyze postponed arguments with fixed parameter types
            if (analyzeArgumentWithFixedParameterTypes(languageVersionSettings, postponedArguments, analyze))
                continue

            val isThereAnyReadyForFixationVariable = variableFixationFinder.findFirstVariableForFixation(
                this,
                getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
                postponedArguments,
                completionMode,
                topLevelType
            ) != null

            // If there aren't any postponed arguments and ready for fixation variables, then completion isn't needed: nothing to do
            if (postponedArguments.isEmpty() && !isThereAnyReadyForFixationVariable)
                break

            val postponedArgumentsWithRevisableType = postponedArguments
                .filterIsInstance<PostponedAtomWithRevisableExpectedType>()
                // NB: FE 1.0 does not perform this check
                .filter { it.revisedExpectedType == null }
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

            if (completionMode == ConstraintSystemCompletionMode.FULL) {
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
                    val argumentWasTransformed = transformToAtomWithNewFunctionalExpectedType(
                        this, context, argument
                    )

                    if (argumentWasTransformed)
                        continue@completion
                }
            }

            // Stage 5: analyze the next ready postponed argument
            if (analyzeNextReadyPostponedArgument(languageVersionSettings, postponedArguments, completionMode, analyze))
                continue

            // Stage 6: fix next ready type variable with proper constraints
            if (fixNextReadyVariable(completionMode, topLevelAtoms, topLevelType, collectVariablesFromContext, postponedArguments))
                continue

            // Stage 7: try to complete call with the builder inference if there are uninferred type variables
            val allTypeVariables = getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms)
            val areThereAppearedProperConstraintsForSomeVariable = tryToCompleteWithBuilderInference(
                completionMode, topLevelType, postponedArguments, allTypeVariables, analyze
            )

            if (areThereAppearedProperConstraintsForSomeVariable)
                continue

            // Stage 8: report "not enough information" for uninferred type variables
            reportNotEnoughTypeInformation(
                completionMode, topLevelAtoms, topLevelType, allTypeVariables, postponedArguments
            )

            // Stage 9: force analysis of remaining not analyzed postponed arguments and rerun stages if there are
            if (completionMode == ConstraintSystemCompletionMode.FULL) {
                if (analyzeRemainingNotAnalyzedPostponedArgument(postponedArguments, analyze))
                    continue
            }

            break
        }
    }

    private fun ConstraintSystemCompletionContext.tryToCompleteWithBuilderInference(
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: ConeKotlinType,
        postponedArguments: List<PostponedResolvedAtom>,
        allTypeVariables: List<TypeConstructorMarker>,
        analyze: (PostponedResolvedAtom) -> Unit
    ): Boolean {
        if (completionMode == ConstraintSystemCompletionMode.PARTIAL) return false

        // If we use the builder inference anyway (if the annotation is presented), then we are already analysed builder inference lambdas
        if (!languageVersionSettings.supportsFeature(LanguageFeature.UseBuilderInferenceOnlyIfNeeded)) return false

        val lambdaArguments = postponedArguments.filterIsInstance<ResolvedLambdaAtom>().takeIf { it.isNotEmpty() } ?: return false

        // We assume useBuilderInferenceWithoutAnnotation = true for FIR

        for (argument in lambdaArguments) {
            val notFixedInputTypeVariables = argument.inputTypes
                .map { it.extractTypeVariables() }.flatten().filter { it !in fixedTypeVariables }

            if (notFixedInputTypeVariables.isEmpty()) continue

            for (variable in notFixedInputTypeVariables) {
                getBuilder().markPostponedVariable(notFixedTypeVariables.getValue(variable).typeVariable)
            }

            analyze(argument)
        }

        val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
            this, allTypeVariables, postponedArguments, completionMode, topLevelType
        )

        // continue completion (rerun stages) only if ready for fixation variables with proper constraints have appeared
        // (after analysing a lambda with the builder inference)
        // otherwise we don't continue and report "not enough type information" error
        return variableForFixation?.hasProperConstraint == true
    }

    private fun transformToAtomWithNewFunctionalExpectedType(
        c: ConstraintSystemCompletionContext,
        resolutionContext: ResolutionContext,
        argument: PostponedAtomWithRevisableExpectedType,
    ): Boolean = with(c) {
        val revisedExpectedType: ConeKotlinType =
            argument.revisedExpectedType?.takeIf { it.isFunctionOrKFunctionWithAnySuspendability() }?.cast() ?: return false

        when (argument) {
            is ResolvedCallableReferenceAtom ->
                argument.reviseExpectedType(revisedExpectedType)
            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                argument.transformToResolvedLambda(c.getBuilder(), resolutionContext, revisedExpectedType, null /*TODO()*/)
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
        val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
            this,
            getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
            postponedArguments,
            completionMode,
            topLevelType
        ) ?: return false

        val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
        if (!variableForFixation.hasProperConstraint) {
            if (context.inferenceSession.isSyntheticTypeVariable(variableWithConstraints.typeVariable)) {
                context.inferenceSession.fixSyntheticTypeVariableWithNotEnoughInformation(
                    variableWithConstraints.typeVariable as ConeTypeVariable,
                    this
                )
                return true
            }

            return false
        }

        fixVariable(this, topLevelType, variableWithConstraints, postponedArguments)

        return true
    }

    private fun ConstraintSystemCompletionContext.reportNotEnoughTypeInformation(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<FirStatement>,
        topLevelType: ConeKotlinType,
        allTypeVariables: List<TypeConstructorMarker>,
        postponedArguments: List<PostponedResolvedAtom>,
    ) {
        while (true) {
            val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
                this, allTypeVariables, postponedArguments, completionMode, topLevelType,
            ) ?: break

            assert(!variableForFixation.hasProperConstraint) {
                "At this stage there should be no remaining variables with proper constraints"
            }

            if (completionMode == ConstraintSystemCompletionMode.PARTIAL) break

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
                    "Cannot infer argument for type parameter ${typeVariable.typeParameterSymbol.name}",
                    isUninferredParameter = true,
                )
            is ConeTypeVariableForLambdaParameterType -> createCannotInferErrorType("Cannot infer lambda parameter type")
            else -> createCannotInferErrorType("Cannot infer type variable $typeVariable")
        }

        fixVariable(typeVariable, resultErrorType, ConeFixVariableConstraintPosition(typeVariable))
    }

    private fun ConstraintSystemCompletionContext.getOrderedAllTypeVariables(
        collectVariablesFromContext: Boolean,
        topLevelAtoms: List<FirStatement>
    ): List<TypeConstructorMarker> {
        if (collectVariablesFromContext) {
            return notFixedTypeVariables.keys.toList()
        }
        val result = LinkedHashSet<TypeConstructorMarker>(notFixedTypeVariables.size)
        fun ConeTypeVariable?.toTypeConstructor(): TypeConstructorMarker? =
            this?.typeConstructor?.takeIf { it in notFixedTypeVariables.keys }

        // TODO: non-top-level variables?
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

        if (context.inferenceSession.hasSyntheticTypeVariables()) {
            result.addAll(notFixedTypeVariables.filter { context.inferenceSession.isSyntheticTypeVariable(it.value.typeVariable) }.keys.asIterable())
        }

        require(result.size == notFixedTypeVariables.size) {
            val notFoundTypeVariables = notFixedTypeVariables.keys.toMutableSet().apply { removeAll(result) }
            "Not all type variables found: $notFoundTypeVariables"
        }

        return result.toList()
    }

    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        topLevelType: KotlinTypeMarker,
        variableWithConstraints: VariableWithConstraints,
        postponedResolveKtPrimitives: List<PostponedResolvedAtom>
    ) {
        val direction = TypeVariableDirectionCalculator(c, postponedResolveKtPrimitives, topLevelType).getDirection(variableWithConstraints)
        val resultType = inferenceComponents.resultTypeResolver.findResultType(c, variableWithConstraints, direction)
        val variable = variableWithConstraints.typeVariable
        c.fixVariable(variable, resultType, ConeFixVariableConstraintPosition(variable)) // TODO: obtain atom for diagnostics
    }

    companion object {
        private fun getOrderedNotAnalyzedPostponedArguments(topLevelAtoms: List<FirStatement>): List<PostponedResolvedAtom> {
            val notAnalyzedArguments = arrayListOf<PostponedResolvedAtom>()
            for (primitive in topLevelAtoms) {
                primitive.processAllContainingCallCandidates(
                    // TODO: remove this argument and relevant parameter
                    // Currently, it's used because otherwise problem happens with a lambda in a try-block (see tryWithLambdaInside test)
                    processBlocks = true
                ) { candidate ->
                    candidate.postponedAtoms.forEach { atom ->
                        notAnalyzedArguments.addIfNotNull(atom.safeAs<PostponedResolvedAtom>()?.takeUnless { it.analyzed })
                    }
                }
            }

            return notAnalyzedArguments
        }

        private fun findResolvedAtomBy(
            typeVariable: TypeVariableMarker,
            topLevelAtoms: List<FirStatement>
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

        private fun createCannotInferErrorType(message: String, isUninferredParameter: Boolean = false) =
            ConeClassErrorType(
                ConeSimpleDiagnostic(
                    message,
                    DiagnosticKind.CannotInferParameterType,
                ),
                isUninferredParameter,
            )


    }
}

fun FirStatement.processAllContainingCallCandidates(processBlocks: Boolean, processor: (Candidate) -> Unit) {
    when (this) {
        is FirFunctionCall -> {
            processCandidateIfApplicable(processor, processBlocks)
            this.arguments.forEach { it.processAllContainingCallCandidates(processBlocks, processor) }
        }

        is FirSafeCallExpression -> {
            this.regularQualifiedAccess.processAllContainingCallCandidates(processBlocks, processor)
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
            processCandidateIfApplicable(processor, processBlocks)
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
    processBlocks: Boolean
) {
    val candidate = (calleeReference as? FirNamedReferenceWithCandidate)?.candidate ?: return
    processor(candidate)

    for (atom in candidate.postponedAtoms) {
        if (atom !is ResolvedLambdaAtom || !atom.analyzed) continue

        atom.returnStatements.forEach {
            it.processAllContainingCallCandidates(processBlocks, processor)
        }
    }
}

val Candidate.csBuilder: NewConstraintSystemImpl get() = system.getBuilder()
