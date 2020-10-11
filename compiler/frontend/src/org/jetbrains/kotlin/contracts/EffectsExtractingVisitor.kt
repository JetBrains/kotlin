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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.ContractProviderKey
import org.jetbrains.kotlin.contracts.model.*
import org.jetbrains.kotlin.contracts.model.functors.*
import org.jetbrains.kotlin.contracts.model.structure.*
import org.jetbrains.kotlin.contracts.model.visitors.Reducer
import org.jetbrains.kotlin.contracts.parsing.isEqualsDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.UnsignedErrorValueTypeConstant
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * Visits a given PSI-tree of call (and nested calls, if any) and extracts information
 * about effects of that call.
 */
class EffectsExtractingVisitor(
    private val trace: BindingTrace,
    private val moduleDescriptor: ModuleDescriptor,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val languageVersionSettings: LanguageVersionSettings
) : KtVisitor<Computation, Unit>() {
    private val builtIns: KotlinBuiltIns get() = moduleDescriptor.builtIns
    private val reducer: Reducer = Reducer(builtIns)

    fun extractOrGetCached(element: KtElement): Computation {
        trace[BindingContext.EXPRESSION_EFFECTS, element]?.let { return it }
        return element.accept(this, Unit).also { trace.record(BindingContext.EXPRESSION_EFFECTS, element, it) }
    }

    override fun visitKtElement(element: KtElement, data: Unit): Computation {
        val resolvedCall = element.getResolvedCall(trace.bindingContext) ?: return UNKNOWN_COMPUTATION
        if (resolvedCall.isCallWithUnsupportedReceiver()) return UNKNOWN_COMPUTATION

        val arguments = resolvedCall.getCallArgumentsAsComputations() ?: return UNKNOWN_COMPUTATION
        val typeSubstitution = resolvedCall.getTypeSubstitution()

        val descriptor = resolvedCall.resultingDescriptor
        return when {
            descriptor.isEqualsDescriptor() -> CallComputation(
                ESBooleanType,
                EqualsFunctor(false).invokeWithArguments(arguments, typeSubstitution, reducer)
            )
            descriptor is ValueDescriptor -> ESVariableWithDataFlowValue(
                descriptor,
                (element as KtExpression).createDataFlowValue() ?: return UNKNOWN_COMPUTATION
            )
            descriptor is FunctionDescriptor -> {
                val esType = descriptor.returnType?.toESType()
                CallComputation(
                    esType,
                    descriptor.getFunctor()?.invokeWithArguments(arguments, typeSubstitution, reducer) ?: emptyList()
                )
            }
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

        val compileTimeConstant: CompileTimeConstant<*> =
            bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expression) ?: return UNKNOWN_COMPUTATION
        if (compileTimeConstant.isError || compileTimeConstant is UnsignedErrorValueTypeConstant) return UNKNOWN_COMPUTATION

        val value: Any? = compileTimeConstant.getValue(type)

        return when (value) {
            is Boolean -> ESConstants.booleanValue(value)
            null -> ESConstants.nullValue
            else -> UNKNOWN_COMPUTATION
        }
    }

    override fun visitIsExpression(expression: KtIsExpression, data: Unit): Computation {
        val rightType = trace[BindingContext.TYPE, expression.typeReference]?.toESType() ?: return UNKNOWN_COMPUTATION
        val arg = extractOrGetCached(expression.leftHandSide)
        return CallComputation(
            ESBooleanType,
            IsFunctor(rightType, expression.isNegated).invokeWithArguments(listOf(arg), emptyMap(), reducer)
        )
    }

    override fun visitSafeQualifiedExpression(expression: KtSafeQualifiedExpression, data: Unit?): Computation {
        val computation = super.visitSafeQualifiedExpression(expression, data)
        if (computation === UNKNOWN_COMPUTATION) return computation

        // For safecall any clauses of form 'returns(null) -> ...' are incorrect, because safecall can return
        // null bypassing function's contract, so we have to filter them out

        fun ESEffect.containsReturnsNull(): Boolean =
            isReturns { value == ESConstants.nullValue } || this is ConditionalEffect && this.simpleEffect.containsReturnsNull()

        val effectsWithoutReturnsNull = computation.effects.filter { !it.containsReturnsNull() }
        return CallComputation(computation.type, effectsWithoutReturnsNull)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, data: Unit): Computation {
        val left = extractOrGetCached(expression.left ?: return UNKNOWN_COMPUTATION)
        val right = extractOrGetCached(expression.right ?: return UNKNOWN_COMPUTATION)

        val args = listOf(left, right)

        return when (expression.operationToken) {
            KtTokens.EXCLEQ -> CallComputation(ESBooleanType, EqualsFunctor(true).invokeWithArguments(args, emptyMap(), reducer))
            KtTokens.EQEQ -> CallComputation(ESBooleanType, EqualsFunctor(false).invokeWithArguments(args, emptyMap(), reducer))
            KtTokens.ANDAND -> CallComputation(ESBooleanType, AndFunctor().invokeWithArguments(args, emptyMap(), reducer))
            KtTokens.OROR -> CallComputation(ESBooleanType, OrFunctor().invokeWithArguments(args, emptyMap(), reducer))
            else -> UNKNOWN_COMPUTATION
        }
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression, data: Unit): Computation {
        val arg = extractOrGetCached(expression.baseExpression ?: return UNKNOWN_COMPUTATION)
        return when (expression.operationToken) {
            KtTokens.EXCL -> CallComputation(ESBooleanType, NotFunctor().invokeWithArguments(arg))
            else -> UNKNOWN_COMPUTATION
        }
    }

    private fun ReceiverValue.toComputation(): Computation = when (this) {
        is ExpressionReceiver -> extractOrGetCached(expression)
        is ExtensionReceiver -> {
            if (languageVersionSettings.supportsFeature(LanguageFeature.ContractsOnCallsWithImplicitReceiver)) {
                ESReceiverWithDataFlowValue(this, createDataFlowValue())
            } else {
                UNKNOWN_COMPUTATION
            }
        }
        else -> UNKNOWN_COMPUTATION
    }

    private fun ExtensionReceiver.createDataFlowValue(): DataFlowValue {
        return dataFlowValueFactory.createDataFlowValue(
            receiverValue = this,
            bindingContext = trace.bindingContext,
            containingDeclarationOrModule = this.declarationDescriptor
        )
    }

    private fun KtExpression.createDataFlowValue(): DataFlowValue? {
        return dataFlowValueFactory.createDataFlowValue(
            expression = this,
            type = trace.getType(this) ?: return null,
            bindingContext = trace.bindingContext,
            containingDeclarationOrModule = moduleDescriptor
        )
    }

    private fun FunctionDescriptor.getFunctor(): Functor? {
        val contractDescription = getUserData(ContractProviderKey)?.getContractDescription() ?: return null
        return contractDescription.getFunctor(moduleDescriptor)
    }

    private fun ResolvedCall<*>.isCallWithUnsupportedReceiver(): Boolean =
        (extensionReceiver as? ExpressionReceiver)?.expression?.getResolvedCall(trace.bindingContext) == this ||
                (dispatchReceiver as? ExpressionReceiver)?.expression?.getResolvedCall(trace.bindingContext) == this ||
                (explicitReceiverKind == ExplicitReceiverKind.BOTH_RECEIVERS)

    private fun ResolvedCall<*>.getCallArgumentsAsComputations(): List<Computation>? {
        val arguments = mutableListOf<Computation>()
        arguments.addIfNotNull(extensionReceiver?.toComputation())
        arguments.addIfNotNull(dispatchReceiver?.toComputation())

        val passedValueArguments = valueArgumentsByIndex ?: return null

        passedValueArguments.mapTo(arguments) { it.toComputation() ?: return null }

        return arguments
    }

    private fun ResolvedCall<*>.getTypeSubstitution(): ESTypeSubstitution {
        val result = mutableMapOf<ESKotlinType, ESKotlinType>()
        for ((typeParameter, typeArgument) in typeArguments) {
            result[ESKotlinType(typeParameter.defaultType)] = ESKotlinType(typeArgument)
        }
        return result
    }

    private fun ResolvedValueArgument.toComputation(): Computation? {
        return when (this) {
            // Assume that we don't know anything about default arguments
            // Note that we don't want to return 'null' here, because 'null' indicates that we can't
            // analyze whole call, which is undesired for cases like `kotlin.test.assertNotNull`
            is DefaultValueArgument -> UNKNOWN_COMPUTATION

            // We prefer to throw away calls with varags completely, just to be safe
            // Potentially, we could return UNKNOWN_COMPUTATION here too
            is VarargValueArgument -> null

            is ExpressionValueArgument -> valueArgument?.toComputation()

            // Should be exhaustive
            else -> throw IllegalStateException("Unexpected ResolvedValueArgument $this")
        }
    }

    private fun ValueArgument.toComputation(): Computation? {
        return when (this) {
            is KtLambdaArgument -> getLambdaExpression()?.let { ESLambda(it) }
            is KtValueArgument -> getArgumentExpression()?.let {
                if (it is KtLambdaExpression) ESLambda(it)
                else extractOrGetCached(it)
            }
            else -> null
        }
    }
}
