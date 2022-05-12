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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.BackendException
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.psi2ir.intermediate.IntermediateValue
import org.jetbrains.kotlin.psi2ir.intermediate.VariableLValue
import org.jetbrains.kotlin.psi2ir.intermediate.declareTemporaryVariableInBlock
import org.jetbrains.kotlin.psi2ir.intermediate.setExplicitReceiverValue
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.SMARTCAST
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.scopes.receivers.ContextClassReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.util.OperatorNameConventions

class StatementGenerator(
    val bodyGenerator: BodyGenerator,
    override val scope: Scope
) : KtVisitor<IrStatement, Nothing?>(),
    GeneratorWithScope {

    override val context: GeneratorContext get() = bodyGenerator.context

    val scopeOwner: DeclarationDescriptor get() = bodyGenerator.scopeOwner

    private val typeTranslator = context.typeTranslator

    fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun generateStatement(ktElement: KtElement): IrStatement =
        try {
            ktElement.deparenthesize().accept(this@StatementGenerator, null)
        } catch (e: BackendException) {
            throw e
        } catch (e: ErrorExpressionException) {
            throw e
        } catch (e: Throwable) {
            ErrorExpressionGenerator(this@StatementGenerator).generateErrorExpression(ktElement, e)
        }

    fun generateStatements(ktStatements: List<KtExpression>, to: IrStatementContainer) =
        ktStatements.mapTo(to.statements) { generateStatement(it) }

    fun generateExpression(ktElement: KtElement): IrExpression =
        when (val irStatement = generateStatement(ktElement)) {
            is IrExpression ->
                irStatement
            is IrDeclaration ->
                IrBlockImpl(
                    irStatement.startOffset,
                    irStatement.endOffset,
                    context.irBuiltIns.unitType,
                    null,
                    listOf(irStatement)
                )
            else ->
                throw AssertionError("Unexpected statement: ${irStatement.render()}")
        }

    override fun visitExpression(expression: KtExpression, data: Nothing?): IrStatement =
        IrErrorExpressionImpl(
            expression.startOffsetSkippingComments,
            expression.endOffset,
            context.irBuiltIns.nothingType,
            expression::class.java.simpleName
        )

    override fun visitProperty(property: KtProperty, data: Nothing?): IrStatement {
        val variableDescriptor = getOrFail(BindingContext.VARIABLE, property)

        property.delegate?.let { ktDelegate ->
            return generateLocalDelegatedProperty(
                property, ktDelegate, variableDescriptor as VariableDescriptorWithAccessors,
                bodyGenerator.scopeOwnerSymbol
            )
        }

        val sourceElement =
            if (context.extensions.debugInfoOnlyOnVariablesInDestructuringDeclarations) {
                property.nameIdentifier ?: property
            } else {
                property
            }
        return context.symbolTable.declareVariable(
            sourceElement.startOffsetSkippingComments, sourceElement.endOffset, IrDeclarationOrigin.DEFINED,
            variableDescriptor,
            variableDescriptor.type.toIrType(),
            property.initializer?.let { generateExpression(it) }
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
        val (blockStartOffset, blockEndOffset) = if (context.extensions.debugInfoOnlyOnVariablesInDestructuringDeclarations) {
            SYNTHETIC_OFFSET to SYNTHETIC_OFFSET
        } else {
            multiDeclaration.startOffsetSkippingComments to multiDeclaration.endOffset
        }
        val irBlock = IrCompositeImpl(
            blockStartOffset, blockEndOffset,
            context.irBuiltIns.unitType, IrStatementOrigin.DESTRUCTURING_DECLARATION
        )
        val ktInitializer = multiDeclaration.initializer!!
        val irInitializer = generateExpression(ktInitializer)

        val containerVariable = scope.declareTemporaryVariableInBlock(irInitializer, irBlock, nameHint = "container")

        val firstContainerValue = VariableLValue(context, containerVariable)
        declareComponentVariablesInBlock(
            multiDeclaration,
            irBlock,
            firstContainerValue,
            if (context.extensions.debugInfoOnlyOnVariablesInDestructuringDeclarations) {
                VariableLValue(context, containerVariable, startOffset = SYNTHETIC_OFFSET, endOffset = SYNTHETIC_OFFSET)
            } else {
                firstContainerValue
            }
        )

        return irBlock
    }

    fun declareComponentVariablesInBlock(
        multiDeclaration: KtDestructuringDeclaration,
        irBlock: IrStatementContainer,
        firstContainerValue: IntermediateValue,
        restContainerValue: IntermediateValue
    ) {
        val callGenerator = CallGenerator(this)

        // TODO: Every access to the container value causes a null check even though subsequent checks after the first can be assumed to pass.
        var containerValue = firstContainerValue
        for ((index, ktEntry) in multiDeclaration.entries.withIndex()) {
            val componentVariable = getOrFail(BindingContext.VARIABLE, ktEntry)

            // componentN for '_' SHOULD NOT be evaluated
            if (componentVariable.name.isSpecial) continue

            val componentResolvedCall = getOrFail(BindingContext.COMPONENT_RESOLVED_CALL, ktEntry)
            val componentSubstitutedCall = pregenerateCall(componentResolvedCall)

            componentSubstitutedCall.setExplicitReceiverValue(containerValue)

            containerValue = restContainerValue

            val (componentCallStartOffset, componentCallEndOffset) =
                if (context.extensions.debugInfoOnlyOnVariablesInDestructuringDeclarations) {
                    SYNTHETIC_OFFSET to SYNTHETIC_OFFSET
                } else {
                    ktEntry.startOffsetSkippingComments to ktEntry.endOffset
                }
            val irComponentCall = callGenerator.generateCall(
                componentCallStartOffset, componentCallEndOffset,
                componentSubstitutedCall,
                IrStatementOrigin.COMPONENT_N.withIndex(index + 1)
            )

            val componentVarOffsetSource: PsiElement =
                if (context.extensions.debugInfoOnlyOnVariablesInDestructuringDeclarations) {
                    ktEntry.nameIdentifier ?: ktEntry
                } else {
                    ktEntry
                }
            val irComponentVar = context.symbolTable.declareVariable(
                componentVarOffsetSource.startOffsetSkippingComments, componentVarOffsetSource.endOffset,
                IrDeclarationOrigin.DEFINED,
                componentVariable, componentVariable.type.toIrType(), irComponentCall
            )
            irBlock.statements.add(irComponentVar)
        }
    }

    override fun visitBlockExpression(expression: KtBlockExpression, data: Nothing?): IrStatement {
        val isBlockBody = expression.parent is KtDeclarationWithBody && expression.parent !is KtFunctionLiteral
        if (isBlockBody) throw AssertionError("Use IrBlockBody and corresponding body generator to generate blocks as function bodies")

        val returnType = getExpressionTypeWithCoercionToUnitOrFail(expression)
        val irBlock = IrBlockImpl(expression.startOffsetSkippingComments, expression.endOffset, returnType.toIrType())

        expression.statements.forEach {
            irBlock.statements.add(generateStatement(it))
        }

        return irBlock
    }

    override fun visitReturnExpression(expression: KtReturnExpression, data: Nothing?): IrStatement {
        val returnTarget = getReturnExpressionTarget(expression)
        val irReturnedExpression = expression.returnedExpression?.let { generateExpression(it) }
            ?: IrGetObjectValueImpl(
                expression.startOffsetSkippingComments, expression.endOffset, context.irBuiltIns.unitType,
                context.irBuiltIns.unitClass
            )
        return IrReturnImpl(
            expression.startOffsetSkippingComments, expression.endOffset, context.irBuiltIns.nothingType,
            context.symbolTable.referenceFunction(returnTarget), irReturnedExpression
        )
    }

    private fun scopeOwnerAsCallable() =
        (scopeOwner as? CallableDescriptor) ?: throw AssertionError("'return' in a non-callable: $scopeOwner")

    private fun getReturnExpressionTarget(expression: KtReturnExpression): CallableDescriptor =
        if (!ExpressionTypingUtils.isFunctionLiteral(scopeOwner) && !ExpressionTypingUtils.isFunctionExpression(scopeOwner)) {
            scopeOwnerAsCallable()
        } else {
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
        return IrThrowImpl(
            expression.startOffsetSkippingComments,
            expression.endOffset,
            context.irBuiltIns.nothingType,
            generateExpression(expression.thrownExpression!!)
        )
    }

    override fun visitConstantExpression(expression: KtConstantExpression, data: Nothing?): IrExpression =
        generateConstantExpression(
            expression,
            ConstantExpressionEvaluator.getConstant(expression, context.bindingContext)
                ?: error("KtConstantExpression was not evaluated: ${expression.text}")
        )

    fun generateConstantExpression(expression: KtExpression, constant: CompileTimeConstant<*>): IrExpression =
        context.constantValueGenerator.generateConstantValueAsExpression(
            expression.startOffsetSkippingComments,
            expression.endOffset,
            constant.toConstantValue(getTypeInferredByFrontendOrFail(expression))
        )

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, data: Nothing?): IrStatement {
        val startOffset = expression.startOffsetSkippingComments
        val endOffset = expression.endOffset

        val resultType = getTypeInferredByFrontendOrFail(expression).toIrType()
        val entries = expression.entries.map { generateExpression(it) }.postprocessStringTemplateEntries()

        return when (entries.size) {
            0 -> IrConstImpl.string(startOffset, endOffset, resultType, "")

            1 -> {
                val first = entries.first()
                if (first is IrConst<*> && first.kind == IrConstKind.String)
                    first
                else
                    IrStringConcatenationImpl(startOffset, endOffset, resultType, listOf(first))
            }

            else -> IrStringConcatenationImpl(startOffset, endOffset, resultType, entries)
        }
    }

    private fun List<IrExpression>.postprocessStringTemplateEntries(): List<IrExpression> =
        ArrayList<IrExpression>(this.size).also { result ->
            val stringType = context.irBuiltIns.stringType

            val constString = StringBuilder()
            var constStringStartOffset = 0
            var constStringEndOffset = 0

            for (entry in this) {
                if (entry is IrConst<*> && entry.kind == IrConstKind.String) {
                    if (constString.isEmpty()) {
                        constStringStartOffset = entry.startOffset
                    }
                    constString.append(IrConstKind.String.valueOf(entry))
                    constStringEndOffset = entry.endOffset
                } else {
                    if (constString.isNotEmpty()) {
                        result.add(
                            IrConstImpl.string(constStringStartOffset, constStringEndOffset, stringType, constString.toString())
                        )
                        constString.clear()
                    }
                    result.add(entry)
                }
            }

            if (constString.isNotEmpty()) {
                result.add(
                    IrConstImpl.string(constStringStartOffset, constStringEndOffset, stringType, constString.toString())
                )
            }
        }

    override fun visitLiteralStringTemplateEntry(entry: KtLiteralStringTemplateEntry, data: Nothing?): IrStatement =
        IrConstImpl.string(entry.startOffsetSkippingComments, entry.endOffset, context.irBuiltIns.stringType, entry.text)

    override fun visitEscapeStringTemplateEntry(entry: KtEscapeStringTemplateEntry, data: Nothing?): IrStatement =
        IrConstImpl.string(entry.startOffsetSkippingComments, entry.endOffset, context.irBuiltIns.stringType, entry.unescapedValue)

    override fun visitStringTemplateEntryWithExpression(entry: KtStringTemplateEntryWithExpression, data: Nothing?): IrStatement =
        generateExpression(entry.expression!!)

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
            expression.startOffsetSkippingComments, expression.endOffset,
            descriptor, resolvedCall, null,
            context.bindingContext.get(SMARTCAST, expression)?.defaultType?.toIrType()
        )

    override fun visitCallExpression(expression: KtCallExpression, data: Nothing?): IrStatement {
        val resolvedCall = getResolvedCall(expression) ?: return ErrorExpressionGenerator(this).generateErrorCall(expression)

        if (resolvedCall is VariableAsFunctionResolvedCall) {
            val functionCall = pregenerateCall(resolvedCall.functionCall)
            return CallGenerator(this).generateCall(expression, functionCall, IrStatementOrigin.INVOKE)
        }

        val calleeExpression = expression.calleeExpression
        val origin =
            if (resolvedCall.resultingDescriptor.name == OperatorNameConventions.INVOKE &&
                calleeExpression !is KtSimpleNameExpression && calleeExpression !is KtQualifiedExpression
            )
                IrStatementOrigin.INVOKE
            else
                null

        return CallGenerator(this).generateCall(
            expression.startOffsetSkippingComments,
            expression.endOffset,
            pregenerateCall(resolvedCall),
            origin
        )
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression, data: Nothing?): IrStatement {
        val indexedGetCall = getOrFail(BindingContext.INDEXED_LVALUE_GET, expression)

        return if (indexedGetCall.resultingDescriptor.isDynamic())
            OperatorExpressionGenerator(this).generateDynamicArrayAccess(expression)
        else
            CallGenerator(this).generateCall(
                expression.startOffsetSkippingComments, expression.endOffset,
                pregenerateCall(indexedGetCall), IrStatementOrigin.GET_ARRAY_ELEMENT
            )
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression, data: Nothing?): IrStatement =
        expression.selectorExpression!!.accept(this, data)

    override fun visitSafeQualifiedExpression(expression: KtSafeQualifiedExpression, data: Nothing?): IrStatement =
        expression.selectorExpression!!.accept(this, data)

    private fun isThisForClassPhysicallyAvailable(classDescriptor: ClassDescriptor): Boolean {
        var scopeDescriptor: DeclarationDescriptor? = scopeOwner
        while (scopeDescriptor != null) {
            if (scopeDescriptor == classDescriptor) return true
            if (scopeDescriptor is ClassDescriptor && !scopeDescriptor.isInner) return false
            scopeDescriptor = scopeDescriptor.containingDeclaration
        }
        return false
    }

    fun generateThisReceiver(startOffset: Int, endOffset: Int, kotlinType: KotlinType, classDescriptor: ClassDescriptor): IrExpression {
        val thisAsReceiverParameter = classDescriptor.thisAsReceiverParameter
        val thisType = kotlinType.toIrType()

        return if (DescriptorUtils.isObject(classDescriptor) && !isThisForClassPhysicallyAvailable(classDescriptor)) {
            IrGetObjectValueImpl(
                startOffset, endOffset,
                thisType,
                context.symbolTable.referenceClass(classDescriptor)
            )
        } else {
            IrGetValueImpl(
                startOffset, endOffset,
                thisType,
                context.symbolTable.referenceValueParameter(thisAsReceiverParameter)
            )
        }
    }

    override fun visitThisExpression(expression: KtThisExpression, data: Nothing?): IrExpression {
        val referenceTarget = getOrFail(BindingContext.REFERENCE_TARGET, expression.instanceReference) { "No reference target for this" }
        val receiverParameter =
            getOrFail<KtReferenceExpression, ReceiverParameterDescriptor>(
                BindingContext.THIS_REFERENCE_TARGET, expression.instanceReference
            ) { "No reference target for this" }
        val startOffset = expression.startOffsetSkippingComments
        val endOffset = expression.endOffset
        return when (referenceTarget) {
            is ClassDescriptor ->
                when (receiverParameter.value) {
                    is ContextClassReceiver -> loadContextReceiver(receiverParameter.value as ContextClassReceiver, startOffset, endOffset)
                    else -> generateThisReceiver(
                        startOffset, endOffset, referenceTarget.thisAsReceiverParameter.type, referenceTarget
                    )
                }
            is CallableDescriptor -> {
                val receiverType = receiverParameter.type.toIrType()
                IrGetValueImpl(
                    startOffset, endOffset,
                    receiverType,
                    context.symbolTable.referenceValueParameter(receiverParameter)
                )
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
        IrBlockImpl(
            // Compile local type aliases to empty blocks
            typeAlias.startOffsetSkippingComments, typeAlias.endOffset,
            context.irBuiltIns.unitType
        )

    override fun visitClassLiteralExpression(expression: KtClassLiteralExpression, data: Nothing?): IrStatement =
        ReflectionReferencesGenerator(this).generateClassLiteral(expression)

    override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression, data: Nothing?): IrStatement =
        ReflectionReferencesGenerator(this).generateCallableReference(expression)
}

abstract class StatementGeneratorExtension(val statementGenerator: StatementGenerator) : GeneratorWithScope {
    override val scope: Scope get() = statementGenerator.scope
    override val context: GeneratorContext get() = statementGenerator.context

    fun KtExpression.genExpr() = statementGenerator.generateExpression(this)
    fun KtExpression.genStmt() = statementGenerator.generateStatement(this)
    fun KotlinType.toIrType() = with(statementGenerator) { toIrType() }
    fun translateType(kotlinType: KotlinType) = kotlinType.toIrType()
}
