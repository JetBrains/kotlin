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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.assertCast
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator

class IrStatementGenerator(
        override val context: IrGeneratorContext,
        val declarationFactory: IrLocalDeclarationsFactory
) : KtVisitor<IrStatement, Nothing?>(), IrGenerator {

    fun generateStatement(ktExpression: KtExpression) = ktExpression.generate()
    fun generateExpression(ktExpression: KtExpression) = ktExpression.generateExpression()

    private fun KtElement.generate(): IrStatement =
            deparenthesize()
                    .accept(this@IrStatementGenerator, null)
                    .applySmartCastIfNeeded(this)

    private fun KtElement.generateExpression(): IrExpression =
            generate().assertCast()

    private fun IrStatement.applySmartCastIfNeeded(ktElement: KtElement): IrStatement {
        if (this is IrExpression && ktElement is KtExpression) {
            val smartCastType = get(BindingContext.SMARTCAST, ktElement)
            if (smartCastType != null) {
                return IrTypeOperatorExpressionImpl(
                        ktElement.startOffset, ktElement.endOffset, smartCastType,
                        IrTypeOperator.SMART_AS, this@applySmartCastIfNeeded, smartCastType
                )
            }
        }
        return this
    }

    override fun visitExpression(expression: KtExpression, data: Nothing?): IrStatement =
            IrDummyExpression(expression.startOffset, expression.endOffset, getType(expression), expression.javaClass.simpleName)

    override fun visitProperty(property: KtProperty, data: Nothing?): IrStatement {
        if (property.delegateExpression != null) TODO("Local delegated property")

        val variableDescriptor = getOrFail(BindingContext.VARIABLE, property)

        val irLocalVariable = declarationFactory.createLocalVariable(property, variableDescriptor)
        irLocalVariable.initializer = property.initializer?.generateExpression()

        return irLocalVariable
    }

    override fun visitDestructuringDeclaration(multiDeclaration: KtDestructuringDeclaration, data: Nothing?): IrStatement {
        // TODO use some special form that introduces multiple declarations into surrounding scope?

        val irBlock = IrBlockExpressionImpl(multiDeclaration.startOffset, multiDeclaration.endOffset, getType(multiDeclaration),
                                            hasResult = false, isDesugared = true)
        val ktInitializer = multiDeclaration.initializer!!
        val irInitializer = declarationFactory.createTemporaryVariable(ktInitializer.generateExpression())
        irBlock.addStatement(irInitializer)

        val irCallGenerator = IrCallGenerator(this).apply { putTemporary(ktInitializer, irInitializer.descriptor) }

        for ((index, ktEntry) in multiDeclaration.entries.withIndex()) {
            val componentResolvedCall = getOrFail(BindingContext.COMPONENT_RESOLVED_CALL, ktEntry)
            val componentVariable = getOrFail(BindingContext.VARIABLE, ktEntry)
            val irComponentCall = irCallGenerator.generateCall(ktEntry, componentVariable.type, componentResolvedCall,
                                                               IrOperator.COMPONENT_N.withIndex(index + 1))
            val irComponentVar = declarationFactory.createLocalVariable(ktEntry, componentVariable, irComponentCall)
            irBlock.addStatement(irComponentVar)
        }

        return irBlock
    }

    override fun visitBlockExpression(expression: KtBlockExpression, data: Nothing?): IrStatement {
        val irBlock = IrBlockExpressionImpl(expression.startOffset, expression.endOffset, getType(expression),
                                            hasResult = isUsedAsExpression(expression), isDesugared = false)
        expression.statements.forEach { irBlock.addStatement(it.generate()) }
        return irBlock
    }

    override fun visitReturnExpression(expression: KtReturnExpression, data: Nothing?): IrStatement =
            IrReturnExpressionImpl(expression.startOffset, expression.endOffset, getType(expression),
                                   expression.returnedExpression?.generateExpression())

    override fun visitConstantExpression(expression: KtConstantExpression, data: Nothing?): IrExpression {
        val compileTimeConstant = ConstantExpressionEvaluator.getConstant(expression, context.bindingContext)
                                  ?: error("KtConstantExpression was not evaluated: ${expression.text}")
        val constantValue = compileTimeConstant.toConstantValue(getTypeOrFail(expression))
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

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, data: Nothing?): IrStatement {
        if (expression.entries.size == 1 && expression.entries[0] is KtLiteralStringTemplateEntry) {
            return expression.entries[0].generate()
        }

        val irStringTemplate = IrStringConcatenationExpressionImpl(expression.startOffset, expression.endOffset, getType(expression))
        expression.entries.forEach { irStringTemplate.addArgument(it.generateExpression()) }
        return irStringTemplate
    }

    override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry, data: Nothing?): IrStatement =
            IrLiteralExpressionImpl.string(entry.startOffset, entry.endOffset, context.builtIns.stringType, entry.text)

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Nothing?): IrExpression {
        val resolvedCall = getResolvedCall(expression)

        if (resolvedCall is VariableAsFunctionResolvedCall) {
            TODO("Unexpected VariableAsFunctionResolvedCall")
        }

        val descriptor = resolvedCall?.resultingDescriptor

        return when (descriptor) {
            is ClassDescriptor ->
                if (DescriptorUtils.isObject(descriptor))
                    IrGetObjectValueExpressionImpl(expression.startOffset, expression.endOffset, getType(expression), descriptor)
                else if (DescriptorUtils.isEnumEntry(descriptor))
                    IrGetEnumValueExpressionImpl(expression.startOffset, expression.endOffset, getType(expression), descriptor)
                else
                    IrGetObjectValueExpressionImpl(expression.startOffset, expression.endOffset, getType(expression),
                                                   descriptor.companionObjectDescriptor ?: error("Class value without companion object: $descriptor"))
            is PropertyDescriptor -> {
                IrCallGenerator(this).generateCall(expression, resolvedCall)
            }
            is VariableDescriptor ->
                IrGetVariableExpressionImpl(expression.startOffset, expression.endOffset, getType(expression), descriptor)
            else ->
                IrDummyExpression(expression.startOffset, expression.endOffset, getType(expression),
                                  expression.getReferencedName() +
                                  ": ${descriptor?.name} ${descriptor?.javaClass?.simpleName}")
        }
    }

    override fun visitCallExpression(expression: KtCallExpression, data: Nothing?): IrStatement {
        val resolvedCall = getResolvedCall(expression) ?: TODO("No resolved call for call expression")

        if (resolvedCall is VariableAsFunctionResolvedCall) {
            TODO("VariableAsFunctionResolvedCall = variable call + invoke call")
        }

        return IrCallGenerator(this).generateCall(expression, resolvedCall)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression, data: Nothing?): IrStatement =
            expression.selectorExpression!!.accept(this, data)

    override fun visitSafeQualifiedExpression(expression: KtSafeQualifiedExpression, data: Nothing?): IrStatement =
            expression.selectorExpression!!.accept(this, data)

    override fun visitThisExpression(expression: KtThisExpression, data: Nothing?): IrExpression {
        val referenceTarget = getOrFail(BindingContext.REFERENCE_TARGET, expression.instanceReference) { "No reference target for this" }
        return when (referenceTarget) {
            is ClassDescriptor ->
                IrThisExpressionImpl(expression.startOffset, expression.endOffset, getType(expression), referenceTarget)
            is CallableDescriptor ->
                IrGetExtensionReceiverExpressionImpl(
                        expression.startOffset, expression.endOffset, getType(expression),
                        referenceTarget.extensionReceiverParameter!!
                )
            else ->
                error("Expected this or receiver: $referenceTarget")
        }
    }
}
