/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.diagnostics.Errors.BadNamedArgumentsTarget.*
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.BuilderInferenceExpectedTypeConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SingleSmartCast
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.calls.util.extractCallableReferenceExpression
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.reportTrailingLambdaErrorOr
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstantChecker
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.StubTypeForBuilderInference
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.intersectWrappedTypes
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContextDelegate
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.model.freshTypeConstructor
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isNullableNothing
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class DiagnosticReporterByTrackingStrategy(
    val constantExpressionEvaluator: ConstantExpressionEvaluator,
    val context: BasicCallResolutionContext,
    val psiKotlinCall: PSIKotlinCall,
    val dataFlowValueFactory: DataFlowValueFactory,
    val allDiagnostics: List<KotlinCallDiagnostic>,
    private val smartCastManager: SmartCastManager,
    private val typeSystemContext: TypeSystemInferenceExtensionContextDelegate
) : DiagnosticReporter {
    private val trace = context.trace as TrackingBindingTrace
    private val tracingStrategy: TracingStrategy get() = psiKotlinCall.tracingStrategy
    private val call: Call get() = psiKotlinCall.psiCall

    override fun onExplicitReceiver(diagnostic: KotlinCallDiagnostic) {

    }

    override fun onCall(diagnostic: KotlinCallDiagnostic) {
        when (diagnostic) {
            is VisibilityError -> tracingStrategy.invisibleMember(trace, diagnostic.invisibleMember)
            is NoValueForParameter -> tracingStrategy.noValueForParameter(trace, diagnostic.parameterDescriptor)
            is TypeCheckerHasRanIntoRecursion -> {
                // Note: we have two similar diagnostics here
                // - TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM (error starting from 1.7)
                // - TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT (error starting from 1.9)
                // however they have different deprecation cycle, and thus it's better to distinguish them.
                // This 'insideAugmentedAssignment' is just a heuristics (approximate) to do it.
                // It cannot turn red code to green or green to red; the worst thing we can get here
                // is replacing red code with yellow, if e.g. LV is set to 1.8 explicitly,
                // and we have chosen the second diagnostics instead of the first one.
                val insideAugmentedAssignment = call.callElement.parents.any {
                    it is KtBinaryExpression && it.operationToken in KtTokens.AUGMENTED_ASSIGNMENTS
                }
                tracingStrategy.recursiveType(trace, context.languageVersionSettings, insideAugmentedAssignment)
            }
            is InstantiationOfAbstractClass -> tracingStrategy.instantiationOfAbstractClass(trace)
            is AbstractSuperCall -> {
                val superExpression = diagnostic.receiver.psiExpression as? KtSuperExpression
                if (context.languageVersionSettings.supportsFeature(LanguageFeature.ForbidSuperDelegationToAbstractAnyMethod) ||
                    superExpression == null ||
                    trace[BindingContext.SUPER_EXPRESSION_FROM_ANY_MIGRATION, superExpression] != true
                ) {
                    tracingStrategy.abstractSuperCall(trace)
                } else {
                    tracingStrategy.abstractSuperCallWarning(trace)
                }
            }
            is AbstractFakeOverrideSuperCall -> {
                if (context.languageVersionSettings.supportsFeature(LanguageFeature.ForbidSuperDelegationToAbstractFakeOverride)) {
                    tracingStrategy.abstractSuperCall(trace)
                } else {
                    tracingStrategy.abstractSuperCallWarning(trace)
                }
            }
            is NonApplicableCallForBuilderInferenceDiagnostic -> {
                val reportOn = diagnostic.kotlinCall
                trace.reportDiagnosticOnce(NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE.on(reportOn.psiKotlinCall.psiCall.callElement))
            }
            is CandidateChosenUsingOverloadResolutionByLambdaAnnotation -> {
                trace.report(CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION.on(psiKotlinCall.psiCall.callElement))
            }
            is EnumEntryAmbiguityWarning -> {
                val propertyDescriptor = diagnostic.property
                val enumEntryDescriptor = diagnostic.enumEntry
                val enumCompanionDescriptor = (enumEntryDescriptor.containingDeclaration as? ClassDescriptor)?.companionObjectDescriptor
                if (enumCompanionDescriptor == null || propertyDescriptor.containingDeclaration != enumCompanionDescriptor) {
                    trace.report(
                        DEPRECATED_RESOLVE_WITH_AMBIGUOUS_ENUM_ENTRY.on(
                            psiKotlinCall.psiCall.callElement, propertyDescriptor, enumEntryDescriptor
                        )
                    )
                }
            }
            is CompatibilityWarning -> {
                val callElement = psiKotlinCall.psiCall.callElement
                trace.report(
                    COMPATIBILITY_WARNING.on(callElement.getCalleeExpressionIfAny() ?: callElement, diagnostic.candidate)
                )
            }
            is NoContextReceiver -> {
                val callElement = psiKotlinCall.psiCall.callElement
                trace.report(
                    NO_CONTEXT_RECEIVER.on(
                        callElement.getCalleeExpressionIfAny() ?: callElement,
                        diagnostic.receiverDescriptor.value.toString()
                    )
                )
            }
            is MultipleArgumentsApplicableForContextReceiver -> {
                val callElement = psiKotlinCall.psiCall.callElement
                trace.report(
                    MULTIPLE_ARGUMENTS_APPLICABLE_FOR_CONTEXT_RECEIVER.on(callElement, diagnostic.receiverDescriptor.value.toString())
                )
            }
            is ContextReceiverAmbiguity -> {
                val callElement = psiKotlinCall.psiCall.callElement
                trace.report(AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER.on(callElement))
            }
            is UnsupportedContextualDeclarationCall -> {
                val callElement = psiKotlinCall.psiCall.callElement
                trace.report(UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL.on(callElement))
            }

            is AdaptedCallableReferenceIsUsedWithReflection, is NotCallableMemberReference, is CallableReferencesDefaultArgumentUsed -> {
                // AdaptedCallableReferenceIsUsedWithReflection -> reported in onCallArgument
                // NotCallableMemberReference -> UNSUPPORTED reported in DoubleColonExpressionResolver
                // CallableReferencesDefaultArgumentUsed -> possible in 1.3 and earlier versions only
                return
            }

            else -> {
                unknownError(diagnostic, "onCall")
            }
        }
    }

    override fun onTypeArguments(diagnostic: KotlinCallDiagnostic) {
        val psiCallElement = psiKotlinCall.psiCall.callElement
        val reportElement =
            if (psiCallElement is KtCallExpression)
                psiCallElement.typeArgumentList ?: psiCallElement.calleeExpression ?: psiCallElement
            else
                psiCallElement

        when (diagnostic) {
            is WrongCountOfTypeArguments -> {
                val expectedTypeArgumentsCount = diagnostic.descriptor.typeParameters.size
                trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(reportElement, expectedTypeArgumentsCount, diagnostic.descriptor))
            }
            else -> {
                unknownError(diagnostic, "onTypeArguments")
            }
        }
    }

    private fun unknownError(diagnostic: KotlinCallDiagnostic, onTarget: String) {
        if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
            throw AssertionError("$onTarget should not be called with ${diagnostic::class.java}")
        } else if (reportAdditionalErrors) {
            trace.report(
                NEW_INFERENCE_UNKNOWN_ERROR.on(
                    psiKotlinCall.psiCall.callElement,
                    diagnostic.candidateApplicability,
                    onTarget
                )
            )
        }
    }

    override fun onCallName(diagnostic: KotlinCallDiagnostic) {

    }

    override fun onTypeArgument(typeArgument: TypeArgument, diagnostic: KotlinCallDiagnostic) {

    }

    override fun onCallReceiver(callReceiver: SimpleKotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        when (diagnostic) {
            is UnsafeCallError -> {
                val isForImplicitInvoke = when (callReceiver) {
                    is ReceiverExpressionKotlinCallArgument -> callReceiver.isForImplicitInvoke
                    else -> diagnostic.isForImplicitInvoke
                            || callReceiver.receiver.receiverValue.type.isExtensionFunctionType
                }

                tracingStrategy.unsafeCall(trace, callReceiver.receiver.receiverValue.type, isForImplicitInvoke)
            }

            is SuperAsExtensionReceiver -> {
                val psiExpression = callReceiver.psiExpression
                if (psiExpression is KtSuperExpression) {
                    trace.report(SUPER_CANT_BE_EXTENSION_RECEIVER.on(psiExpression, psiExpression.text))
                }
            }

            is StubBuilderInferenceReceiver -> {
                val stubType = callReceiver.receiver.receiverValue.type as? StubTypeForBuilderInference
                val originalTypeParameter = stubType?.originalTypeVariable?.originalTypeParameter

                trace.report(
                    BUILDER_INFERENCE_STUB_RECEIVER.on(
                        callReceiver.psiExpression ?: call.callElement,
                        originalTypeParameter?.name ?: SpecialNames.NO_NAME_PROVIDED,
                        originalTypeParameter?.containingDeclaration?.name ?: SpecialNames.NO_NAME_PROVIDED
                    )
                )
            }
            else -> {
                unknownError(diagnostic, "onCallReceiver")
            }
        }
    }

    override fun onCallArgument(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        when (diagnostic) {
            is SmartCastDiagnostic -> reportSmartCast(diagnostic)
            is UnstableSmartCast -> reportUnstableSmartCast(diagnostic)
            is VisibilityErrorOnArgument -> {
                val invisibleMember = diagnostic.invisibleMember
                val argumentExpression =
                    diagnostic.argument.psiCallArgument.valueArgument.getArgumentExpression()?.lastBlockStatementOrThis()

                if (argumentExpression != null) {
                    trace.report(INVISIBLE_MEMBER.on(argumentExpression, invisibleMember, invisibleMember.visibility, invisibleMember))
                }
            }
            is TooManyArguments -> {
                trace.reportTrailingLambdaErrorOr(callArgument.psiExpression) { expr ->
                    TOO_MANY_ARGUMENTS.on(expr, diagnostic.descriptor)
                }

                trace.markAsReported()
            }
            is VarargArgumentOutsideParentheses -> trace.reportTrailingLambdaErrorOr(callArgument.psiExpression) { expr ->
                VARARG_OUTSIDE_PARENTHESES.on(expr)
            }

            is MixingNamedAndPositionArguments -> {
                trace.report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(callArgument.psiCallArgument.valueArgument.asElement()))
            }

            is NoneCallableReferenceCallCandidates -> {
                val argument = diagnostic.argument
                val expression = (argument as? CallableReferenceKotlinCallArgumentImpl)?.ktCallableReferenceExpression
                if (expression != null) {
                    trace.report(UNRESOLVED_REFERENCE.on(expression.callableReference, expression.callableReference))
                }
            }

            is CallableReferenceCallCandidatesAmbiguity -> {
                val expression = when (val psiExpression = diagnostic.argument.psiExpression) {
                    is KtPsiUtil.KtExpressionWrapper -> psiExpression.baseExpression
                    else -> psiExpression
                } as? KtCallableReferenceExpression

                val candidates = diagnostic.candidates.map { it.candidate }
                if (expression != null) {
                    trace.reportDiagnosticOnce(CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY.on(expression.callableReference, candidates))
                    trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, expression.callableReference, candidates)
                }
            }

            is ArgumentNullabilityErrorDiagnostic -> reportNullabilityMismatchDiagnostic(callArgument, diagnostic)

            is ArgumentNullabilityWarningDiagnostic -> reportNullabilityMismatchDiagnostic(callArgument, diagnostic)

            is CallableReferencesDefaultArgumentUsed -> {
                val callableReferenceExpression = diagnostic.argument.call.extractCallableReferenceExpression()

                require(callableReferenceExpression != null) {
                    "A call element must be callable reference for `CallableReferencesDefaultArgumentUsed`"
                }

                trace.report(
                    UNSUPPORTED_FEATURE.on(
                        callableReferenceExpression,
                        LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType to context.languageVersionSettings
                    )
                )
            }

            is ResolvedToSamWithVarargDiagnostic -> {
                trace.report(TYPE_INFERENCE_CANDIDATE_WITH_SAM_AND_VARARG.on(callArgument.psiCallArgument.valueArgument.asElement()))
            }

            is NotEnoughInformationForLambdaParameter -> {
                val lambdaArgument = diagnostic.lambdaArgument
                val parameterIndex = diagnostic.parameterIndex

                val valueArgument = lambdaArgument.psiCallArgument.valueArgument

                val valueParameters = when (val argumentExpression = KtPsiUtil.deparenthesize(valueArgument.getArgumentExpression())) {
                    is KtLambdaExpression -> argumentExpression.valueParameters
                    is KtNamedFunction -> argumentExpression.valueParameters // for anonymous functions
                    else -> return
                }

                val parameter = valueParameters.getOrNull(parameterIndex)
                if (parameter != null) {
                    trace.report(CANNOT_INFER_PARAMETER_TYPE.on(parameter))
                }
            }

            is CompatibilityWarningOnArgument -> {
                trace.report(
                    COMPATIBILITY_WARNING.on(callArgument.psiCallArgument.valueArgument.asElement(), diagnostic.candidate)
                )
            }

            is AdaptedCallableReferenceIsUsedWithReflection -> {
                trace.report(
                    ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE.on(
                        callArgument.psiCallArgument.valueArgument.asElement()
                    )
                )
            }

            is MultiLambdaBuilderInferenceRestriction -> {
                val typeParameter = diagnostic.typeParameter as? TypeParameterDescriptor

                trace.reportDiagnosticOnce(
                    BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION.on(
                        callArgument.psiCallArgument.valueArgument.asElement(),
                        typeParameter?.name ?: SpecialNames.NO_NAME_PROVIDED,
                        typeParameter?.containingDeclaration?.name ?: SpecialNames.NO_NAME_PROVIDED,
                    )
                )
            }

            is NotCallableMemberReference, is NotCallableExpectedType -> {
                // NotCallableMemberReference -> UNSUPPORTED is reported in DoubleColonExpressionResolver
                // NotCallableExpectedType -> TYPE_MISMATCH is reported in reportConstraintErrorByPosition
                return
            }

            else -> {
                unknownError(diagnostic, "onCallArgument")
            }
        }
    }

    override fun onCallArgumentName(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        val nameReference = callArgument.psiCallArgument.valueArgument.getArgumentName()?.referenceExpression ?: return
        when (diagnostic) {
            is NamedArgumentReference -> {
                trace.record(BindingContext.REFERENCE_TARGET, nameReference, diagnostic.parameterDescriptor)
                trace.markAsReported()
            }
            is NameForAmbiguousParameter -> trace.report(NAME_FOR_AMBIGUOUS_PARAMETER.on(nameReference))
            is NameNotFound -> trace.report(NAMED_PARAMETER_NOT_FOUND.on(nameReference, nameReference))

            is NamedArgumentNotAllowed -> trace.report(
                NAMED_ARGUMENTS_NOT_ALLOWED.on(
                    nameReference,
                    when (diagnostic.descriptor) {
                        is FunctionInvokeDescriptor -> INVOKE_ON_FUNCTION_TYPE
                        is DeserializedCallableMemberDescriptor -> INTEROP_FUNCTION
                        else -> NON_KOTLIN_FUNCTION
                    }
                )
            )
            is ArgumentPassedTwice -> trace.report(ARGUMENT_PASSED_TWICE.on(nameReference))
            else -> {
                unknownError(diagnostic, "onCallArgumentName")
            }
        }
    }

    override fun onCallArgumentSpread(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        when (diagnostic) {
            is NonVarargSpread -> {
                val castedPsiCallArgument = callArgument as? PSIKotlinCallArgument
                val castedCallArgument = callArgument as? ExpressionKotlinCallArgumentImpl

                if (castedCallArgument != null) {
                    val spreadElement = castedCallArgument.valueArgument.getSpreadElement()
                    if (spreadElement != null) {
                        trace.report(NON_VARARG_SPREAD.onError(spreadElement))
                    }
                } else if (castedPsiCallArgument != null) {
                    val spreadElement = castedPsiCallArgument.valueArgument.getSpreadElement()
                    if (spreadElement != null) {
                        trace.report(NON_VARARG_SPREAD.on(context.languageVersionSettings, spreadElement))
                    }
                }
            }
            else -> {
                unknownError(diagnostic, "onCallArgumentSpread")
            }
        }
    }

    private fun reportSmartCast(smartCastDiagnostic: SmartCastDiagnostic) {
        val expressionArgument = smartCastDiagnostic.argument
        val smartCastResult = when (expressionArgument) {
            is ExpressionKotlinCallArgumentImpl -> {
                trace.markAsReported()
                val context = context.replaceDataFlowInfo(expressionArgument.dataFlowInfoBeforeThisArgument)
                val argumentExpression = KtPsiUtil.getLastElementDeparenthesized(
                    expressionArgument.valueArgument.getArgumentExpression(),
                    context.statementFilter
                )
                val dataFlowValue = dataFlowValueFactory.createDataFlowValue(expressionArgument.receiver.receiverValue, context)
                val call = if (call.callElement is KtBinaryExpression) null else call
                if (!expressionArgument.valueArgument.isExternal()) {
                    smartCastManager.checkAndRecordPossibleCast(
                        dataFlowValue, smartCastDiagnostic.smartCastType, argumentExpression, context, call,
                        recordExpressionType = false
                    )
                } else null
            }
            is ReceiverExpressionKotlinCallArgument -> {
                trace.markAsReported()
                val receiverValue = expressionArgument.receiver.receiverValue
                val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiverValue, context)
                smartCastManager.checkAndRecordPossibleCast(
                    dataFlowValue, smartCastDiagnostic.smartCastType, (receiverValue as? ExpressionReceiver)?.expression, context, call,
                    recordExpressionType = true
                )
            }
            else -> null
        }
        val resolvedCall =
            smartCastDiagnostic.kotlinCall?.psiKotlinCall?.psiCall?.getResolvedCall(trace.bindingContext) as? NewResolvedCallImpl<*>
        if (resolvedCall != null && smartCastResult != null) {
            if (resolvedCall.extensionReceiver == expressionArgument.receiver.receiverValue) {
                resolvedCall.updateExtensionReceiverWithSmartCastIfNeeded(smartCastResult.resultType)
            }
            if (resolvedCall.dispatchReceiver == expressionArgument.receiver.receiverValue) {
                resolvedCall.setSmartCastDispatchReceiverType(smartCastResult.resultType)
            }
        }
    }

    private fun reportUnstableSmartCast(unstableSmartCast: UnstableSmartCast) {
        val dataFlowValue = dataFlowValueFactory.createDataFlowValue(unstableSmartCast.argument.receiver.receiverValue, context)
        val possibleTypes = unstableSmartCast.argument.receiver.typesFromSmartCasts
        val argumentExpression = unstableSmartCast.argument.psiExpression ?: return

        require(possibleTypes.isNotEmpty()) { "Receiver for unstable smart cast without possible types" }
        val intersectWrappedTypes = intersectWrappedTypes(possibleTypes)
        trace.record(BindingContext.UNSTABLE_SMARTCAST, argumentExpression, SingleSmartCast(null, intersectWrappedTypes))
        trace.report(
            SMARTCAST_IMPOSSIBLE.on(
                argumentExpression,
                intersectWrappedTypes,
                argumentExpression.text,
                dataFlowValue.kind.description
            )
        )
    }

    private fun reportCallableReferenceConstraintError(
        error: NewConstraintMismatch,
        rhsExpression: KtSimpleNameExpression
    ) {
        trace.report(TYPE_MISMATCH.on(rhsExpression, error.lowerKotlinType, error.upperKotlinType))
    }

    private fun reportConstraintErrorByPosition(error: NewConstraintMismatch, position: ConstraintPosition) {
        if (position is CallableReferenceConstraintPositionImpl) {
            val callableReferenceExpression = position.callableReferenceCall.call.extractCallableReferenceExpression()

            require(callableReferenceExpression != null) {
                "There should be the corresponding callable reference expression for `CallableReferenceConstraintPositionImpl`"
            }

            reportCallableReferenceConstraintError(error, callableReferenceExpression.callableReference)
            return
        }

        val isWarning = error is NewConstraintWarning
        val typeMismatchDiagnostic = if (isWarning) TYPE_MISMATCH_WARNING else TYPE_MISMATCH
        val report = if (isWarning) trace::reportDiagnosticOnce else trace::report

        when (position) {
            is ArgumentConstraintPosition<*> -> {
                reportArgumentConstraintErrorByPosition(
                    error, position.argument as KotlinCallArgument,
                    isWarning, typeMismatchDiagnostic,
                    selectorCall = null, report
                )
            }
            is ReceiverConstraintPosition<*> -> {
                reportArgumentConstraintErrorByPosition(
                    error, position.argument as KotlinCallArgument,
                    isWarning, typeMismatchDiagnostic,
                    selectorCall = (position as ReceiverConstraintPositionImpl).selectorCall, report
                )
            }
            is LambdaArgumentConstraintPosition<*> -> {
                reportArgumentConstraintErrorByPosition(
                    error, (position.lambda as ResolvedLambdaAtom).atom,
                    isWarning, typeMismatchDiagnostic,
                    selectorCall = null, report
                )
            }
            is BuilderInferenceExpectedTypeConstraintPosition -> {
                val inferredType =
                    if (!error.lowerKotlinType.isNullableNothing()) error.lowerKotlinType
                    else error.upperKotlinType.makeNullable()
                trace.report(TYPE_MISMATCH.on(position.topLevelCall, error.upperKotlinType, inferredType))
            }
            is ExpectedTypeConstraintPosition<*> -> {
                val call = (position.topLevelCall as? KotlinCall)?.psiKotlinCall?.psiCall?.callElement as? KtExpression
                val inferredType =
                    if (!error.lowerKotlinType.isNullableNothing()) error.lowerKotlinType
                    else error.upperKotlinType.makeNullable()
                if (call != null) {
                    report(typeMismatchDiagnostic.on(call, error.upperKotlinType, inferredType))
                }
            }
            is BuilderInferenceSubstitutionConstraintPosition<*> -> {
                reportConstraintErrorByPosition(error, position.initialConstraint.position)
            }
            is ExplicitTypeParameterConstraintPosition<*> -> {
                val typeArgumentReference = (position.typeArgument as SimpleTypeArgumentImpl).typeProjection.typeReference ?: return
                val diagnosticFactory = if (isWarning) UPPER_BOUND_VIOLATED_WARNING else UPPER_BOUND_VIOLATED
                report(diagnosticFactory.on(typeArgumentReference, error.upperKotlinType, error.lowerKotlinType))
            }
            is FixVariableConstraintPosition<*> -> {
                val morePreciseDiagnosticExists = allDiagnostics.any { other ->
                    val otherError = other.constraintSystemError ?: return@any false
                    otherError is NewConstraintError && otherError.position.from !is FixVariableConstraintPositionImpl
                }
                if (morePreciseDiagnosticExists) return

                val call = ((position.resolvedAtom as? ResolvedAtom)?.atom as? PSIKotlinCall)?.psiCall ?: call
                val expression = call.calleeExpression ?: return

                trace.reportDiagnosticOnce(typeMismatchDiagnostic.on(expression, error.upperKotlinType, error.lowerKotlinType))
            }
            BuilderInferencePosition -> {
                // some error reported later?
            }
            is DeclaredUpperBoundConstraintPosition<*> -> {
                val originalCall = (position as DeclaredUpperBoundConstraintPositionImpl).kotlinCall
                val typeParameterDescriptor = position.typeParameter
                val ownerDescriptor = typeParameterDescriptor.containingDeclaration
                if (reportAdditionalErrors) {
                    trace.reportDiagnosticOnce(
                        UPPER_BOUND_VIOLATION_IN_CONSTRAINT.on(
                            (originalCall as PSIKotlinCall).psiCall.callElement,
                            typeParameterDescriptor.name,
                            ownerDescriptor.name,
                            error.upperKotlinType,
                            error.lowerKotlinType
                        )
                    )
                }
            }
            is DelegatedPropertyConstraintPosition<*> -> {
                // DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, reported later
            }
            is KnownTypeParameterConstraintPosition<*> -> {
                // UPPER_BOUND_VIOLATED, reported later?
            }
            is CallableReferenceConstraintPosition<*>,
            is IncorporationConstraintPosition,
            is InjectedAnotherStubTypeConstraintPosition<*>,
            is LHSArgumentConstraintPosition<*, *>, SimpleConstraintSystemConstraintPosition, ProvideDelegateFixationPosition
            -> {
                if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                    throw AssertionError("Constraint error in unexpected position: $position")
                } else if (reportAdditionalErrors) {
                    report(
                        TYPE_MISMATCH_IN_CONSTRAINT.on(
                            psiKotlinCall.psiCall.callElement,
                            error.upperKotlinType,
                            error.lowerKotlinType,
                            position
                        )
                    )
                }
            }
        }
    }

    private fun reportArgumentConstraintErrorByPosition(
        error: NewConstraintMismatch,
        argument: KotlinCallArgument,
        isWarning: Boolean,
        typeMismatchDiagnostic: DiagnosticFactory2<KtExpression, KotlinType, KotlinType>,
        selectorCall: KotlinCall?,
        report: (Diagnostic) -> Unit
    ) {
        if (argument is LambdaKotlinCallArgument) {
            val parameterTypes = argument.parametersTypes?.toList()
            if (parameterTypes != null) {
                val index = parameterTypes.indexOf(error.upperKotlinType.unwrap())
                val lambdaExpression = argument.psiExpression as? KtLambdaExpression
                val parameter = lambdaExpression?.valueParameters?.getOrNull(index)
                if (parameter != null) {
                    val diagnosticFactory =
                        if (isWarning) EXPECTED_PARAMETER_TYPE_MISMATCH_WARNING else EXPECTED_PARAMETER_TYPE_MISMATCH
                    report(diagnosticFactory.on(parameter, error.lowerKotlinType))
                    return
                }
            }
        }

        val expression = argument.psiExpression ?: run {
            val psiCall = (selectorCall as? PSIKotlinCall)?.psiCall ?: psiKotlinCall.psiCall
            // Note: we don't report RECEIVER_TYPE_MISMATCH w/out ProperTypeInferenceConstraintsProcessing
            // See KT-57854. This is needed for intellij.go.tests (recursive generics case) compilation with K1
            if (context.languageVersionSettings.supportsFeature(LanguageFeature.ProperTypeInferenceConstraintsProcessing) &&
                reportAdditionalErrors
            ) {
                report(
                    RECEIVER_TYPE_MISMATCH.on(
                        psiCall.calleeExpression ?: psiCall.callElement, error.upperKotlinType, error.lowerKotlinType
                    )
                )
            }
            return
        }

        val deparenthesized = KtPsiUtil.safeDeparenthesize(expression)
        if (reportConstantTypeMismatch(error, deparenthesized)) return

        val compileTimeConstant = trace[BindingContext.COMPILE_TIME_VALUE, deparenthesized] as? TypedCompileTimeConstant
        if (compileTimeConstant != null) {
            val expressionType = trace[BindingContext.EXPRESSION_TYPE_INFO, expression]?.type
            if (expressionType != null &&
                !UnsignedTypes.isUnsignedType(compileTimeConstant.type) && UnsignedTypes.isUnsignedType(expressionType)
            ) {
                // This is a special "hack" to prevent TYPE_MISMATCH
                // in case of a compile-time constant with signed VS unsigned type
                // See conversionOfSignedToUnsigned.kt diagnostic test
                return
            }
        }
        report(typeMismatchDiagnostic.on(deparenthesized, error.upperKotlinType, error.lowerKotlinType))
    }

    /**
     * Should we report additional errors appeared in Kotlin compiler 1.9.0 or not
     *
     * This property appeared in Kotlin compiler 1.9.0, after we discovered some "swallowed" diagnostics in this class.
     * We added a set of diagnostics (see property usages) to have additional protection before migrating to K2.
     * For details see KT-55055, KT-55056, KT-55079.
     * This property is normally true, but can be disabled with a feature NoAdditionalErrorsInK1DiagnosticReporter
     */
    private val reportAdditionalErrors: Boolean
        get() = !context.languageVersionSettings.supportsFeature(LanguageFeature.NoAdditionalErrorsInK1DiagnosticReporter)

    override fun constraintError(error: ConstraintSystemError) {
        when (error) {
            is NewConstraintMismatch -> reportConstraintErrorByPosition(error, error.position.from)

            is CapturedTypeFromSubtyping -> {
                val position = error.position
                val argumentPosition: ArgumentConstraintPositionImpl? =
                    position as? ArgumentConstraintPositionImpl
                        ?: (position as? IncorporationConstraintPosition)?.from as? ArgumentConstraintPositionImpl

                argumentPosition?.let {
                    val expression = it.argument.psiExpression ?: return
                    trace.reportDiagnosticOnce(
                        NEW_INFERENCE_ERROR.on(
                            expression,
                            "Capture type from subtyping ${error.constraintType} for variable ${error.typeVariable}"
                        )
                    )
                }
            }

            is InferredIntoDeclaredUpperBounds -> {
                val psiCall = psiKotlinCall.psiCall
                val expression = if (psiCall is CallTransformer.CallForImplicitInvoke) {
                    psiCall.outerCall.calleeExpression
                } else {
                    psiCall.calleeExpression?.takeIf { it.isPhysical } ?: psiCall.callElement
                } ?: return
                val typeVariable = error.typeVariable as? TypeVariableFromCallableDescriptor ?: return

                trace.reportDiagnosticOnce(
                    INFERRED_INTO_DECLARED_UPPER_BOUNDS.on(expression, typeVariable.originalTypeParameter.name.asString())
                )
            }

            is NotEnoughInformationForTypeParameterImpl -> {
                val resolvedAtom = error.resolvedAtom
                val isDiagnosticRedundant = !isSpecialFunction(resolvedAtom) && allDiagnostics.any {
                    when (it) {
                        is WrongCountOfTypeArguments -> true
                        is KotlinConstraintSystemDiagnostic -> {
                            val otherError = it.error
                            (otherError is ConstrainingTypeIsError && otherError.typeVariable == error.typeVariable)
                                    || otherError is NewConstraintError
                        }
                        else -> false
                    }
                }

                if (isDiagnosticRedundant) return
                val expression = when (val atom = error.resolvedAtom.atom) {
                    is PSIKotlinCall -> {
                        val psiCall = atom.psiCall
                        if (psiCall is CallTransformer.CallForImplicitInvoke) {
                            psiCall.outerCall.calleeExpression
                        } else {
                            psiCall.calleeExpression
                        }
                    }
                    is PSIKotlinCallArgument -> atom.valueArgument.getArgumentExpression()
                    else -> call.calleeExpression
                } ?: return

                if (isSpecialFunction(resolvedAtom)) {
                    // We locally report errors on some arguments of special calls, on which the error may not be reported directly
                    reportNotEnoughInformationForTypeParameterForSpecialCall(resolvedAtom, error)
                } else {
                    val typeVariableName = when (val typeVariable = error.typeVariable) {
                        is TypeVariableFromCallableDescriptor -> typeVariable.originalTypeParameter.name.asString()
                        is TypeVariableForLambdaReturnType -> "return type of lambda"
                        else -> error("Unsupported type variable: $typeVariable")
                    }
                    val unwrappedExpression = if (expression is KtBlockExpression) {
                        expression.statements.lastOrNull() ?: expression
                    } else expression

                    val diagnostic = if (error.couldBeResolvedWithUnrestrictedBuilderInference) {
                        COULD_BE_INFERRED_ONLY_WITH_UNRESTRICTED_BUILDER_INFERENCE
                    } else {
                        NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
                    }

                    trace.reportDiagnosticOnce(diagnostic.on(unwrappedExpression, typeVariableName))
                }
            }

            is OnlyInputTypesDiagnostic -> {
                val typeVariable = error.typeVariable as? TypeVariableFromCallableDescriptor ?: return
                psiKotlinCall.psiCall.calleeExpression?.let {
                    trace.report(
                        TYPE_INFERENCE_ONLY_INPUT_TYPES.on(context.languageVersionSettings, it, typeVariable.originalTypeParameter)
                    )
                }
            }

            is InferredEmptyIntersectionError, is InferredEmptyIntersectionWarning -> {
                val typeVariable = (error as InferredEmptyIntersection).typeVariable
                psiKotlinCall.psiCall.calleeExpression?.let { expression ->
                    val typeVariableText = (typeVariable as? TypeVariableFromCallableDescriptor)?.originalTypeParameter?.name?.asString()
                        ?: typeVariable.toString()

                    @Suppress("UNCHECKED_CAST")
                    val incompatibleTypes = error.incompatibleTypes as List<KotlinType>

                    @Suppress("UNCHECKED_CAST")
                    val causingTypes = error.causingTypes as List<KotlinType>
                    val causingTypesText = if (incompatibleTypes == causingTypes) "" else ": ${causingTypes.joinToString()}"
                    val diagnostic = if (error.kind.isDefinitelyEmpty) {
                        INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION.on(
                            context.languageVersionSettings, expression, typeVariableText,
                            incompatibleTypes, error.kind.description, causingTypesText
                        )
                    } else {
                        INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION.on(
                            expression, typeVariableText,
                            incompatibleTypes, error.kind.description, causingTypesText
                        )
                    }

                    trace.reportDiagnosticOnce(diagnostic)
                }
            }
            // ConstrainingTypeIsError means that some type isError, so it's reported somewhere else
            is ConstrainingTypeIsError -> {}
            // LowerPriorityToPreserveCompatibility is not expected to report something
            is LowerPriorityToPreserveCompatibility -> {}
            // NoSuccessfulFork does not exist in K1
            is NoSuccessfulFork -> {}
            // NotEnoughInformationForTypeParameterImpl is already considered above
            is NotEnoughInformationForTypeParameter<*> -> {
                throw AssertionError("constraintError should not be called with ${error::class.java}")
            }
        }
    }

    private fun reportNullabilityMismatchDiagnostic(callArgument: KotlinCallArgument, diagnostic: ArgumentNullabilityMismatchDiagnostic) {
        val expression = (callArgument as? PSIKotlinCallArgument)?.valueArgument?.getArgumentExpression()?.let {
            KtPsiUtil.deparenthesize(it) ?: it
        }
        if (expression != null) {
            if (expression.isNull() && expression is KtConstantExpression) {
                val factory = when (diagnostic) {
                    is ArgumentNullabilityErrorDiagnostic -> NULL_FOR_NONNULL_TYPE
                    is ArgumentNullabilityWarningDiagnostic -> NULL_FOR_NONNULL_TYPE_WARNING
                }
                trace.reportDiagnosticOnce(factory.on(expression, diagnostic.expectedType))
            } else {
                val factory = when (diagnostic) {
                    is ArgumentNullabilityErrorDiagnostic -> TYPE_MISMATCH
                    is ArgumentNullabilityWarningDiagnostic -> TYPE_MISMATCH_WARNING
                }
                trace.report(factory.on(expression, diagnostic.expectedType, diagnostic.actualType))
            }
        }
    }

    private fun reportNotEnoughInformationForTypeParameterForSpecialCall(
        resolvedAtom: ResolvedCallAtom,
        error: NotEnoughInformationForTypeParameterImpl
    ) {
        val subResolvedAtomsToReportError =
            getSubResolvedAtomsOfSpecialCallToReportUninferredTypeParameter(resolvedAtom, error.typeVariable)

        if (subResolvedAtomsToReportError.isEmpty()) return

        for (subResolvedAtom in subResolvedAtomsToReportError) {
            val atom = subResolvedAtom.atom as? PSIKotlinCallArgument ?: continue
            val argumentsExpression = getArgumentsExpressionOrLastExpressionInBlock(atom)

            if (argumentsExpression != null) {
                val specialFunctionName = requireNotNull(
                    ControlStructureTypingUtils.ResolveConstruct.values().find { specialFunction ->
                        specialFunction.specialFunctionName == resolvedAtom.candidateDescriptor.name
                    }
                ) { "Unsupported special construct: ${resolvedAtom.candidateDescriptor.name} not found in special construct names" }

                trace.reportDiagnosticOnce(
                    NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER.on(
                        argumentsExpression, " for subcalls of ${specialFunctionName.getName()} expression"
                    )
                )
            }
        }
    }

    private fun getArgumentsExpressionOrLastExpressionInBlock(atom: PSIKotlinCallArgument): KtExpression? {
        val valueArgumentExpression = atom.valueArgument.getArgumentExpression()

        return if (valueArgumentExpression is KtBlockExpression) valueArgumentExpression.statements.lastOrNull() else valueArgumentExpression
    }

    private fun KotlinType.containsUninferredTypeParameter(uninferredTypeVariable: TypeVariableMarker) = contains {
        ErrorUtils.isUninferredTypeVariable(it) || it == TypeUtils.DONT_CARE
                || it.constructor == uninferredTypeVariable.freshTypeConstructor(typeSystemContext)
    }

    private fun getSubResolvedAtomsOfSpecialCallToReportUninferredTypeParameter(
        resolvedAtom: ResolvedAtom,
        uninferredTypeVariable: TypeVariableMarker
    ): Set<ResolvedAtom> =
        buildSet {
            for (subResolvedAtom in resolvedAtom.subResolvedAtoms ?: return@buildSet) {
                val atom = subResolvedAtom.atom
                val typeToCheck = when {
                    subResolvedAtom is PostponedResolvedAtom -> subResolvedAtom.expectedType ?: return@buildSet
                    atom is SimpleKotlinCallArgument -> atom.receiver.receiverValue.type
                    else -> return@buildSet
                }

                if (typeToCheck.containsUninferredTypeParameter(uninferredTypeVariable)) {
                    add(subResolvedAtom)
                }

                if (!subResolvedAtom.subResolvedAtoms.isNullOrEmpty()) {
                    addAll(
                        getSubResolvedAtomsOfSpecialCallToReportUninferredTypeParameter(subResolvedAtom, uninferredTypeVariable)
                    )
                }
            }
        }

    @OptIn(ExperimentalContracts::class)
    private fun isSpecialFunction(atom: ResolvedAtom): Boolean {
        contract {
            returns(true) implies (atom is ResolvedCallAtom)
        }
        if (atom !is ResolvedCallAtom) return false

        return ControlStructureTypingUtils.ResolveConstruct.values().any { specialFunction ->
            specialFunction.specialFunctionName == atom.candidateDescriptor.name
        }
    }

    private fun reportConstantTypeMismatch(constraintError: NewConstraintMismatch, expression: KtExpression): Boolean {
        if (expression is KtConstantExpression) {
            val module = context.scope.ownerDescriptor.module
            val constantValue = constantExpressionEvaluator.evaluateToConstantValue(expression, trace, context.expectedType)
            val hasConstantTypeError = CompileTimeConstantChecker(context, module, true)
                .checkConstantExpressionType(constantValue, expression, constraintError.upperKotlinType)
            if (hasConstantTypeError) return true
        }
        return false
    }

}

val NewConstraintMismatch.upperKotlinType get() = upperType as KotlinType
val NewConstraintMismatch.lowerKotlinType get() = lowerType as KotlinType
