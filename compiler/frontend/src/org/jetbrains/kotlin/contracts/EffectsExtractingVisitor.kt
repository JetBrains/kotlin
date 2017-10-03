/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.contracts

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.contracts.interpretation.ContractInterpretationDispatcher
import org.jetbrains.kotlin.contracts.model.Computation
import org.jetbrains.kotlin.contracts.model.Functor
import org.jetbrains.kotlin.contracts.model.functors.*
import org.jetbrains.kotlin.contracts.model.structure.CallComputation
import org.jetbrains.kotlin.contracts.model.structure.ESConstant
import org.jetbrains.kotlin.contracts.model.structure.UNKNOWN_COMPUTATION
import org.jetbrains.kotlin.contracts.model.structure.lift
import org.jetbrains.kotlin.contracts.parsing.isEqualsDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * Visits a given PSI-tree of call (and nested calls, if any) and extracts information
 * about effects of that call.
 */
class EffectsExtractingVisitor(
        private val trace: BindingTrace,
        private val moduleDescriptor: ModuleDescriptor
) : KtVisitor<Computation, Unit>() {
    fun extractOrGetCached(element: KtElement): Computation {
        trace[BindingContext.EXPRESSION_EFFECTS, element]?.let { return it }
        return element.accept(this, Unit).also { trace.record(BindingContext.EXPRESSION_EFFECTS, element, it) }
    }

    override fun visitKtElement(element: KtElement, data: Unit): Computation {
        val resolvedCall = element.getResolvedCall(trace.bindingContext) ?: return UNKNOWN_COMPUTATION
        if (resolvedCall.isCallWithUnsupportedReceiver()) return UNKNOWN_COMPUTATION

        val arguments = resolvedCall.getCallArgumentsAsComputations() ?: return UNKNOWN_COMPUTATION

        val descriptor = resolvedCall.resultingDescriptor
        return when {
            descriptor.isEqualsDescriptor() -> CallComputation(DefaultBuiltIns.Instance.booleanType, EqualsFunctor(false).invokeWithArguments(arguments))
            descriptor is ValueDescriptor -> ESDataFlowValue(descriptor, (element as KtExpression).createDataFlowValue() ?: return UNKNOWN_COMPUTATION)
            descriptor is FunctionDescriptor -> CallComputation(descriptor.returnType, descriptor.getFunctor()?.invokeWithArguments(arguments) ?: emptyList())
            else -> UNKNOWN_COMPUTATION
        }
    }

    // We need lambdas only as arguments currently, and it is processed in 'visitElement' while parsing arguments of call.
    // For all other cases we don't need lambdas.
    override fun visitLambdaExpression(expression: KtLambdaExpression, data: Unit?): Computation = UNKNOWN_COMPUTATION

    override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, data: Unit): Computation =
            KtPsiUtil.deparenthesize(expression)?.accept(this, data) ?: UNKNOWN_COMPUTATION

    override fun visitConstantExpression(expression: KtConstantExpression, data: Unit): Computation {
        val bindingContext = trace.bindingContext

        val type: KotlinType = bindingContext.getType(expression) ?: return UNKNOWN_COMPUTATION

        val compileTimeConstant: CompileTimeConstant<*>
                = bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expression) ?: return UNKNOWN_COMPUTATION
        val value: Any? = compileTimeConstant.getValue(type)

        return when (value) {
            is Boolean -> value.lift()
            null -> ESConstant.NULL
            else -> UNKNOWN_COMPUTATION
        }
    }

    override fun visitIsExpression(expression: KtIsExpression, data: Unit): Computation {
        val rightType: KotlinType = trace[BindingContext.TYPE, expression.typeReference] ?: return UNKNOWN_COMPUTATION
        val arg = extractOrGetCached(expression.leftHandSide)
        return CallComputation(DefaultBuiltIns.Instance.booleanType, IsFunctor(rightType, expression.isNegated).invokeWithArguments(listOf(arg)))
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, data: Unit): Computation {
        val left = extractOrGetCached(expression.left ?: return UNKNOWN_COMPUTATION)
        val right = extractOrGetCached(expression.right ?: return UNKNOWN_COMPUTATION)

        val args = listOf(left, right)

        return when (expression.operationToken) {
            KtTokens.EXCLEQ -> CallComputation(DefaultBuiltIns.Instance.booleanType, EqualsFunctor(true).invokeWithArguments(args))
            KtTokens.EQEQ -> CallComputation(DefaultBuiltIns.Instance.booleanType, EqualsFunctor(false).invokeWithArguments(args))
            KtTokens.ANDAND -> CallComputation(DefaultBuiltIns.Instance.booleanType, AndFunctor().invokeWithArguments(args))
            KtTokens.OROR -> CallComputation(DefaultBuiltIns.Instance.booleanType, OrFunctor().invokeWithArguments(args))
            else -> UNKNOWN_COMPUTATION
        }
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression, data: Unit): Computation {
        val arg = extractOrGetCached(expression.baseExpression ?: return UNKNOWN_COMPUTATION)
        return when (expression.operationToken) {
            KtTokens.EXCL -> CallComputation(DefaultBuiltIns.Instance.booleanType, NotFunctor().invokeWithArguments(arg))
            else -> UNKNOWN_COMPUTATION
        }
    }

    private fun ReceiverValue.toComputation(): Computation = when (this) {
        is ExpressionReceiver -> extractOrGetCached(expression)
        else -> UNKNOWN_COMPUTATION
    }

    private fun KtExpression.createDataFlowValue(): DataFlowValue? {
        return DataFlowValueFactory.createDataFlowValue(
                expression = this,
                type = trace.getType(this) ?: return null,
                bindingContext = trace.bindingContext,
                containingDeclarationOrModule = moduleDescriptor
        )
    }

    private fun FunctionDescriptor.getFunctor(): Functor? {
        trace[BindingContext.FUNCTOR, this]?.let { return it }

        val functor = ContractInterpretationDispatcher().resolveFunctor(this) ?: return null
        trace.record(BindingContext.FUNCTOR, this, functor)
        return functor
    }

    private fun ResolvedCall<*>.isCallWithUnsupportedReceiver(): Boolean =
            (extensionReceiver as? ExpressionReceiver)?.expression?.getResolvedCall(trace.bindingContext) == this ||
            (dispatchReceiver as? ExpressionReceiver)?.expression?.getResolvedCall(trace.bindingContext) == this ||
            (explicitReceiverKind == ExplicitReceiverKind.BOTH_RECEIVERS)

    private fun ResolvedCall<*>.getCallArgumentsAsComputations(): List<Computation>? {
        val arguments = mutableListOf<Computation>()
        arguments.addIfNotNull(extensionReceiver?.toComputation())
        arguments.addIfNotNull(dispatchReceiver?.toComputation())

        valueArgumentsByIndex?.mapTo(arguments) {
            val valueArgument = (it as? ExpressionValueArgument)?.valueArgument ?: return null
            when (valueArgument) {
                is KtLambdaArgument -> ESLambda(valueArgument.getLambdaExpression())
                else -> extractOrGetCached(valueArgument.getArgumentExpression() ?: return null)
            }
        } ?: return null

        return arguments
    }
}