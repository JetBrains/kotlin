/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.diagnostics.Errors.BadNamedArgumentsTarget.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstantChecker
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

class DiagnosticReporterByTrackingStrategy(
        val constantExpressionEvaluator: ConstantExpressionEvaluator,
        val context: BasicCallResolutionContext,
        val trace: BindingTrace,
        val psiKotlinCall: PSIKotlinCall
): DiagnosticReporter {
    private val tracingStrategy: TracingStrategy get() = psiKotlinCall.tracingStrategy
    private val call: Call get() = psiKotlinCall.psiCall

    override fun onExplicitReceiver(diagnostic: KotlinCallDiagnostic) {

    }

    override fun onCall(diagnostic: KotlinCallDiagnostic) {
        when (diagnostic.javaClass) {
            VisibilityError::class.java -> tracingStrategy.invisibleMember(trace, (diagnostic as VisibilityError).invisibleMember)
            NoValueForParameter::class.java -> tracingStrategy.noValueForParameter(trace, (diagnostic as NoValueForParameter).parameterDescriptor)
            InstantiationOfAbstractClass::class.java -> tracingStrategy.instantiationOfAbstractClass(trace)
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
                val implicitInvokeCheck = (callReceiver as? ReceiverExpressionKotlinCallArgument)?.isVariableReceiverForInvoke ?: false
                tracingStrategy.unsafeCall(trace, callReceiver.receiver.receiverValue.type, implicitInvokeCheck)
            }
        }
    }

    override fun onCallArgument(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        when (diagnostic.javaClass) {
            SmartCastDiagnostic::class.java -> reportSmartCast(diagnostic as SmartCastDiagnostic)
            UnstableSmartCast::class.java -> reportUnstableSmartCast(diagnostic as UnstableSmartCast)
            TooManyArguments::class.java ->
                trace.report(TOO_MANY_ARGUMENTS.on(callArgument.psiExpression!!, (diagnostic as TooManyArguments).descriptor))
            VarargArgumentOutsideParentheses::class.java ->
                trace.report(VARARG_OUTSIDE_PARENTHESES.on(callArgument.psiExpression!!))
        }
    }

    override fun onCallArgumentName(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        val nameReference = callArgument.psiCallArgument.valueArgument.getArgumentName()?.referenceExpression ?:
                           error("Argument name should be not null for argument: $callArgument")
        when (diagnostic.javaClass) {
            NamedArgumentReference::class.java ->
                trace.record(BindingContext.REFERENCE_TARGET, nameReference, (diagnostic as NamedArgumentReference).parameterDescriptor)
            NameForAmbiguousParameter::class.java -> trace.report(NAME_FOR_AMBIGUOUS_PARAMETER.on(nameReference))
            NameNotFound::class.java -> trace.report(NAMED_PARAMETER_NOT_FOUND.on(nameReference, nameReference))

            NamedArgumentNotAllowed::class.java -> trace.report(NAMED_ARGUMENTS_NOT_ALLOWED.on(
                    nameReference,
                    if ((diagnostic as NamedArgumentNotAllowed).descriptor is FunctionInvokeDescriptor) INVOKE_ON_FUNCTION_TYPE else NON_KOTLIN_FUNCTION
            ))
            ArgumentPassedTwice::class.java -> trace.report(ARGUMENT_PASSED_TWICE.on(nameReference))
        }
    }

    override fun onCallArgumentSpread(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic) {

    }

    private fun reportSmartCast(smartCastDiagnostic: SmartCastDiagnostic) {
        val expressionArgument = smartCastDiagnostic.argument
        if (expressionArgument is ExpressionKotlinCallArgumentImpl) {
            val context = context.replaceDataFlowInfo(expressionArgument.dataFlowInfoBeforeThisArgument)
            val argumentExpression = KtPsiUtil.getLastElementDeparenthesized(expressionArgument.valueArgument.getArgumentExpression (), context.statementFilter)
            val dataFlowValue = DataFlowValueFactory.createDataFlowValue(expressionArgument.receiver.receiverValue, context)
            SmartCastManager.checkAndRecordPossibleCast(
                    dataFlowValue, smartCastDiagnostic.smartCastType, argumentExpression, context, call,
                    recordExpressionType = true)
        }
        else if(expressionArgument is ReceiverExpressionKotlinCallArgument) {
            val receiverValue = expressionArgument.receiver.receiverValue
            val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverValue, context)
            SmartCastManager.checkAndRecordPossibleCast(
                    dataFlowValue, smartCastDiagnostic.smartCastType, (receiverValue as? ExpressionReceiver)?.expression, context, call,
                    recordExpressionType = true)
        }
    }

    private fun reportUnstableSmartCast(unstableSmartCast: UnstableSmartCast) {
        // todo hack -- remove it after removing SmartCastManager
        reportSmartCast(SmartCastDiagnostic(unstableSmartCast.argument, unstableSmartCast.targetType))
    }

    override fun constraintError(diagnostic: KotlinCallDiagnostic) {
        when (diagnostic.javaClass) {
            NewConstraintError::class.java -> {
                val constraintError = diagnostic as NewConstraintError
                val position = constraintError.position.from
                val argument = (position as? ArgumentConstraintPosition)?.argument
                               ?: (position as? ReceiverConstraintPosition)?.argument
                argument?.let {
                    val expression = it.psiExpression ?: return
                    if (reportConstantTypeMismatch(constraintError, expression)) return
                    trace.report(Errors.TYPE_MISMATCH.on(expression, constraintError.upperType, constraintError.lowerType))
                }
                (position as? ExplicitTypeParameterConstraintPosition)?.let {
                    val typeArgumentReference = (it.typeArgument as SimpleTypeArgumentImpl).typeReference
                    trace.report(UPPER_BOUND_VIOLATED.on(typeArgumentReference, constraintError.upperType, constraintError.lowerType))
                }
            }
            CapturedTypeFromSubtyping::class.java -> {
                val capturedError = diagnostic as CapturedTypeFromSubtyping
                (capturedError.position as? ArgumentConstraintPosition)?.let {
                    val expression = it.argument.psiExpression ?: return
                    trace.report(NEW_INFERENCE_ERROR.on(expression, "Capture type from subtyping ${capturedError.constraintType} for variable ${capturedError.typeVariable}"))
                }
            }
        }
    }

    private fun reportConstantTypeMismatch(constraintError: NewConstraintError, expression: KtExpression): Boolean {
        if (expression is KtConstantExpression) {
            val builtIns = context.scope.ownerDescriptor.builtIns
            val constantValue = constantExpressionEvaluator.evaluateToConstantValue(expression, trace, context.expectedType)
            val hasConstantTypeError = CompileTimeConstantChecker(context, builtIns, true)
                    .checkConstantExpressionType(constantValue, expression, constraintError.upperType)
            if (hasConstantTypeError) return true
        }
        return false
    }

}