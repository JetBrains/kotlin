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

package org.jetbrains.kotlin.psi2ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.scopes.receivers.*

class IrExpressionGenerator(
        override val context: IrGeneratorContext,
        val declarationFactory: IrDeclarationFactory
) : KtVisitor<IrExpression, Nothing?>(), IrGenerator {
    fun generateExpression(ktExpression: KtExpression) = ktExpression.generate()

    private fun KtElement.generate(): IrExpression =
            deparenthesize()
                    .accept(this@IrExpressionGenerator, null)
                    .smartCastIfNeeded(this)

    private fun KtExpression.type() =
            getType(this) ?: TODO("no type for expression")

    private fun IrExpression.smartCastIfNeeded(ktElement: KtElement): IrExpression {
        if (ktElement is KtExpression) {
            val smartCastType = get(BindingContext.SMARTCAST, ktElement)
            if (smartCastType != null) {
                return IrTypeOperatorExpressionImpl(
                        ktElement.startOffset, ktElement.endOffset, smartCastType,
                        IrTypeOperator.SMART_AS, smartCastType
                ).apply { argument = this@smartCastIfNeeded }
            }
        }
        return this
    }

    override fun visitExpression(expression: KtExpression, data: Nothing?): IrExpression =
            IrDummyExpression(expression.startOffset, expression.endOffset, expression.type(), expression.javaClass.simpleName)

    override fun visitBlockExpression(expression: KtBlockExpression, data: Nothing?): IrExpression {
        val irBlock = IrBlockExpressionImpl(expression.startOffset, expression.endOffset, expression.type(), false)
        expression.statements.forEach { irBlock.addChildExpression(it.generate()) }
        return irBlock
    }

    override fun visitReturnExpression(expression: KtReturnExpression, data: Nothing?): IrExpression =
            IrReturnExpressionImpl(expression.startOffset, expression.endOffset, expression.type())
                    .apply { this.argument = expression.returnedExpression?.generate() }

    override fun visitConstantExpression(expression: KtConstantExpression, data: Nothing?): IrExpression {
        val compileTimeConstant = ConstantExpressionEvaluator.getConstant(expression, context.bindingContext)
                                  ?: error("KtConstantExpression was not evaluated: ${expression.text}")
        val constantValue = compileTimeConstant.toConstantValue(expression.type())
        val constantType = constantValue.type

        return when (constantValue) {
            is StringValue ->
                IrLiteralExpressionImpl.string(expression.startOffset, expression.endOffset, constantType, constantValue.value)
            is IntValue ->
                IrLiteralExpressionImpl.int(expression.startOffset, expression.endOffset, constantType, constantValue.value)
            else ->
                TODO("handle other literal types: ${constantValue.type}")
        }
    }

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, data: Nothing?): IrExpression {
        if (expression.entries.size == 1 && expression.entries[0] is KtLiteralStringTemplateEntry) {
            return expression.entries[0].generate()
        }

        val irStringTemplate = IrStringConcatenationExpressionImpl(expression.startOffset, expression.endOffset, expression.type())
        expression.entries.forEach { irStringTemplate.addChildExpression(it.generate()) }
        return irStringTemplate
    }

    override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry, data: Nothing?): IrExpression =
            IrLiteralExpressionImpl.string(entry.startOffset, entry.endOffset, context.builtIns.stringType, entry.text)

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Nothing?): IrExpression {
        val resolvedCall = getResolvedCall(expression)

        if (resolvedCall is VariableAsFunctionResolvedCall) {
            TODO("Unexpected VariableAsFunctionResolvedCall")
        }

        val descriptor = resolvedCall?.resultingDescriptor ?: get(BindingContext.REFERENCE_TARGET, expression)

        return when (descriptor) {
            is ClassDescriptor ->
                if (DescriptorUtils.isObject(descriptor))
                    IrGetObjectValueExpressionImpl(expression.startOffset, expression.endOffset, expression.type(), descriptor)
                else if (DescriptorUtils.isEnumEntry(descriptor))
                    IrGetEnumValueExpressionImpl(expression.startOffset, expression.endOffset, expression.type(), descriptor)
                else
                    IrGetObjectValueExpressionImpl(expression.startOffset, expression.endOffset, expression.type(),
                                          descriptor.companionObjectDescriptor ?: error("Class value without companion object: $descriptor"))
            is PropertyDescriptor -> {
                generateCall(expression, resolvedCall ?: TODO("Property, no resolved call: ${descriptor.name}"), null)
            }
            is VariableDescriptor ->
                IrGetVariableExpressionImpl(expression.startOffset, expression.endOffset, expression.type(), descriptor)
            else ->
                IrDummyExpression(expression.startOffset, expression.endOffset, expression.type(),
                                  expression.getReferencedName() +
                                  ": ${descriptor?.name} ${descriptor?.javaClass?.simpleName}")
        }
    }

    override fun visitCallExpression(expression: KtCallExpression, data: Nothing?): IrExpression {
        val resolvedCall = getResolvedCall(expression) ?: TODO("No resolved call for call expression")

        if (resolvedCall is VariableAsFunctionResolvedCall) {
            TODO("VariableAsFunctionResolvedCall = variable call + invoke call")
        }

        return generateCall(expression, resolvedCall, null, null)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression, data: Nothing?): IrExpression =
            expression.selectorExpression!!.accept(this, data)

    override fun visitSafeQualifiedExpression(expression: KtSafeQualifiedExpression, data: Nothing?): IrExpression =
            expression.selectorExpression!!.accept(this, data)

    override fun visitThisExpression(expression: KtThisExpression, data: Nothing?): IrExpression {
        val referenceTarget = getOrFail(BindingContext.REFERENCE_TARGET, expression.instanceReference) { "No reference target for this" }
        return when (referenceTarget) {
            is ClassDescriptor ->
                IrThisExpressionImpl(expression.startOffset, expression.endOffset, expression.type(), referenceTarget)
            is CallableDescriptor ->
                IrGetExtensionReceiverExpressionImpl(expression.startOffset, expression.endOffset, expression.type(),
                                                     referenceTarget.extensionReceiverParameter!!)
            else ->
                error("Expected this or receiver: $referenceTarget")
        }
    }

    private fun generateCall(
            ktExpression: KtExpression,
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            operator: IrOperator? = null,
            superQualifier: ClassDescriptor? = null
    ): IrExpression {
        val descriptor = resolvedCall.resultingDescriptor
        return when (descriptor) {
            is PropertyDescriptor ->
                IrGetPropertyExpressionImpl(
                        ktExpression.startOffset, ktExpression.endOffset, ktExpression.type(),
                        resolvedCall.call.isSafeCall(), descriptor
                )
            else ->
                IrCallExpressionImpl(
                        ktExpression.startOffset, ktExpression.endOffset, ktExpression.type(),
                        resolvedCall.resultingDescriptor, resolvedCall.call.isSafeCall(), operator, superQualifier
                ).apply {
                    val valueParameters = resolvedCall.resultingDescriptor.valueParameters
                    val valueArguments = resolvedCall.valueArgumentsByIndex ?: TODO("null for value arguments: ${ktExpression.text}")
                    for (index in valueArguments.indices) {
                        val valueArgument = valueArguments[index]
                        val valueParameter = valueParameters[index]
                        putValueArgument(valueParameter, generateValueArgument(valueParameter, valueArgument))
                    }
                }

        }.apply {
            dispatchReceiver = generateReceiver(ktExpression, resolvedCall.dispatchReceiver)
            extensionReceiver = generateReceiver(ktExpression, resolvedCall.extensionReceiver)
        }
    }

    private fun generateReceiver(ktExpression: KtExpression, receiver: ReceiverValue?): IrExpression? =
            when (receiver) {
                is ImplicitClassReceiver ->
                    IrThisExpressionImpl(ktExpression.startOffset, ktExpression.startOffset, receiver.type, receiver.classDescriptor)
                is ThisClassReceiver ->
                    (receiver as? ExpressionReceiver)?.expression?.let { receiverExpression ->
                        IrThisExpressionImpl(receiverExpression.startOffset, receiverExpression.endOffset, receiver.type, receiver.classDescriptor)
                    } ?: TODO("Non-implicit ThisClassReceiver should be an expression receiver")
                is ExpressionReceiver ->
                    receiver.expression.generate()
                is ClassValueReceiver ->
                    IrGetObjectValueExpressionImpl(receiver.expression.startOffset, receiver.expression.endOffset, receiver.type,
                                                   receiver.classQualifier.descriptor)
                is ExtensionReceiver ->
                    IrGetExtensionReceiverExpressionImpl(ktExpression.startOffset, ktExpression.startOffset, receiver.type,
                                                         receiver.declarationDescriptor.extensionReceiverParameter!!)
                null ->
                    null
                else ->
                    TODO("Receiver: ${receiver.javaClass.simpleName}")
            }

    private fun generateValueArgument(valueParameter: ValueParameterDescriptor, valueArgument: ResolvedValueArgument): IrExpression? {
        if (valueParameter.varargElementType == null) {
            assert(valueArgument.arguments.size == 1) { "Single value argument expected for a non-vararg parameter" }
            return valueArgument.arguments.single().getArgumentExpression()!!.generate()
        }
        else {
            TODO("vararg")
        }
    }
}
