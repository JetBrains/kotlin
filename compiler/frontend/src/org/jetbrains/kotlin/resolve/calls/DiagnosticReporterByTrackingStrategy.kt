/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.diagnostics.Errors.BadNamedArgumentsTarget.INVOKE_ON_FUNCTION_TYPE
import org.jetbrains.kotlin.diagnostics.Errors.BadNamedArgumentsTarget.NON_KOTLIN_FUNCTION
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.reportTrailingLambdaErrorOr
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstantChecker
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class DiagnosticReporterByTrackingStrategy(
    val constantExpressionEvaluator: ConstantExpressionEvaluator,
    val context: BasicCallResolutionContext,
    val psiKotlinCall: PSIKotlinCall,
    val dataFlowValueFactory: DataFlowValueFactory,
    val allDiagnostics: List<KotlinCallDiagnostic>,
    private val smartCastManager: SmartCastManager
) : DiagnosticReporter {
    private val trace = context.trace as TrackingBindingTrace
    private val tracingStrategy: TracingStrategy get() = psiKotlinCall.tracingStrategy
    private val call: Call get() = psiKotlinCall.psiCall

    override fun onExplicitReceiver(diagnostic: KotlinCallDiagnostic) {

    }

    override fun onCall(diagnostic: KotlinCallDiagnostic) {
        when (diagnostic.javaClass) {
            VisibilityError::class.java -> tracingStrategy.invisibleMember(trace, (diagnostic as VisibilityError).invisibleMember)
            NoValueForParameter::class.java -> tracingStrategy.noValueForParameter(
                trace,
                (diagnostic as NoValueForParameter).parameterDescriptor
            )
            InstantiationOfAbstractClass::class.java -> tracingStrategy.instantiationOfAbstractClass(trace)
            AbstractSuperCall::class.java -> tracingStrategy.abstractSuperCall(trace)
            NonApplicableCallForBuilderInferenceDiagnostic::class.java -> {
                val reportOn = (diagnostic as NonApplicableCallForBuilderInferenceDiagnostic).kotlinCall
                trace.reportDiagnosticOnce(Errors.NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE.on(reportOn.psiKotlinCall.psiCall.callElement))
            }
            OnlyInputTypesDiagnostic::class.java -> {
                val typeVariable = (diagnostic as OnlyInputTypesDiagnostic).typeVariable as? TypeVariableFromCallableDescriptor ?: return
                psiKotlinCall.psiCall.calleeExpression?.let {
                    val factory = if (context.languageVersionSettings.supportsFeature(LanguageFeature.NonStrictOnlyInputTypesChecks))
                        TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING
                    else TYPE_INFERENCE_ONLY_INPUT_TYPES
                    trace.report(factory.on(it, typeVariable.originalTypeParameter))
                }
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
        }
    }

    override fun onCallName(diagnostic: KotlinCallDiagnostic) {

    }

    override fun onTypeArgument(typeArgument: TypeArgument, diagnostic: KotlinCallDiagnostic) {

    }

    override fun onCallReceiver(callReceiver: SimpleKotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        when (diagnostic.javaClass) {
            UnsafeCallError::class.java -> {
                val implicitInvokeCheck = (callReceiver as? ReceiverExpressionKotlinCallArgument)?.isForImplicitInvoke
                    ?: callReceiver.receiver.receiverValue.type.isExtensionFunctionType
                tracingStrategy.unsafeCall(trace, callReceiver.receiver.receiverValue.type, implicitInvokeCheck)
            }

            SuperAsExtensionReceiver::class.java -> {
                val psiExpression = callReceiver.psiExpression
                if (psiExpression is KtSuperExpression) {
                    trace.report(SUPER_CANT_BE_EXTENSION_RECEIVER.on(psiExpression, psiExpression.text))
                }
            }
        }
    }

    override fun onCallArgument(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        when (diagnostic.javaClass) {
            SmartCastDiagnostic::class.java -> reportSmartCast(diagnostic as SmartCastDiagnostic)
            UnstableSmartCast::class.java -> reportUnstableSmartCast(diagnostic as UnstableSmartCast)
            TooManyArguments::class.java -> {
                trace.reportTrailingLambdaErrorOr(callArgument.psiExpression) { expr ->
                    TOO_MANY_ARGUMENTS.on(expr, (diagnostic as TooManyArguments).descriptor)
                }

                trace.markAsReported()
            }
            VarargArgumentOutsideParentheses::class.java -> trace.reportTrailingLambdaErrorOr(callArgument.psiExpression) { expr ->
                VARARG_OUTSIDE_PARENTHESES.on(expr)
            }

            MixingNamedAndPositionArguments::class.java ->
                trace.report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(callArgument.psiCallArgument.valueArgument.asElement()))

            NoneCallableReferenceCandidates::class.java -> {
                val expression = diagnostic.cast<NoneCallableReferenceCandidates>()
                    .argument.safeAs<CallableReferenceKotlinCallArgumentImpl>()?.ktCallableReferenceExpression
                reportIfNonNull(expression) {
                    trace.report(UNRESOLVED_REFERENCE.on(it.callableReference, it.callableReference))
                }
            }

            CallableReferenceCandidatesAmbiguity::class.java -> {
                val ambiguityDiagnostic = diagnostic as CallableReferenceCandidatesAmbiguity
                val expression = ambiguityDiagnostic.argument.psiExpression.safeAs<KtCallableReferenceExpression>()
                val candidates = ambiguityDiagnostic.candidates.map { it.candidate }
                reportIfNonNull(expression) {
                    trace.reportDiagnosticOnce(CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY.on(it.callableReference, candidates))
                    trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, it.callableReference, candidates)
                }
            }

            ArgumentTypeMismatchDiagnostic::class.java -> {
                require(diagnostic is ArgumentTypeMismatchDiagnostic)
                reportIfNonNull(callArgument.safeAs<PSIKotlinCallArgument>()?.valueArgument?.getArgumentExpression()) {
                    if (it.isNull()) {
                        trace.reportDiagnosticOnce(NULL_FOR_NONNULL_TYPE.on(it, diagnostic.expectedType))
                    } else {
                        trace.report(TYPE_MISMATCH.on(it, diagnostic.expectedType, diagnostic.actualType))
                    }
                }
            }

            CallableReferencesDefaultArgumentUsed::class.java -> {
                require(diagnostic is CallableReferencesDefaultArgumentUsed) {
                    "diagnostic ($diagnostic) should have type CallableReferencesDefaultArgumentUsed"
                }

                diagnostic.argument.psiExpression?.let {
                    trace.report(
                        UNSUPPORTED_FEATURE.on(
                            it, LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType to context.languageVersionSettings
                        )
                    )
                }

            }

            ResolvedToSamWithVarargDiagnostic::class.java -> {
                trace.report(TYPE_INFERENCE_CANDIDATE_WITH_SAM_AND_VARARG.on(callArgument.psiCallArgument.valueArgument.asElement()))
            }
        }
    }

    private fun <T> reportIfNonNull(element: T?, report: (T) -> Unit) {
        if (element != null) report(element)
    }

    override fun onCallArgumentName(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        val nameReference = callArgument.psiCallArgument.valueArgument.getArgumentName()?.referenceExpression ?: return
        when (diagnostic.javaClass) {
            NamedArgumentReference::class.java -> {
                trace.record(BindingContext.REFERENCE_TARGET, nameReference, (diagnostic as NamedArgumentReference).parameterDescriptor)
                trace.markAsReported()
            }
            NameForAmbiguousParameter::class.java -> trace.report(NAME_FOR_AMBIGUOUS_PARAMETER.on(nameReference))
            NameNotFound::class.java -> trace.report(NAMED_PARAMETER_NOT_FOUND.on(nameReference, nameReference))

            NamedArgumentNotAllowed::class.java -> trace.report(
                NAMED_ARGUMENTS_NOT_ALLOWED.on(
                    nameReference,
                    if ((diagnostic as NamedArgumentNotAllowed).descriptor is FunctionInvokeDescriptor) INVOKE_ON_FUNCTION_TYPE else NON_KOTLIN_FUNCTION
                )
            )
            ArgumentPassedTwice::class.java -> trace.report(ARGUMENT_PASSED_TWICE.on(nameReference))
        }
    }

    override fun onCallArgumentSpread(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        when (diagnostic.javaClass) {
            NonVarargSpread::class.java -> {
                val spreadElement = callArgument.safeAs<ExpressionKotlinCallArgumentImpl>()?.valueArgument?.getSpreadElement()
                reportIfNonNull(spreadElement) { trace.report(NON_VARARG_SPREAD.on(it)) }
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
                smartCastManager.checkAndRecordPossibleCast(
                    dataFlowValue, smartCastDiagnostic.smartCastType, argumentExpression, context, call,
                    recordExpressionType = false
                )
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
        // todo hack -- remove it after removing SmartCastManager
        reportSmartCast(SmartCastDiagnostic(unstableSmartCast.argument, unstableSmartCast.targetType, null))
    }

    override fun constraintError(diagnostic: KotlinCallDiagnostic) {
        when (diagnostic.javaClass) {
            NewConstraintError::class.java -> {
                val constraintError = diagnostic as NewConstraintError
                val position = constraintError.position.from
                val argument =
                    when (position) {
                        is ArgumentConstraintPosition -> position.argument
                        is ReceiverConstraintPosition -> position.argument
                        is LHSArgumentConstraintPosition -> position.argument
                        else -> null
                    }
                argument?.let {
                    it.safeAs<LambdaKotlinCallArgument>()?.let lambda@{ lambda ->
                        val parameterTypes = lambda.parametersTypes?.toList() ?: return@lambda
                        val index = parameterTypes.indexOf(constraintError.upperKotlinType.unwrap())
                        val lambdaExpression = lambda.psiExpression as? KtLambdaExpression ?: return@lambda
                        val parameter = lambdaExpression.valueParameters.getOrNull(index) ?: return@lambda
                        trace.report(Errors.EXPECTED_PARAMETER_TYPE_MISMATCH.on(parameter, constraintError.upperKotlinType))
                        return
                    }

                    val expression = it.psiExpression ?: return
                    val deparenthesized = KtPsiUtil.safeDeparenthesize(expression)
                    if (reportConstantTypeMismatch(constraintError, deparenthesized)) return

                    val compileTimeConstant = trace[BindingContext.COMPILE_TIME_VALUE, deparenthesized] as? TypedCompileTimeConstant
                    if (compileTimeConstant != null) {
                        val expressionType = trace[BindingContext.EXPRESSION_TYPE_INFO, expression]?.type
                        if (expressionType != null &&
                            !UnsignedTypes.isUnsignedType(compileTimeConstant.type) && UnsignedTypes.isUnsignedType(expressionType)
                        ) {
                            return
                        }
                    }
                    trace.report(
                        Errors.TYPE_MISMATCH.on(
                            deparenthesized,
                            constraintError.upperKotlinType,
                            constraintError.lowerKotlinType
                        )
                    )
                }

                (position as? ExpectedTypeConstraintPosition)?.let {
                    val call = it.topLevelCall.psiKotlinCall.psiCall.callElement.safeAs<KtExpression>()
                    reportIfNonNull(call) {
                        trace.report(
                            Errors.TYPE_MISMATCH.on(
                                it,
                                constraintError.upperKotlinType,
                                constraintError.lowerKotlinType
                            )
                        )
                    }
                }

                (position as? ExplicitTypeParameterConstraintPosition)?.let {
                    val typeArgumentReference = (it.typeArgument as SimpleTypeArgumentImpl).typeReference
                    trace.report(
                        UPPER_BOUND_VIOLATED.on(
                            typeArgumentReference,
                            constraintError.upperKotlinType,
                            constraintError.lowerKotlinType
                        )
                    )
                }

                (position as? FixVariableConstraintPosition)?.let {
                    val morePreciseDiagnosticExists = allDiagnostics.any { other ->
                        other is NewConstraintError && other.position.from !is FixVariableConstraintPosition
                    }
                    if (morePreciseDiagnosticExists) return

                    val call = it.resolvedAtom?.atom?.safeAs<PSIKotlinCall>()?.psiCall ?: call
                    val expression = call.calleeExpression ?: return

                    trace.reportDiagnosticOnce(
                        TYPE_MISMATCH.on(
                            expression,
                            constraintError.upperKotlinType,
                            constraintError.lowerKotlinType
                        )
                    )
                }
            }

            CapturedTypeFromSubtyping::class.java -> {
                val capturedError = diagnostic as CapturedTypeFromSubtyping
                val position = capturedError.position
                val argumentPosition =
                    position.safeAs<ArgumentConstraintPosition>()
                        ?: position.safeAs<IncorporationConstraintPosition>()?.from.safeAs<ArgumentConstraintPosition>()

                argumentPosition?.let {
                    val expression = it.argument.psiExpression ?: return
                    trace.reportDiagnosticOnce(
                        NEW_INFERENCE_ERROR.on(
                            expression,
                            "Capture type from subtyping ${capturedError.constraintType} for variable ${capturedError.typeVariable}"
                        )
                    )
                }
            }

            NotEnoughInformationForTypeParameter::class.java -> {
                val error = diagnostic as NotEnoughInformationForTypeParameter
                if (allDiagnostics.any {
                        (it is ConstrainingTypeIsError && it.typeVariable == error.typeVariable)
                                || it is NewConstraintError || it is WrongCountOfTypeArguments
                    }
                ) return

                val expression = when (val atom = error.resolvedAtom.atom) {
                    is PSIKotlinCall -> atom.psiCall.calleeExpression
                    is PSIKotlinCallArgument -> atom.valueArgument.getArgumentExpression()
                    else -> call.calleeExpression
                } ?: return

                val typeVariableName = when (val typeVariable = error.typeVariable) {
                    is TypeVariableFromCallableDescriptor -> typeVariable.originalTypeParameter.name.asString()
                    is TypeVariableForLambdaReturnType -> "return type of lambda"
                    else -> error("Unsupported type variable: $typeVariable")
                }
                trace.reportDiagnosticOnce(NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER.on(expression, typeVariableName))
            }
        }
    }

    private fun reportConstantTypeMismatch(constraintError: NewConstraintError, expression: KtExpression): Boolean {
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

val NewConstraintError.upperKotlinType get() = upperType as KotlinType
val NewConstraintError.lowerKotlinType get() = lowerType as KotlinType
