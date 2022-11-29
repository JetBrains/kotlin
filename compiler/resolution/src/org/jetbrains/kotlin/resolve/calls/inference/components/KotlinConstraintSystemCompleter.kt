/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.components.candidate.SimpleResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.components.transformToResolvedLambda
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.model.TypeVariableTypeConstructorMarker
import org.jetbrains.kotlin.types.model.safeSubstitute
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addIfNotNull

class KotlinConstraintSystemCompleter(
    private val resultTypeResolver: ResultTypeResolver,
    val variableFixationFinder: VariableFixationFinder,
    private val postponedArgumentsInputTypesResolver: PostponedArgumentInputTypesResolver,
    private val languageVersionSettings: LanguageVersionSettings
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
                        findResolvedAtomBy(it, topLevelAtoms) ?: topLevelAtoms.firstOrNull()
                    }

                    if (variableWasFixed)
                        continue@completion
                }

                // Stage 4: create atoms with revised expected types if needed
                for (argument in postponedArgumentsWithRevisableType) {
                    val argumentWasTransformed = transformToAtomWithNewFunctionalExpectedType(
                        this, argument, diagnosticsHolder
                    )

                    if (argumentWasTransformed)
                        continue@completion
                }
            }

            // Stage 5: analyze the next ready postponed argument
            if (analyzeNextReadyPostponedArgument(languageVersionSettings, postponedArguments, completionMode, analyze))
                continue

            // Stage 6: fix next ready type variable with proper constraints
            if (
                fixNextReadyVariable(
                    completionMode, topLevelAtoms, topLevelType, collectVariablesFromContext, postponedArguments, diagnosticsHolder
                )
            ) continue

            // Stage 7: try to complete call with the builder inference if there are uninferred type variables
            val areThereAppearedProperConstraintsForSomeVariable = tryToCompleteWithBuilderInference(
                completionMode, topLevelAtoms, topLevelType, postponedArguments, collectVariablesFromContext, diagnosticsHolder, analyze
            )

            if (areThereAppearedProperConstraintsForSomeVariable)
                continue

            // Stage 8: report "not enough information" for uninferred type variables
            reportNotEnoughTypeInformation(
                completionMode, topLevelAtoms, topLevelType, collectVariablesFromContext, postponedArguments, diagnosticsHolder
            )

            // Stage 9: force analysis of remaining not analyzed postponed arguments and rerun stages if there are
            if (completionMode == ConstraintSystemCompletionMode.FULL) {
                if (analyzeRemainingNotAnalyzedPostponedArgument(postponedArguments, analyze))
                    continue
            }

            break
        }
    }

    fun prepareLambdaAtomForFactoryPattern(
        atom: ResolvedLambdaAtom,
        candidate: SimpleResolutionCandidate,
        diagnosticsHolder: KotlinDiagnosticsHolder,
    ): ResolvedLambdaAtom {
        val returnVariable = TypeVariableForLambdaReturnType(candidate.callComponents.builtIns, "_R")
        val csBuilder = candidate.getSystem().getBuilder()
        csBuilder.registerVariable(returnVariable)
        val functionalType: KotlinType = csBuilder.buildCurrentSubstitutor().safeSubstitute(candidate.getSystem().asConstraintSystemCompleterContext(), atom.expectedType!!) as KotlinType
        val expectedType = KotlinTypeFactory.simpleType(
            functionalType.attributes,
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

    private fun ConstraintSystemCompletionContext.tryToCompleteWithBuilderInference(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        postponedArguments: List<PostponedResolvedAtom>,
        collectVariablesFromContext: Boolean,
        diagnosticsHolder: KotlinDiagnosticsHolder,
        analyze: (PostponedResolvedAtom) -> Unit
    ): Boolean {
        if (completionMode != ConstraintSystemCompletionMode.FULL) return false

        val useBuilderInferenceOnlyIfNeeded = languageVersionSettings.supportsFeature(LanguageFeature.UseBuilderInferenceOnlyIfNeeded)

        // If we use the builder inference anyway (if the annotation is presented), then we are already analysed builder inference lambdas
        if (!useBuilderInferenceOnlyIfNeeded) return false

        val lambdaArguments = postponedArguments.filterIsInstance<ResolvedLambdaAtom>().takeIf { it.isNotEmpty() } ?: return false

        fun ResolvedLambdaAtom.notFixedInputTypeVariables(): List<TypeVariableTypeConstructorMarker> =
            inputTypes.flatMap { it.extractTypeVariables() }.filter { it !in fixedTypeVariables }

        val useBuilderInferenceWithoutAnnotation =
            languageVersionSettings.supportsFeature(LanguageFeature.UseBuilderInferenceWithoutAnnotation)

        val checkForDangerousBuilderInference =
            !languageVersionSettings.supportsFeature(LanguageFeature.NoBuilderInferenceWithoutAnnotationRestriction)

        // Let's call builder lambda (BL) a lambda that has non-zero not fixed input type variables in
        // type arguments of it's input types
        // ex: MutableList<T>.() -> Unit
        // During type inference of call-site such lambda will be considered BL, if
        // T not fixed yet
        // Given we have two or more builder lambdas among postponed arguments, it could result in incorrect type inference due to
        // incorrect constraint propagation into common system
        // See KT-53740
        // Constraint propagation into common system happens at
        // org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer.applyResultsOfAnalyzedLambdaToCandidateSystem
        val dangerousBuilderInferenceWithoutAnnotation =
            lambdaArguments.size >= 2 && lambdaArguments.count { it.notFixedInputTypeVariables().isNotEmpty() } >= 2

        val builder = getBuilder()
        for (argument in lambdaArguments) {
            val reallyHasBuilderInferenceAnnotation = argument.atom.hasBuilderInferenceAnnotation

            // no annotation and builder inference without annotation is disabled
            if (!reallyHasBuilderInferenceAnnotation && !useBuilderInferenceWithoutAnnotation) continue

            // Imitate having builder inference annotation. TODO: Remove after getting rid of @BuilderInference
            if (!reallyHasBuilderInferenceAnnotation) {
                argument.atom.hasBuilderInferenceAnnotation = true
            }

            val notFixedInputTypeVariables = argument.notFixedInputTypeVariables()

            // lambda is subject to builder inference past this point
            if (notFixedInputTypeVariables.isEmpty()) continue

            // we have dangerous inference situation
            // if lambda annotated with BuilderInference it's probably safe, due to type shape
            // otherwise report multi-lambda builder inference restriction diagnostic
            if (checkForDangerousBuilderInference && dangerousBuilderInferenceWithoutAnnotation && !reallyHasBuilderInferenceAnnotation) {
                for (variable in notFixedInputTypeVariables) {
                    diagnosticsHolder.addDiagnostic(MultiLambdaBuilderInferenceRestriction(argument.atom, variable.typeParameter))
                }
            }

            for (variable in notFixedInputTypeVariables) {
                builder.markPostponedVariable(notFixedTypeVariables.getValue(variable).typeVariable)
            }

            analyze(argument)
        }

        val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
            this, getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms), postponedArguments, completionMode, topLevelType
        )

        // continue completion (rerun stages) only if ready for fixation variables with proper constraints have appeared
        // (after analysing a lambda with the builder inference)
        // otherwise we don't continue and report "not enough type information" error
        return variableForFixation?.hasProperConstraint == true
    }

    private fun transformToAtomWithNewFunctionalExpectedType(
        c: ConstraintSystemCompletionContext,
        argument: PostponedAtomWithRevisableExpectedType,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): Boolean = with(c) {
        val revisedExpectedType: UnwrappedType = argument.revisedExpectedType
            ?.takeIf { it.isFunctionOrKFunctionWithAnySuspendability() } as UnwrappedType? ?: return false

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

    private fun ConstraintSystemCompletionContext.fixNextReadyVariable(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        collectVariablesFromContext: Boolean,
        postponedArguments: List<PostponedResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): Boolean {
        val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
            this,
            getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
            postponedArguments,
            completionMode,
            topLevelType
        ) ?: return false

        if (!variableForFixation.hasProperConstraint) return false

        fixVariable(this, notFixedTypeVariables.getValue(variableForFixation.variable), topLevelAtoms, diagnosticsHolder)

        return true
    }

    private fun ConstraintSystemCompletionContext.reportNotEnoughTypeInformation(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ResolvedAtom>,
        topLevelType: UnwrappedType,
        collectVariablesFromContext: Boolean,
        postponedArguments: List<PostponedResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        while (true) {
            val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
                this, getOrderedAllTypeVariables(collectVariablesFromContext, topLevelAtoms),
                postponedArguments, completionMode, topLevelType,
            ) ?: break

            assert(!variableForFixation.hasProperConstraint) {
                "At this stage there should be no remaining variables with proper constraints"
            }

            if (completionMode == ConstraintSystemCompletionMode.PARTIAL) break

            val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
            processVariableWhenNotEnoughInformation(variableWithConstraints, topLevelAtoms, diagnosticsHolder)
        }
    }

    private fun ConstraintSystemCompletionContext.processVariableWhenNotEnoughInformation(
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        val typeVariable = variableWithConstraints.typeVariable
        val resolvedAtom = findResolvedAtomBy(typeVariable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()

        if (resolvedAtom != null) {
            addError(
                NotEnoughInformationForTypeParameterImpl(typeVariable, resolvedAtom, couldBeResolvedWithUnrestrictedBuilderInference())
            )
        }

        val resultErrorType = when {
            typeVariable is TypeVariableFromCallableDescriptor -> {
                ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_TYPE_VARIABLE, typeVariable.originalTypeParameter.name.asString())
            }
            typeVariable is TypeVariableForLambdaParameterType && typeVariable.atom is LambdaKotlinCallArgument -> {
                diagnosticsHolder.addDiagnostic(
                    NotEnoughInformationForLambdaParameter(typeVariable.atom, typeVariable.index)
                )
                ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_LAMBDA_PARAMETER_TYPE)
            }
            else -> ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_TYPE_VARIABLE, typeVariable.toString())
        }

        fixVariable(typeVariable, resultErrorType, FixVariableConstraintPositionImpl(typeVariable, resolvedAtom))

    }

    private fun ConstraintSystemCompletionContext.getOrderedAllTypeVariables(
        collectVariablesFromContext: Boolean,
        topLevelAtoms: List<ResolvedAtom>
    ): List<TypeConstructorMarker> {
        if (collectVariablesFromContext)
            return notFixedTypeVariables.keys.toList()

        fun getVariablesFromRevisedExpectedType(revisedExpectedType: KotlinType?) =
            revisedExpectedType?.arguments?.map { it.type.constructor }?.filterIsInstance<TypeVariableTypeConstructor>()

        // Note that it's important to use Set here, because several atoms can share the same type variable
        val result = linkedSetOf<TypeConstructor>()

        fun ResolvedAtom.collectAllTypeVariables() {
            val typeVariables = when (this) {
                is ResolvedLambdaAtom -> {
                    listOfNotNull(typeVariableForLambdaReturnType?.freshTypeConstructor)
                }
                is LambdaWithTypeVariableAsExpectedTypeAtom -> {
                    getVariablesFromRevisedExpectedType(revisedExpectedType).orEmpty()
                }
                is PostponedCallableReferenceAtom -> {
                    getVariablesFromRevisedExpectedType(revisedExpectedType).orEmpty() +
                            candidate?.freshVariablesSubstitutor?.freshVariables?.map { it.freshTypeConstructor }.orEmpty()
                }
                is ResolvedCallAtom -> freshVariablesSubstitutor.freshVariables.map { it.freshTypeConstructor }
                is ResolvedCallableReferenceArgumentAtom -> candidate?.freshVariablesSubstitutor?.freshVariables?.map { it.freshTypeConstructor }.orEmpty()
                else -> emptyList()
            }

            typeVariables.mapNotNullTo(result) {
                it.takeIf { notFixedTypeVariables.containsKey(it) }
            }

            /*
             * Hack for completing error candidates in delegate resolve
             */
            if (this is StubResolvedAtom && typeVariable in notFixedTypeVariables) {
                result += typeVariable
            }

            if (analyzed) {
                subResolvedAtoms?.forEach { it.collectAllTypeVariables() }
            }
        }

        for (topLevelAtom in topLevelAtoms) {
            topLevelAtom.collectAllTypeVariables()
        }

        require(result.size == notFixedTypeVariables.size) {
            val notFoundTypeVariables = notFixedTypeVariables.keys.toMutableSet().apply { removeAll(result) }
            "Not all type variables found: $notFoundTypeVariables"
        }

        return result.toList()
    }

    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        fixVariable(c, variableWithConstraints, TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN, topLevelAtoms, diagnosticsHolder)
    }

    private fun reportWarningIfFixedIntoDeclaredUpperBounds(
        diagnosticsHolder: KotlinDiagnosticsHolder,
        variableWithConstraints: VariableWithConstraints
    ) {
        val areAllConstraintFromDeclaredUpperBounds = variableWithConstraints.constraints.all {
            val position = it.position.from
            position is BuilderInferenceSubstitutionConstraintPosition<*, *> && position.isFromNotSubstitutedDeclaredUpperBound
        }

        if (areAllConstraintFromDeclaredUpperBounds) {
            diagnosticsHolder.addDiagnostic(
                KotlinConstraintSystemDiagnostic(InferredIntoDeclaredUpperBounds(variableWithConstraints.typeVariable))
            )
        }
    }

    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection,
        topLevelAtoms: List<ResolvedAtom>,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        val resultType = resultTypeResolver.findResultType(c, variableWithConstraints, direction)
        val variable = variableWithConstraints.typeVariable
        val resolvedAtom = findResolvedAtomBy(variable, topLevelAtoms) ?: topLevelAtoms.firstOrNull()

        reportWarningIfFixedIntoDeclaredUpperBounds(diagnosticsHolder, variableWithConstraints)

        c.fixVariable(variable, resultType, FixVariableConstraintPositionImpl(variable, resolvedAtom))
    }

    companion object {
        fun getOrderedNotAnalyzedPostponedArguments(topLevelAtoms: List<ResolvedAtom>): List<PostponedResolvedAtom> {
            fun ResolvedAtom.process(to: MutableList<PostponedResolvedAtom>) {
                to.addIfNotNull((this as? PostponedResolvedAtom)?.takeUnless { it.analyzed })

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
                    is ResolvedCallableReferenceArgumentAtom -> candidate?.freshVariablesSubstitutor?.freshVariables?.let { typeVariable in it } ?: false
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
