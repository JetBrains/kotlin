/**
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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.assertCast
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeAliasImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.psi2ir.intermediate.IntermediateValue
import org.jetbrains.kotlin.psi2ir.intermediate.createTemporaryVariableInBlock
import org.jetbrains.kotlin.psi2ir.intermediate.setExplicitReceiverValue
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import java.lang.AssertionError

class StatementGenerator(
        val bodyGenerator: BodyGenerator,
        override val scope: Scope
) : KtVisitor<IrStatement, Nothing?>(), GeneratorWithScope {
    override val context: GeneratorContext get() = bodyGenerator.context
    val scopeOwner: DeclarationDescriptor get() = bodyGenerator.scopeOwner

    fun generateStatement(ktElement: KtElement): IrStatement =
            ktElement.genStmt()

    fun generateStatements(ktStatements: List<KtExpression>, to: IrStatementContainer) =
            ktStatements.mapTo(to.statements) { generateStatement(it) }

    fun generateExpression(ktExpression: KtExpression): IrExpression =
            ktExpression.genExpr()

    private fun KtElement.genStmt(): IrStatement =
            try {
                deparenthesize().accept(this@StatementGenerator, null)
            }
            catch (e: Exception) {
                ErrorExpressionGenerator(this@StatementGenerator).generateErrorExpression(this, e)
            }

    private fun KtElement.genExpr(): IrExpression =
            genStmt().assertCast()

    override fun visitExpression(expression: KtExpression, data: Nothing?): IrStatement =
            createDummyExpression(expression, expression::class.java.simpleName)

    override fun visitProperty(property: KtProperty, data: Nothing?): IrStatement {
        val variableDescriptor = getOrFail(BindingContext.VARIABLE, property)

        property.delegate?.let { ktDelegate ->
            return generateLocalDelegatedProperty(property, ktDelegate, variableDescriptor as VariableDescriptorWithAccessors,
                                                  bodyGenerator.scopeOwnerSymbol)
        }

        return context.symbolTable.declareVariable(
                property.startOffset, property.endOffset, IrDeclarationOrigin.DEFINED,
                variableDescriptor, property.initializer?.genExpr()
        )
    }

    private fun generateLocalDelegatedProperty(
            ktProperty: KtProperty,
            ktDelegate: KtPropertyDelegate,
            variableDescriptor: VariableDescriptorWithAccessors,
            scopeOwnerSymbol: IrSymbol
    ): IrStatement =
            DelegatedPropertyGenerator(context)
                    .generateLocalDelegatedProperty(ktProperty, ktDelegate, variableDescriptor, scopeOwnerSymbol)

    override fun visitDestructuringDeclaration(multiDeclaration: KtDestructuringDeclaration, data: Nothing?): IrStatement {
        val irBlock = IrCompositeImpl(multiDeclaration.startOffset, multiDeclaration.endOffset,
                                      context.builtIns.unitType, IrStatementOrigin.DESTRUCTURING_DECLARATION)
        val ktInitializer = multiDeclaration.initializer!!
        val containerValue = scope.createTemporaryVariableInBlock(ktInitializer.genExpr(), irBlock, "container")

        declareComponentVariablesInBlock(multiDeclaration, irBlock, containerValue)

        return irBlock
    }

    fun declareComponentVariablesInBlock(multiDeclaration: KtDestructuringDeclaration, irBlock: IrStatementContainer, containerValue: IntermediateValue) {
        val callGenerator = CallGenerator(this)
        for ((index, ktEntry) in multiDeclaration.entries.withIndex()) {
            val componentResolvedCall = getOrFail(BindingContext.COMPONENT_RESOLVED_CALL, ktEntry)

            val componentSubstitutedCall = pregenerateCall(componentResolvedCall)
            componentSubstitutedCall.setExplicitReceiverValue(containerValue)

            val componentVariable = getOrFail(BindingContext.VARIABLE, ktEntry)

            // componentN for '_' SHOULD NOT be evaluated
            if (componentVariable.name.isSpecial) continue

            val irComponentCall = callGenerator.generateCall(ktEntry.startOffset, ktEntry.endOffset, componentSubstitutedCall,
                                                             IrStatementOrigin.COMPONENT_N.withIndex(index + 1))
            val irComponentVar = context.symbolTable.declareVariable(
                    ktEntry.startOffset, ktEntry.endOffset, IrDeclarationOrigin.DEFINED,
                    componentVariable, irComponentCall
            )
            irBlock.statements.add(irComponentVar)
        }
    }

    override fun visitBlockExpression(expression: KtBlockExpression, data: Nothing?): IrStatement {
        val isBlockBody = expression.parent is KtDeclarationWithBody && expression.parent !is KtFunctionLiteral
        if (isBlockBody) throw AssertionError("Use IrBlockBody and corresponding body generator to generate blocks as function bodies")

        val returnType = getInferredTypeWithImplicitCasts(expression) ?: context.builtIns.unitType
        val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, returnType)

        expression.statements.forEach {
            irBlock.statements.add(it.genStmt())
        }

        return irBlock
    }

    override fun visitReturnExpression(expression: KtReturnExpression, data: Nothing?): IrStatement {
        val returnTarget = getReturnExpressionTarget(expression)
        val irReturnedExpression = expression.returnedExpression?.genExpr() ?:
                                   IrGetObjectValueImpl(expression.startOffset, expression.endOffset, context.builtIns.unitType,
                                                        context.symbolTable.referenceClass(context.builtIns.unit))
        return IrReturnImpl(expression.startOffset, expression.endOffset, context.builtIns.nothingType,
                            context.symbolTable.referenceFunction(returnTarget), irReturnedExpression)
    }

    private fun scopeOwnerAsCallable() =
            (scopeOwner as? CallableDescriptor) ?: throw AssertionError("'return' in a non-callable: $scopeOwner")

    private fun getReturnExpressionTarget(expression: KtReturnExpression): CallableDescriptor =
            if (!ExpressionTypingUtils.isFunctionLiteral(scopeOwner) && !ExpressionTypingUtils.isFunctionExpression(scopeOwner)) {
                scopeOwnerAsCallable()
            }
            else {
                val label = expression.getTargetLabel()
                when {
                    label != null -> {
                        val labelTarget = getOrFail(BindingContext.LABEL_TARGET, label)
                        val labelTargetDescriptor = getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, labelTarget)
                        labelTargetDescriptor as CallableDescriptor
                    }
                    ExpressionTypingUtils.isFunctionLiteral(scopeOwner) -> {
                        BindingContextUtils.getContainingFunctionSkipFunctionLiterals(scopeOwner, true).first
                    }
                    else -> {
                        scopeOwnerAsCallable()
                    }
                }
            }

    override fun visitThrowExpression(expression: KtThrowExpression, data: Nothing?): IrStatement {
        return IrThrowImpl(expression.startOffset, expression.endOffset, context.builtIns.nothingType, expression.thrownExpression!!.genExpr())
    }

    override fun visitConstantExpression(expression: KtConstantExpression, data: Nothing?): IrExpression =
            generateConstantExpression(expression,
                                       ConstantExpressionEvaluator.getConstant(expression, context.bindingContext) ?:
                                       error("KtConstantExpression was not evaluated: ${expression.text}"))

    fun generateConstantExpression(expression: KtExpression, constant: CompileTimeConstant<*>): IrExpression {
        val constantValue = constant.toConstantValue(getInferredTypeWithImplicitCastsOrFail(expression))
        val constantType = constantValue.type

        return when (constantValue) {
            is StringValue ->
                IrConstImpl.string(expression.startOffset, expression.endOffset, constantType, constantValue.value)
            is IntValue ->
                IrConstImpl.int(expression.startOffset, expression.endOffset, constantType, constantValue.value)
            is NullValue ->
                IrConstImpl.constNull(expression.startOffset, expression.endOffset, constantType)
            is BooleanValue ->
                IrConstImpl.boolean(expression.startOffset, expression.endOffset, constantType, constantValue.value)
            is LongValue ->
                IrConstImpl.long(expression.startOffset, expression.endOffset, constantType, constantValue.value)
            is DoubleValue ->
                IrConstImpl.double(expression.startOffset, expression.endOffset, constantType, constantValue.value)
            is FloatValue ->
                IrConstImpl.float(expression.startOffset, expression.endOffset, constantType, constantValue.value)
            is CharValue ->
                IrConstImpl.char(expression.startOffset, expression.endOffset, constantType, constantValue.value)
            is ByteValue ->
                IrConstImpl.byte(expression.startOffset, expression.endOffset, constantType, constantValue.value)
            is ShortValue ->
                IrConstImpl.short(expression.startOffset, expression.endOffset, constantType, constantValue.value)
            else ->
                TODO("handle other literal types: ${constantValue.type}")
        }
    }

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, data: Nothing?): IrStatement {
        val entries = expression.entries
        val resultType = getInferredTypeWithImplicitCastsOrFail(expression)
        return when (entries.size) {
            1 -> {
                val irArg = entries[0].genExpr()
                if (irArg is IrConst<*> && irArg.kind == IrConstKind.String)
                    irArg
                else
                    IrStringConcatenationImpl(expression.startOffset, expression.endOffset, resultType, listOf(irArg))
            }
            0 ->
                IrConstImpl.string(expression.startOffset, expression.endOffset, resultType, "")
            else ->
                IrStringConcatenationImpl(expression.startOffset, expression.endOffset, resultType, entries.map { it.genExpr() })
        }
    }

    override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry, data: Nothing?): IrStatement =
            IrConstImpl.string(entry.startOffset, entry.endOffset, context.builtIns.stringType, entry.text)

    override fun visitEscapeStringTemplateEntry(entry: KtEscapeStringTemplateEntry, data: Nothing?): IrStatement =
            IrConstImpl.string(entry.startOffset, entry.endOffset, context.builtIns.stringType, entry.unescapedValue)

    override fun visitStringTemplateEntryWithExpression(entry: KtStringTemplateEntryWithExpression, data: Nothing?): IrStatement =
            entry.expression!!.genExpr()

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Nothing?): IrExpression {
        val resolvedCall = getResolvedCall(expression)

        if (resolvedCall != null) {
            if (resolvedCall is VariableAsFunctionResolvedCall) {
                val variableCall = pregenerateCall(resolvedCall.variableCall)
                return CallGenerator(this).generateCall(expression, variableCall, IrStatementOrigin.VARIABLE_AS_FUNCTION)
            }

            val descriptor = resolvedCall.resultingDescriptor

            return generateExpressionForReferencedDescriptor(descriptor, expression, resolvedCall)
        }

        val referenceTarget = get(BindingContext.REFERENCE_TARGET, expression)
        if (referenceTarget != null) {
            return generateExpressionForReferencedDescriptor(referenceTarget, expression, null)
        }

        return ErrorExpressionGenerator(this).generateErrorSimpleName(expression)
    }

    private fun generateExpressionForReferencedDescriptor(
            descriptor: DeclarationDescriptor,
            expression: KtExpression,
            resolvedCall: ResolvedCall<*>?
    ): IrExpression =
            CallGenerator(this).generateValueReference(
                    expression.startOffset, expression.endOffset,
                    descriptor, resolvedCall, null
            )

    override fun visitCallExpression(expression: KtCallExpression, data: Nothing?): IrStatement {
        val resolvedCall = getResolvedCall(expression) ?:
                           return ErrorExpressionGenerator(this).generateErrorCall(expression)

        if (resolvedCall is VariableAsFunctionResolvedCall) {
            val functionCall = pregenerateCall(resolvedCall.functionCall)
            return CallGenerator(this).generateCall(expression, functionCall, IrStatementOrigin.INVOKE)
        }

        return CallGenerator(this).generateCall(expression.startOffset, expression.endOffset, pregenerateCall(resolvedCall))
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression, data: Nothing?): IrStatement {
        val indexedGetCall = getOrFail(BindingContext.INDEXED_LVALUE_GET, expression)

        return CallGenerator(this).generateCall(expression.startOffset, expression.endOffset,
                                                pregenerateCall(indexedGetCall), IrStatementOrigin.GET_ARRAY_ELEMENT)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression, data: Nothing?): IrStatement =
            expression.selectorExpression!!.accept(this, data)

    override fun visitSafeQualifiedExpression(expression: KtSafeQualifiedExpression, data: Nothing?): IrStatement =
            expression.selectorExpression!!.accept(this, data)

    override fun visitThisExpression(expression: KtThisExpression, data: Nothing?): IrExpression {
        val referenceTarget = getOrFail(BindingContext.REFERENCE_TARGET, expression.instanceReference) { "No reference target for this" }
        return when (referenceTarget) {
            is ClassDescriptor ->
                IrGetValueImpl(expression.startOffset, expression.endOffset,
                               context.symbolTable.referenceValueParameter(referenceTarget.thisAsReceiverParameter))
            is CallableDescriptor -> {
                val extensionReceiver = referenceTarget.extensionReceiverParameter ?: TODO("No extension receiver: $referenceTarget")
                IrGetValueImpl(expression.startOffset, expression.endOffset,
                               context.symbolTable.referenceValueParameter(extensionReceiver))
            }
            else ->
                error("Expected this or receiver: $referenceTarget")
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, data: Nothing?): IrStatement =
            OperatorExpressionGenerator(this).generateBinaryExpression(expression)

    override fun visitPrefixExpression(expression: KtPrefixExpression, data: Nothing?): IrStatement =
            OperatorExpressionGenerator(this).generatePrefixExpression(expression)

    override fun visitPostfixExpression(expression: KtPostfixExpression, data: Nothing?): IrStatement =
            OperatorExpressionGenerator(this).generatePostfixExpression(expression)

    override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS, data: Nothing?): IrStatement =
            OperatorExpressionGenerator(this).generateCastExpression(expression)

    override fun visitIsExpression(expression: KtIsExpression, data: Nothing?): IrStatement =
            OperatorExpressionGenerator(this).generateInstanceOfExpression(expression)

    override fun visitIfExpression(expression: KtIfExpression, data: Nothing?): IrStatement =
            BranchingExpressionGenerator(this).generateIfExpression(expression)

    override fun visitWhenExpression(expression: KtWhenExpression, data: Nothing?): IrStatement =
            BranchingExpressionGenerator(this).generateWhenExpression(expression)

    override fun visitWhileExpression(expression: KtWhileExpression, data: Nothing?): IrStatement =
            LoopExpressionGenerator(this).generateWhileLoop(expression)

    override fun visitDoWhileExpression(expression: KtDoWhileExpression, data: Nothing?): IrStatement =
            LoopExpressionGenerator(this).generateDoWhileLoop(expression)

    override fun visitForExpression(expression: KtForExpression, data: Nothing?): IrStatement =
            LoopExpressionGenerator(this).generateForLoop(expression)

    override fun visitBreakExpression(expression: KtBreakExpression, data: Nothing?): IrStatement =
            LoopExpressionGenerator(this).generateBreak(expression)

    override fun visitContinueExpression(expression: KtContinueExpression, data: Nothing?): IrStatement =
            LoopExpressionGenerator(this).generateContinue(expression)

    override fun visitTryExpression(expression: KtTryExpression, data: Nothing?): IrStatement =
            TryCatchExpressionGenerator(this).generateTryCatch(expression)

    override fun visitLambdaExpression(expression: KtLambdaExpression, data: Nothing?): IrStatement =
            LocalFunctionGenerator(this).generateLambda(expression)

    override fun visitNamedFunction(function: KtNamedFunction, data: Nothing?): IrStatement =
            LocalFunctionGenerator(this).generateFunction(function)

    override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression, data: Nothing?): IrStatement =
            LocalClassGenerator(this).generateObjectLiteral(expression)

    override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Nothing?): IrStatement =
            LocalClassGenerator(this).generateLocalClass(classOrObject)

    override fun visitTypeAlias(typeAlias: KtTypeAlias, data: Nothing?): IrStatement =
            IrTypeAliasImpl(typeAlias.startOffset, typeAlias.endOffset, IrDeclarationOrigin.DEFINED,
                            getOrFail(BindingContext.TYPE_ALIAS, typeAlias))

    override fun visitClassLiteralExpression(expression: KtClassLiteralExpression, data: Nothing?): IrStatement =
            ReflectionReferencesGenerator(this).generateClassLiteral(expression)

    override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression, data: Nothing?): IrStatement =
            ReflectionReferencesGenerator(this).generateCallableReference(expression)
}

abstract class StatementGeneratorExtension(val statementGenerator: StatementGenerator) : GeneratorWithScope {
    override val scope: Scope get() = statementGenerator.scope
    override val context: GeneratorContext get() = statementGenerator.context
}