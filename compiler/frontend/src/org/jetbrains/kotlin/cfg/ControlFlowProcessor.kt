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

package org.jetbrains.kotlin.cfg

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartFMap
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg.ControlFlowBuilder.PredefinedOperation.*
import org.jetbrains.kotlin.cfg.pseudocode.ControlFlowInstructionsGenerator
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeImpl
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.AccessTarget
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.InstructionWithValue
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.contracts.description.canBeRevisited
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.getFakeDescriptorForObject
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import java.util.*

typealias DeferredGenerator = (ControlFlowBuilder) -> Unit

class ControlFlowProcessor(
    private val trace: BindingTrace,
    private val languageVersionSettings: LanguageVersionSettings?
) {

    private val builder: ControlFlowBuilder = ControlFlowInstructionsGenerator()

    fun generatePseudocode(subroutine: KtElement): Pseudocode {
        val pseudocode = generate(subroutine, null)
        (pseudocode as PseudocodeImpl).postProcess()
        return pseudocode
    }

    private fun generate(subroutine: KtElement, invocationKind: InvocationKind? = null): Pseudocode {
        builder.enterSubroutine(subroutine, invocationKind)
        val cfpVisitor = CFPVisitor(builder)
        if (subroutine is KtDeclarationWithBody && subroutine !is KtSecondaryConstructor) {
            val valueParameters = subroutine.valueParameters
            for (valueParameter in valueParameters) {
                cfpVisitor.generateInstructions(valueParameter)
            }
            val bodyExpression = subroutine.bodyExpression
            if (bodyExpression != null) {
                cfpVisitor.generateInstructions(bodyExpression)
                if (!subroutine.hasBlockBody()) {
                    generateImplicitReturnValue(bodyExpression, subroutine)
                }
            }
        } else {
            cfpVisitor.generateInstructions(subroutine)
        }
        return builder.exitSubroutine(subroutine, invocationKind)
    }

    private fun generateImplicitReturnValue(bodyExpression: KtExpression, subroutine: KtElement) {
        val subroutineDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, subroutine) as CallableDescriptor? ?: return

        val returnType = subroutineDescriptor.returnType
        if (returnType != null && KotlinBuiltIns.isUnit(returnType) && subroutineDescriptor is AnonymousFunctionDescriptor) return

        val returnValue = builder.getBoundValue(bodyExpression) ?: return

        builder.returnValue(bodyExpression, returnValue, subroutine)
    }

    private fun processLocalDeclaration(subroutine: KtDeclaration) {
        val afterDeclaration = builder.createUnboundLabel("after local declaration")

        builder.nondeterministicJump(afterDeclaration, subroutine, null)
        generate(subroutine, null)
        builder.bindLabel(afterDeclaration)
    }

    private class CatchFinallyLabels(val onException: Label?, val toFinally: Label?, val tryExpression: KtTryExpression?)

    private inner class CFPVisitor(private val builder: ControlFlowBuilder) : KtVisitorVoid() {

        private val catchFinallyStack = Stack<CatchFinallyLabels>()

        // Some language constructs (e.g. inlined lambdas) should be partially processed before call
        // (to provide argument for call itself), and partially - after (in case of inlined lambdas,
        // their body should be generated after call). To do so, we store deferred generators, which
        // will be called after call instruction is emitted.
        // Stack is necessary to store generators across nested calls
        private val deferredGeneratorsStack = Stack<MutableList<DeferredGenerator>>()

        private val conditionVisitor = object : KtVisitorVoid() {

            private fun getSubjectExpression(condition: KtWhenCondition): KtExpression? =
                condition.getStrictParentOfType<KtWhenExpression>()?.subjectExpression

            override fun visitWhenConditionInRange(condition: KtWhenConditionInRange) {
                if (!generateCall(condition.operationReference)) {
                    val rangeExpression = condition.rangeExpression
                    generateInstructions(rangeExpression)
                    createNonSyntheticValue(condition, MagicKind.UNRESOLVED_CALL, rangeExpression)
                }
            }

            override fun visitWhenConditionIsPattern(condition: KtWhenConditionIsPattern) {
                mark(condition)
                createNonSyntheticValue(condition, MagicKind.IS, getSubjectExpression(condition))
            }

            override fun visitWhenConditionWithExpression(condition: KtWhenConditionWithExpression) {
                mark(condition)

                val expression = condition.expression
                generateInstructions(expression)

                val subjectExpression = getSubjectExpression(condition)
                if (subjectExpression != null) {
                    // todo: this can be replaced by equals() invocation (when corresponding resolved call is recorded)
                    createNonSyntheticValue(condition, MagicKind.EQUALS_IN_WHEN_CONDITION, subjectExpression, expression)
                } else {
                    copyValue(expression, condition)
                }
            }

            override fun visitKtElement(element: KtElement) {
                throw UnsupportedOperationException("[ControlFlowProcessor] " + element.toString())
            }
        }

        private fun mark(element: KtElement) {
            builder.mark(element)
        }

        fun generateInstructions(element: KtElement?) {
            if (element == null) return
            element.accept(this)
            checkNothingType(element)
        }

        private fun checkNothingType(element: KtElement) {
            if (element !is KtExpression) return

            val expression = KtPsiUtil.deparenthesize(element) ?: return

            if (expression is KtStatementExpression || expression is KtTryExpression
                || expression is KtIfExpression || expression is KtWhenExpression
            ) {
                return
            }

            val type = trace.bindingContext.getType(expression)
            if (type != null && KotlinBuiltIns.isNothing(type)) {
                builder.jumpToError(expression)
            }
        }

        private fun createSyntheticValue(instructionElement: KtElement, kind: MagicKind, vararg from: KtElement): PseudoValue =
            builder.magic(instructionElement, null, elementsToValues(from.asList()), kind).outputValue

        private fun createNonSyntheticValue(to: KtElement, from: List<KtElement?>, kind: MagicKind): PseudoValue =
            builder.magic(to, to, elementsToValues(from), kind).outputValue

        private fun createNonSyntheticValue(to: KtElement, kind: MagicKind, vararg from: KtElement?): PseudoValue =
            createNonSyntheticValue(to, from.asList(), kind)

        private fun mergeValues(from: List<KtExpression>, to: KtExpression) {
            builder.merge(to, elementsToValues(from))
        }

        private fun copyValue(from: KtElement?, to: KtElement) {
            getBoundOrUnreachableValue(from)?.let { builder.bindValue(it, to) }
        }

        private fun getBoundOrUnreachableValue(element: KtElement?): PseudoValue? {
            if (element == null) return null

            val value = builder.getBoundValue(element)
            return if (value != null || element is KtDeclaration) value else builder.newValue(element)
        }

        private fun elementsToValues(from: List<KtElement?>): List<PseudoValue> =
            from.mapNotNull { element -> getBoundOrUnreachableValue(element) }

        private fun generateInitializer(declaration: KtDeclaration, initValue: PseudoValue) {
            builder.write(declaration, declaration, initValue, getDeclarationAccessTarget(declaration), emptyMap())
        }

        private fun getResolvedCallAccessTarget(element: KtElement?): AccessTarget =
            element.getResolvedCall(trace.bindingContext)?.let { AccessTarget.Call(it) }
                ?: AccessTarget.BlackBox

        private fun getDeclarationAccessTarget(element: KtElement): AccessTarget {
            val descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
            return if (descriptor is VariableDescriptor)
                AccessTarget.Declaration(descriptor)
            else
                AccessTarget.BlackBox
        }

        override fun visitParenthesizedExpression(expression: KtParenthesizedExpression) {
            mark(expression)
            val innerExpression = expression.expression
            if (innerExpression != null) {
                generateInstructions(innerExpression)
                copyValue(innerExpression, expression)
            }
        }

        override fun visitAnnotatedExpression(expression: KtAnnotatedExpression) {
            val baseExpression = expression.baseExpression
            if (baseExpression != null) {
                generateInstructions(baseExpression)
                copyValue(baseExpression, expression)
            }
        }

        override fun visitThisExpression(expression: KtThisExpression) {
            val resolvedCall = expression.getResolvedCall(trace.bindingContext)
            if (resolvedCall == null) {
                createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL)
                return
            }

            val resultingDescriptor = resolvedCall.resultingDescriptor
            if (resultingDescriptor is ReceiverParameterDescriptor) {
                builder.readVariable(expression, resolvedCall, getReceiverValues(resolvedCall))
            }

            copyValue(expression, expression.instanceReference)
        }

        override fun visitConstantExpression(expression: KtConstantExpression) {
            val constant = ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)
            builder.loadConstant(expression, constant)
        }

        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
            val resolvedCall = expression.getResolvedCall(trace.bindingContext)
            if (resolvedCall is VariableAsFunctionResolvedCall) {
                generateCall(resolvedCall.variableCall)
            } else {
                if (resolvedCall == null) {
                    val qualifier = trace.bindingContext[BindingContext.QUALIFIER, expression]
                    if (qualifier != null && generateQualifier(expression, qualifier)) return
                }
                if (!generateCall(expression) && expression.parent !is KtCallExpression) {
                    createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL, generateAndGetReceiverIfAny(expression))
                }
            }
        }

        override fun visitLabeledExpression(expression: KtLabeledExpression) {
            mark(expression)
            val baseExpression = expression.baseExpression
            if (baseExpression != null) {
                generateInstructions(baseExpression)
                copyValue(baseExpression, expression)
            }

            val labelNameExpression = expression.getTargetLabel()
            if (labelNameExpression != null) {
                val deparenthesizedBaseExpression = KtPsiUtil.deparenthesize(expression)
                if (deparenthesizedBaseExpression !is KtLambdaExpression &&
                    deparenthesizedBaseExpression !is KtLoopExpression &&
                    deparenthesizedBaseExpression !is KtNamedFunction
                ) {
                    trace.report(Errors.REDUNDANT_LABEL_WARNING.on(labelNameExpression))
                }
            }
        }

        override fun visitBinaryExpression(expression: KtBinaryExpression) {
            val operationReference = expression.operationReference
            val operationType = operationReference.getReferencedNameElementType()

            val left = expression.left
            val right = expression.right
            if (operationType === ANDAND || operationType === OROR) {
                generateBooleanOperation(expression)
            } else if (operationType === EQ) {
                visitAssignment(left, getDeferredValue(right), expression)
            } else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
                val resolvedCall = expression.getResolvedCall(trace.bindingContext)
                if (resolvedCall != null) {
                    val rhsValue = generateCall(resolvedCall).outputValue
                    val assignMethodName = OperatorConventions.getNameForOperationSymbol(expression.operationToken as KtToken)
                    if (resolvedCall.resultingDescriptor.name != assignMethodName) {
                        /* At this point assignment of the form a += b actually means a = a + b
                         * So we first generate call of "+" operation and then use its output pseudo-value
                         * as a right-hand side when generating assignment call
                         */
                        visitAssignment(left, getValueAsFunction(rhsValue), expression)
                    }
                } else {
                    generateBothArgumentsAndMark(expression)
                }
            } else if (operationType === ELVIS) {
                generateInstructions(left)
                mark(expression)
                val afterElvis = builder.createUnboundLabel("after elvis operator")
                builder.jumpOnTrue(afterElvis, expression, builder.getBoundValue(left))
                generateInstructions(right)
                builder.bindLabel(afterElvis)
                mergeValues(listOf(left, right).filterNotNull(), expression)
            } else {
                if (!generateCall(expression)) {
                    generateBothArgumentsAndMark(expression)
                }
            }
        }

        private fun generateBooleanOperation(expression: KtBinaryExpression) {
            val operationType = expression.operationReference.getReferencedNameElementType()
            val left = expression.left
            val right = expression.right

            val resultLabel = builder.createUnboundLabel("result of boolean operation")
            generateInstructions(left)
            if (operationType === ANDAND) {
                builder.jumpOnFalse(resultLabel, expression, builder.getBoundValue(left))
            } else {
                builder.jumpOnTrue(resultLabel, expression, builder.getBoundValue(left))
            }
            generateInstructions(right)
            builder.bindLabel(resultLabel)
            val operation = if (operationType === ANDAND) AND else OR
            builder.predefinedOperation(expression, operation, elementsToValues(listOf(left, right).filterNotNull()))
        }

        private fun getValueAsFunction(value: PseudoValue?) = { value }

        private fun getDeferredValue(expression: KtExpression?) = {
            generateInstructions(expression)
            getBoundOrUnreachableValue(expression)
        }

        private fun generateBothArgumentsAndMark(expression: KtBinaryExpression) {
            val left = KtPsiUtil.deparenthesize(expression.left)
            if (left != null) {
                generateInstructions(left)
            }
            val right = expression.right
            if (right != null) {
                generateInstructions(right)
            }
            mark(expression)
            createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL, left, right)
        }

        private fun visitAssignment(
            lhs: KtExpression?,
            rhsDeferredValue: () -> PseudoValue?,
            parentExpression: KtExpression
        ) {
            val left = KtPsiUtil.deparenthesize(lhs)
            if (left == null) {
                val arguments = rhsDeferredValue()?.let { listOf(it) } ?: emptyList()
                builder.magic(parentExpression, parentExpression, arguments, MagicKind.UNSUPPORTED_ELEMENT)
                return
            }

            if (left is KtArrayAccessExpression) {
                generateArrayAssignment(left, rhsDeferredValue, parentExpression)
                return
            }

            var receiverValues: Map<PseudoValue, ReceiverValue> = SmartFMap.emptyMap<PseudoValue, ReceiverValue>()
            var accessTarget: AccessTarget = AccessTarget.BlackBox
            if (left is KtSimpleNameExpression || left is KtQualifiedExpression) {
                accessTarget = getResolvedCallAccessTarget(left.getQualifiedElementSelector())
                if (accessTarget is AccessTarget.Call) {
                    receiverValues = getReceiverValues(accessTarget.resolvedCall)
                }
            } else if (left is KtProperty) {
                accessTarget = getDeclarationAccessTarget(left)
            }

            if (accessTarget === AccessTarget.BlackBox && left !is KtProperty) {
                generateInstructions(left)
                createSyntheticValue(left, MagicKind.VALUE_CONSUMER, left)
            }

            val rightValue = rhsDeferredValue.invoke()
            val rValue = rightValue ?: createSyntheticValue(parentExpression, MagicKind.UNRECOGNIZED_WRITE_RHS)
            builder.write(parentExpression, left, rValue, accessTarget, receiverValues)
        }

        private fun generateArrayAssignment(
            lhs: KtArrayAccessExpression,
            rhsDeferredValue: () -> PseudoValue?,
            parentExpression: KtExpression
        ) {
            val setResolvedCall = trace.get(BindingContext.INDEXED_LVALUE_SET, lhs)

            if (setResolvedCall == null) {
                generateArrayAccess(lhs, null)

                val arguments = listOf(getBoundOrUnreachableValue(lhs), rhsDeferredValue.invoke()).filterNotNull()
                builder.magic(parentExpression, parentExpression, arguments, MagicKind.UNRESOLVED_CALL)

                return
            }

            // In case of simple ('=') array assignment mark instruction is not generated yet, so we put it before generating "set" call
            if ((parentExpression as KtOperationExpression).operationReference.getReferencedNameElementType() === EQ) {
                mark(lhs)
            }

            generateInstructions(lhs.arrayExpression)

            val receiverValues = getReceiverValues(setResolvedCall)
            val argumentValues = getArraySetterArguments(rhsDeferredValue, setResolvedCall)

            builder.call(parentExpression, setResolvedCall, receiverValues, argumentValues)
        }

        /* We assume that assignment right-hand side corresponds to the last argument of the call
        *  So receiver instructions/pseudo-values are generated for all arguments except the last one which is replaced
        *  by pre-generated pseudo-value
        *  For example, assignment a[1, 2] += 3 means a.set(1, 2, a.get(1) + 3), so in order to generate "set" call
        *  we first generate instructions for 1 and 2 whereas 3 is replaced by pseudo-value corresponding to "a.get(1) + 3"
        */
        private fun getArraySetterArguments(
            rhsDeferredValue: () -> PseudoValue?,
            setResolvedCall: ResolvedCall<FunctionDescriptor>
        ): SmartFMap<PseudoValue, ValueParameterDescriptor> {
            val valueArguments = setResolvedCall.resultingDescriptor.valueParameters.flatMapTo(
                ArrayList<ValueArgument>()
            ) { descriptor -> setResolvedCall.valueArguments[descriptor]?.arguments ?: emptyList() }

            val rhsArgument = valueArguments.lastOrNull()
            var argumentValues = SmartFMap.emptyMap<PseudoValue, ValueParameterDescriptor>()
            for (valueArgument in valueArguments) {
                val argumentMapping = setResolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: continue
                val parameterDescriptor = argumentMapping.valueParameter
                if (valueArgument !== rhsArgument) {
                    argumentValues = generateValueArgument(valueArgument, parameterDescriptor, argumentValues)
                } else {
                    val rhsValue = rhsDeferredValue.invoke()
                    if (rhsValue != null) {
                        argumentValues = argumentValues.plus(rhsValue, parameterDescriptor)
                    }
                }
            }
            return argumentValues
        }

        private fun generateArrayAccess(arrayAccessExpression: KtArrayAccessExpression, resolvedCall: ResolvedCall<*>?) {
            if (builder.getBoundValue(arrayAccessExpression) != null) return
            mark(arrayAccessExpression)
            if (!checkAndGenerateCall(resolvedCall)) {
                generateArrayAccessWithoutCall(arrayAccessExpression)
            }
        }

        private fun generateArrayAccessWithoutCall(arrayAccessExpression: KtArrayAccessExpression) {
            createNonSyntheticValue(arrayAccessExpression, generateArrayAccessArguments(arrayAccessExpression), MagicKind.UNRESOLVED_CALL)
        }

        private fun generateArrayAccessArguments(arrayAccessExpression: KtArrayAccessExpression): List<KtExpression> {
            val inputExpressions = ArrayList<KtExpression>()

            val arrayExpression = arrayAccessExpression.arrayExpression
            if (arrayExpression != null) {
                inputExpressions.add(arrayExpression)
            }
            generateInstructions(arrayExpression)

            for (index in arrayAccessExpression.indexExpressions) {
                generateInstructions(index)
                inputExpressions.add(index)
            }

            return inputExpressions
        }

        override fun visitUnaryExpression(expression: KtUnaryExpression) {
            val operationSign = expression.operationReference
            val operationType = operationSign.getReferencedNameElementType()
            val baseExpression = expression.baseExpression ?: return
            if (KtTokens.EXCLEXCL === operationType) {
                generateInstructions(baseExpression)
                builder.predefinedOperation(expression, NOT_NULL_ASSERTION, elementsToValues(listOf(baseExpression)))
                return
            }

            val incrementOrDecrement = isIncrementOrDecrement(operationType)
            val resolvedCall = expression.getResolvedCall(trace.bindingContext)

            val rhsValue: PseudoValue? = if (resolvedCall != null) {
                generateCall(resolvedCall).outputValue
            } else {
                generateInstructions(baseExpression)
                createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL, baseExpression)
            }

            if (incrementOrDecrement) {
                visitAssignment(baseExpression, getValueAsFunction(rhsValue), expression)
                if (expression is KtPostfixExpression) {
                    copyValue(baseExpression, expression)
                }
            }
        }

        private fun isIncrementOrDecrement(operationType: IElementType): Boolean =
            operationType === KtTokens.PLUSPLUS || operationType === KtTokens.MINUSMINUS

        override fun visitIfExpression(expression: KtIfExpression) {
            mark(expression)
            val branches = ArrayList<KtExpression>(2)
            val condition = expression.condition
            generateInstructions(condition)
            val elseLabel = builder.createUnboundLabel("else branch")
            builder.jumpOnFalse(elseLabel, expression, builder.getBoundValue(condition))
            val thenBranch = expression.then
            if (thenBranch != null) {
                branches.add(thenBranch)
                generateInstructions(thenBranch)
            } else {
                builder.loadUnit(expression)
            }
            val resultLabel = builder.createUnboundLabel("'if' expression result")
            builder.jump(resultLabel, expression)
            builder.bindLabel(elseLabel)
            val elseBranch = expression.`else`
            if (elseBranch != null) {
                branches.add(elseBranch)
                generateInstructions(elseBranch)
            } else {
                builder.loadUnit(expression)
            }
            builder.bindLabel(resultLabel)
            mergeValues(branches, expression)
        }

        private inner class FinallyBlockGenerator(private val finallyBlock: KtFinallySection?) {
            private var startFinally: Label? = null
            private var finishFinally: Label? = null

            fun generate() {
                val finalExpression = finallyBlock?.finalExpression ?: return
                catchFinallyStack.push(CatchFinallyLabels(null, null, null))
                startFinally?.let {
                    assert(finishFinally != null) { "startFinally label is set to $startFinally but finishFinally label is not set" }
                    builder.repeatPseudocode(it, finishFinally!!)
                    catchFinallyStack.pop()
                    return
                }
                builder.createUnboundLabel("start finally").let {
                    startFinally = it
                    builder.bindLabel(it)
                }
                generateInstructions(finalExpression)
                builder.createUnboundLabel("finish finally").let {
                    finishFinally = it
                    builder.bindLabel(it)
                }
                catchFinallyStack.pop()
            }
        }

        override fun visitTryExpression(expression: KtTryExpression) {
            mark(expression)

            val finallyBlock = expression.finallyBlock
            val finallyBlockGenerator = FinallyBlockGenerator(finallyBlock)
            val hasFinally = finallyBlock != null
            if (hasFinally) {
                builder.enterTryFinally(object : GenerationTrigger {
                    private var working = false

                    override fun generate() {
                        // This checks are needed for the case of having e.g. return inside finally: 'try {return} finally{return}'
                        if (working) return
                        working = true
                        finallyBlockGenerator.generate()
                        working = false
                    }
                })
            }

            val onExceptionToFinallyBlock = generateTryAndCatches(expression)

            if (hasFinally) {
                assert(onExceptionToFinallyBlock != null) { "No finally label generated: " + expression.text }

                builder.exitTryFinally()

                val skipFinallyToErrorBlock = builder.createUnboundLabel("skipFinallyToErrorBlock")
                builder.jump(skipFinallyToErrorBlock, expression)
                builder.bindLabel(onExceptionToFinallyBlock!!)
                finallyBlockGenerator.generate()
                builder.jumpToError(expression)
                builder.bindLabel(skipFinallyToErrorBlock)

                finallyBlockGenerator.generate()
            }

            val branches = ArrayList<KtExpression>()
            branches.add(expression.tryBlock)
            for (catchClause in expression.catchClauses) {
                catchClause.catchBody?.let { branches.add(it) }
            }
            mergeValues(branches, expression)
        }

        // Returns label for 'finally' block
        private fun generateTryAndCatches(expression: KtTryExpression): Label? {
            val catchClauses = expression.catchClauses
            val hasCatches = !catchClauses.isEmpty()

            var onException: Label? = null
            if (hasCatches) {
                onException = builder.createUnboundLabel("onException")
                builder.nondeterministicJump(onException, expression, null)
            }

            var onExceptionToFinallyBlock: Label? = null
            if (expression.finallyBlock != null) {
                onExceptionToFinallyBlock = builder.createUnboundLabel("onExceptionToFinallyBlock")
                builder.nondeterministicJump(onExceptionToFinallyBlock, expression, null)
            }

            val tryBlock = expression.tryBlock
            catchFinallyStack.push(CatchFinallyLabels(onException, onExceptionToFinallyBlock, expression))
            generateInstructions(tryBlock)
            generateJumpsToCatchAndFinally()
            catchFinallyStack.pop()

            if (hasCatches && onException != null) {
                val afterCatches = builder.createUnboundLabel("afterCatches")
                builder.jump(afterCatches, expression)

                builder.bindLabel(onException)
                val catchLabels = LinkedList<Label>()
                val catchClausesSize = catchClauses.size
                for (i in 0 until catchClausesSize - 1) {
                    catchLabels.add(builder.createUnboundLabel("catch " + i))
                }
                if (!catchLabels.isEmpty()) {
                    builder.nondeterministicJump(catchLabels, expression)
                }
                var isFirst = true
                for (catchClause in catchClauses) {
                    builder.enterBlockScope(catchClause)
                    if (!isFirst) {
                        builder.bindLabel(catchLabels.remove())
                    } else {
                        isFirst = false
                    }
                    val catchParameter = catchClause.catchParameter
                    if (catchParameter != null) {
                        builder.declareParameter(catchParameter)
                        generateInitializer(catchParameter, createSyntheticValue(catchParameter, MagicKind.FAKE_INITIALIZER))
                    }
                    generateInstructions(catchClause.catchBody)
                    builder.jump(afterCatches, expression)
                    builder.exitBlockScope(catchClause)
                }

                builder.bindLabel(afterCatches)
            }

            return onExceptionToFinallyBlock
        }

        override fun visitWhileExpression(expression: KtWhileExpression) {
            val loopInfo = builder.enterLoop(expression)

            builder.bindLabel(loopInfo.conditionEntryPoint)
            val condition = expression.condition
            generateInstructions(condition)
            mark(expression)
            if (!CompileTimeConstantUtils.canBeReducedToBooleanConstant(condition, trace.bindingContext, true)) {
                builder.jumpOnFalse(loopInfo.exitPoint, expression, builder.getBoundValue(condition))
            } else {
                assert(condition != null) { "Invalid while condition: " + expression.text }
                createSyntheticValue(condition!!, MagicKind.VALUE_CONSUMER, condition)
            }

            builder.enterLoopBody(expression)
            generateInstructions(expression.body)
            builder.jump(loopInfo.entryPoint, expression)
            builder.exitLoopBody(expression)
            builder.bindLabel(loopInfo.exitPoint)
            builder.loadUnit(expression)
        }

        override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
            builder.enterBlockScope(expression)
            mark(expression)
            val loopInfo = builder.enterLoop(expression)

            builder.enterLoopBody(expression)
            generateInstructions(expression.body)
            builder.exitLoopBody(expression)
            builder.bindLabel(loopInfo.conditionEntryPoint)
            val condition = expression.condition
            generateInstructions(condition)
            builder.exitBlockScope(expression)
            if (!CompileTimeConstantUtils.canBeReducedToBooleanConstant(condition, trace.bindingContext, true)) {
                builder.jumpOnTrue(loopInfo.entryPoint, expression, builder.getBoundValue(expression.condition))
            } else {
                assert(condition != null) { "Invalid do / while condition: " + expression.text }
                createSyntheticValue(condition!!, MagicKind.VALUE_CONSUMER, condition)
                builder.jump(loopInfo.entryPoint, expression)
            }
            builder.bindLabel(loopInfo.exitPoint)
            builder.loadUnit(expression)
        }

        override fun visitForExpression(expression: KtForExpression) {
            builder.enterBlockScope(expression)

            val loopRange = expression.loopRange
            generateInstructions(loopRange)
            generateLoopConventionCall(loopRange, BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL)
            declareLoopParameter(expression)

            // TODO : primitive cases
            val loopInfo = builder.enterLoop(expression)

            builder.bindLabel(loopInfo.conditionEntryPoint)
            generateLoopConventionCall(loopRange, BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL)
            builder.nondeterministicJump(loopInfo.exitPoint, expression, null)
            generateLoopConventionCall(loopRange, BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL)

            writeLoopParameterAssignment(expression)

            mark(expression)
            builder.enterLoopBody(expression)
            generateInstructions(expression.body)
            builder.jump(loopInfo.entryPoint, expression)

            builder.exitLoopBody(expression)
            builder.bindLabel(loopInfo.exitPoint)
            builder.loadUnit(expression)
            builder.exitBlockScope(expression)
        }

        private fun generateLoopConventionCall(
            loopRange: KtExpression?,
            callSlice: ReadOnlySlice<KtExpression, ResolvedCall<FunctionDescriptor>>
        ) {
            if (loopRange == null) return
            val resolvedCall = trace.bindingContext[callSlice, loopRange] ?: return
            generateCall(resolvedCall)
        }

        private fun declareLoopParameter(expression: KtForExpression) {
            val loopParameter = expression.loopParameter
            if (loopParameter != null) {
                val destructuringDeclaration = loopParameter.destructuringDeclaration
                if (destructuringDeclaration != null) {
                    visitDestructuringDeclaration(destructuringDeclaration, false)
                } else {
                    builder.declareParameter(loopParameter)
                }
            }
        }

        private fun writeLoopParameterAssignment(expression: KtForExpression) {
            val loopParameter = expression.loopParameter
            val loopRange = expression.loopRange

            val value = builder.magic(
                loopRange ?: expression,
                null,
                ContainerUtil.createMaybeSingletonList(builder.getBoundValue(loopRange)),
                MagicKind.LOOP_RANGE_ITERATION
            ).outputValue

            if (loopParameter != null) {
                val destructuringDeclaration = loopParameter.destructuringDeclaration
                if (destructuringDeclaration != null) {
                    for (entry in destructuringDeclaration.entries) {
                        generateInitializer(entry, value)
                    }
                } else {
                    generateInitializer(loopParameter, value)
                }
            }
        }

        override fun visitBreakExpression(expression: KtBreakExpression) {
            val loop = getCorrespondingLoop(expression)
            if (loop != null) {
                if (jumpCrossesTryCatchBoundary(expression, loop)) {
                    generateJumpsToCatchAndFinally()
                }
                if (jumpDoesNotCrossFunctionBoundary(expression, loop)) {
                    builder.getLoopExitPoint(loop)?.let { builder.jump(it, expression) }
                }
            }
        }

        override fun visitContinueExpression(expression: KtContinueExpression) {
            val loop = getCorrespondingLoop(expression)
            if (loop != null) {
                if (jumpCrossesTryCatchBoundary(expression, loop)) {
                    generateJumpsToCatchAndFinally()
                }
                if (jumpDoesNotCrossFunctionBoundary(expression, loop)) {
                    builder.getLoopConditionEntryPoint(loop)?.let { builder.jump(it, expression) }
                }
            }
        }

        private fun getNearestLoopExpression(expression: KtExpression) = expression.getStrictParentOfType<KtLoopExpression>()

        private fun getCorrespondingLoopWithoutLabel(expression: KtExpression): KtLoopExpression? {
            val parentLoop = getNearestLoopExpression(expression) ?: return null
            val parentBody = parentLoop.body
            return if (parentBody != null && parentBody.textRange.contains(expression.textRange)) {
                parentLoop
            } else {
                getNearestLoopExpression(parentLoop)
            }
        }

        private fun getCorrespondingLoop(expression: KtExpressionWithLabel): KtLoopExpression? {
            val labelName = expression.getLabelName()
            val loop: KtLoopExpression?
            if (labelName != null) {
                val targetLabel = expression.getTargetLabel()!!
                val labeledElement = trace.get(BindingContext.LABEL_TARGET, targetLabel)
                loop = if (labeledElement is KtLoopExpression) {
                    labeledElement
                } else {
                    trace.report(NOT_A_LOOP_LABEL.on(expression, targetLabel.text))
                    null
                }
            } else {
                loop = getCorrespondingLoopWithoutLabel(expression)
                if (loop == null) {
                    trace.report(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP.on(expression))
                } else {
                    val whenExpression = PsiTreeUtil.getParentOfType(
                        expression, KtWhenExpression::class.java, true,
                        KtLoopExpression::class.java
                    )
                    if (whenExpression != null) {
                        trace.report(BREAK_OR_CONTINUE_IN_WHEN.on(expression))
                    }
                }
            }
            loop?.body?.let {
                if (!it.textRange.contains(expression.textRange)) {
                    trace.report(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP.on(expression))
                    return null
                }
            }
            return loop
        }

        private fun returnCrossesTryCatchBoundary(returnExpression: KtReturnExpression): Boolean {
            val targetLabel = returnExpression.getTargetLabel() ?: return true
            val labeledElement = trace.get(BindingContext.LABEL_TARGET, targetLabel) ?: return true
            return jumpCrossesTryCatchBoundary(returnExpression, labeledElement)
        }

        private fun jumpCrossesTryCatchBoundary(jumpExpression: KtExpressionWithLabel, jumpTarget: PsiElement): Boolean {
            var current = jumpExpression.parent
            while (true) {
                when (current) {
                    jumpTarget -> return false
                    is KtTryExpression -> return true
                    else -> current = current.parent
                }
            }
        }

        private fun jumpDoesNotCrossFunctionBoundary(jumpExpression: KtExpressionWithLabel, jumpTarget: KtLoopExpression): Boolean {
            val bindingContext = trace.bindingContext

            val labelExprEnclosingFunc = BindingContextUtils.getEnclosingFunctionDescriptor(bindingContext, jumpExpression)
            val labelTargetEnclosingFunc = BindingContextUtils.getEnclosingFunctionDescriptor(bindingContext, jumpTarget)
            return if (labelExprEnclosingFunc !== labelTargetEnclosingFunc) {
                // Check to report only once
                if (builder.getLoopExitPoint(jumpTarget) != null ||
                    // Local class secondary constructors are handled differently
                    // They are the only local class element NOT included in owner pseudocode
                    // See generateInitializersForScriptClassOrObject && generateDeclarationForLocalClassOrObjectIfNeeded
                    labelExprEnclosingFunc is ConstructorDescriptor && !labelExprEnclosingFunc.isPrimary
                ) {
                    trace.report(BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY.on(jumpExpression))
                }
                false
            } else {
                true
            }
        }

        override fun visitReturnExpression(expression: KtReturnExpression) {
            if (returnCrossesTryCatchBoundary(expression)) {
                generateJumpsToCatchAndFinally()
            }
            val returnedExpression = expression.returnedExpression
            if (returnedExpression != null) {
                generateInstructions(returnedExpression)
            }
            val labelElement = expression.getTargetLabel()
            val subroutine: KtElement?
            val labelName = expression.getLabelName()
            subroutine = if (labelElement != null && labelName != null) {
                trace.get(BindingContext.LABEL_TARGET, labelElement)?.let { labeledElement ->
                    val labeledKtElement = labeledElement as KtElement
                    checkReturnLabelTarget(expression, labeledKtElement)
                    labeledKtElement
                }
            } else {
                builder.returnSubroutine
                // TODO : a context check
            }

            if (subroutine is KtFunction || subroutine is KtPropertyAccessor) {
                val returnValue = if (returnedExpression != null) builder.getBoundValue(returnedExpression) else null
                if (returnValue == null) {
                    builder.returnNoValue(expression, subroutine)
                } else {
                    builder.returnValue(expression, returnValue, subroutine)
                }
            } else {
                createNonSyntheticValue(expression, MagicKind.UNSUPPORTED_ELEMENT, returnedExpression)
            }
        }

        private fun checkReturnLabelTarget(returnExpression: KtReturnExpression, labeledElement: KtElement) {
            if (languageVersionSettings == null) return
            if (labeledElement !is KtFunctionLiteral && labeledElement !is KtNamedFunction) {
                if (languageVersionSettings.supportsFeature(LanguageFeature.RestrictReturnStatementTarget)) {
                    trace.report(Errors.NOT_A_FUNCTION_LABEL.on(returnExpression))
                } else {
                    trace.report(Errors.NOT_A_FUNCTION_LABEL_WARNING.on(returnExpression))
                }
            }
        }

        override fun visitParameter(parameter: KtParameter) {
            builder.declareParameter(parameter)
            val defaultValue = parameter.defaultValue
            if (defaultValue != null) {
                val skipDefaultValue = builder.createUnboundLabel("after default value for parameter ${parameter.name ?: "<anonymous>"}")
                builder.nondeterministicJump(skipDefaultValue, defaultValue, null)
                generateInstructions(defaultValue)
                builder.bindLabel(skipDefaultValue)
            }
            generateInitializer(parameter, computePseudoValueForParameter(parameter))

            parameter.destructuringDeclaration?.let {
                visitDestructuringDeclaration(it, generateWriteForEntries = true)
            }
        }

        private fun computePseudoValueForParameter(parameter: KtParameter): PseudoValue {
            val syntheticValue = createSyntheticValue(parameter, MagicKind.FAKE_INITIALIZER)
            val defaultValue = builder.getBoundValue(parameter.defaultValue) ?: return syntheticValue
            return builder.merge(parameter, arrayListOf(defaultValue, syntheticValue)).outputValue
        }

        override fun visitBlockExpression(expression: KtBlockExpression) {
            val declareBlockScope = !isBlockInDoWhile(expression)
            if (declareBlockScope) {
                builder.enterBlockScope(expression)
            }
            mark(expression)
            val statements = expression.statements
            for (statement in statements) {
                val afterClassLabel = (statement as? KtClassOrObject)?.let { builder.createUnboundLabel("after local class") }
                if (afterClassLabel != null) {
                    builder.nondeterministicJump(afterClassLabel, statement, null)
                }
                generateInstructions(statement)
                if (afterClassLabel != null) {
                    builder.bindLabel(afterClassLabel)
                }
            }
            if (statements.isEmpty()) {
                builder.loadUnit(expression)
            } else {
                copyValue(statements.lastOrNull(), expression)
            }
            if (declareBlockScope) {
                builder.exitBlockScope(expression)
            }
        }

        private fun isBlockInDoWhile(expression: KtBlockExpression): Boolean {
            val parent = expression.parent
            return parent.parent is KtDoWhileExpression
        }

        private fun visitFunction(function: KtFunction, invocationKind: InvocationKind? = null) {
            if (invocationKind == null) {
                processLocalDeclaration(function)
            } else {
                visitInlinedFunction(function, invocationKind)
            }

            val isAnonymousFunction = function is KtFunctionLiteral || function.name == null
            if (isAnonymousFunction || function.isLocal && function.parent !is KtBlockExpression) {
                builder.createLambda(function)
            }
        }

        private fun visitInlinedFunction(lambdaFunctionLiteral: KtFunction, invocationKind: InvocationKind) {
            // Defer emitting of inlined declaration
            deferredGeneratorsStack.peek().add({ builder ->
                                                   val beforeDeclaration = builder.createUnboundLabel("before inlined declaration")
                                                   val afterDeclaration = builder.createUnboundLabel("after inlined declaration")

                                                   builder.bindLabel(beforeDeclaration)

                                                   if (!invocationKind.isDefinitelyVisited()) {
                                                       builder.nondeterministicJump(afterDeclaration, lambdaFunctionLiteral, null)
                                                   }

                                                   generate(lambdaFunctionLiteral, invocationKind)

                                                   if (invocationKind.canBeRevisited()) {
                                                       builder.nondeterministicJump(beforeDeclaration, lambdaFunctionLiteral, null)
                                                   }

                                                   builder.bindLabel(afterDeclaration)
                                               })
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            visitFunction(function)
        }

        override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
            mark(lambdaExpression)
            val functionLiteral = lambdaExpression.functionLiteral

            // NB. Behaviour here is implicitly controlled by the LanguageFeature 'UseCallsInPlaceEffect'
            // If this feature is turned off, then slice LAMBDA_INVOCATIONS is never written and invocationKind
            // in all subsequent calls always 'null', resulting in falling back to old behaviour
            visitFunction(functionLiteral, trace[BindingContext.LAMBDA_INVOCATIONS, lambdaExpression])
            copyValue(functionLiteral, lambdaExpression)
        }

        override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
            mark(expression)
            val selectorExpression = expression.selectorExpression
            val receiverExpression = expression.receiverExpression
            val safe = expression is KtSafeQualifiedExpression

            // todo: replace with selectorExpresion != null after parser is fixed
            if (selectorExpression is KtCallExpression || selectorExpression is KtSimpleNameExpression) {
                if (!safe) {
                    generateInstructions(selectorExpression)
                } else {
                    val resultLabel = builder.createUnboundLabel("result of call")
                    builder.jumpOnFalse(resultLabel, expression, null)
                    generateInstructions(selectorExpression)
                    builder.bindLabel(resultLabel)
                }
                copyValue(selectorExpression, expression)
            } else {
                generateInstructions(receiverExpression)
                createNonSyntheticValue(expression, MagicKind.UNSUPPORTED_ELEMENT, receiverExpression)
            }
        }

        override fun visitCallExpression(expression: KtCallExpression) {
            if (!generateCall(expression)) {
                val inputExpressions = ArrayList<KtExpression>()
                for (argument in expression.valueArguments) {
                    val argumentExpression = argument.getArgumentExpression()
                    if (argumentExpression != null) {
                        generateInstructions(argumentExpression)
                        inputExpressions.add(argumentExpression)
                    }
                }
                val calleeExpression = expression.calleeExpression
                generateInstructions(calleeExpression)
                if (calleeExpression != null) {
                    inputExpressions.add(calleeExpression)
                    generateAndGetReceiverIfAny(expression)?.let { inputExpressions.add(it) }
                }

                mark(expression)
                createNonSyntheticValue(expression, inputExpressions, MagicKind.UNRESOLVED_CALL)
            }
        }

        private fun generateAndGetReceiverIfAny(expression: KtExpression): KtExpression? {
            val parent = expression.parent as? KtQualifiedExpression ?: return null

            if (parent.selectorExpression !== expression) return null

            val receiverExpression = parent.receiverExpression
            generateInstructions(receiverExpression)

            return receiverExpression
        }

        override fun visitProperty(property: KtProperty) {
            builder.declareVariable(property)
            val initializer = property.initializer
            if (initializer != null) {
                visitAssignment(property, getDeferredValue(initializer), property)
            }
            val delegate = property.delegateExpression
            if (delegate != null) {
                // We do not want to have getDeferredValue(delegate) here, because delegate value will be read anyway later
                visitAssignment(property, getDeferredValue(null), property)
                generateInstructions(delegate)
                if (property.isLocal) {
                    generateInitializer(property, createSyntheticValue(property, MagicKind.FAKE_INITIALIZER))
                }
                if (builder.getBoundValue(delegate) != null) {
                    createSyntheticValue(property, MagicKind.VALUE_CONSUMER, delegate)
                }
            }

            if (KtPsiUtil.isLocal(property)) {
                for (accessor in property.accessors) {
                    generateInstructions(accessor)
                }
            }
        }

        override fun visitDestructuringDeclaration(declaration: KtDestructuringDeclaration) {
            visitDestructuringDeclaration(declaration, true)
        }

        private fun visitDestructuringDeclaration(declaration: KtDestructuringDeclaration, generateWriteForEntries: Boolean) {
            val initializer = declaration.initializer
            generateInstructions(initializer)
            for (entry in declaration.entries) {
                builder.declareVariable(entry)

                val resolvedCall = trace.get(BindingContext.COMPONENT_RESOLVED_CALL, entry)

                val writtenValue: PseudoValue?
                writtenValue = if (resolvedCall != null) {
                    builder.call(
                        entry,
                        resolvedCall,
                        getReceiverValues(resolvedCall),
                        emptyMap()
                    ).outputValue
                } else {
                    initializer?.let { createSyntheticValue(entry, MagicKind.UNRESOLVED_CALL, it) }
                }

                if (generateWriteForEntries) {
                    generateInitializer(entry, writtenValue ?: createSyntheticValue(entry, MagicKind.FAKE_INITIALIZER))
                }
            }
        }

        override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
            processLocalDeclaration(accessor)
        }

        override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
            mark(expression)

            val operationType = expression.operationReference.getReferencedNameElementType()
            val left = expression.left
            if (operationType === KtTokens.AS_KEYWORD || operationType === KtTokens.`AS_SAFE`) {
                generateInstructions(left)
                if (getBoundOrUnreachableValue(left) != null) {
                    createNonSyntheticValue(expression, MagicKind.CAST, left)
                }
            } else {
                visitKtElement(expression)
                createNonSyntheticValue(expression, MagicKind.UNSUPPORTED_ELEMENT, left)
            }
        }

        private fun generateJumpsToCatchAndFinally() {
            if (catchFinallyStack.isNotEmpty()) {
                with(catchFinallyStack.peek()) {
                    if (tryExpression != null) {
                        onException?.let {
                            builder.nondeterministicJump(it, tryExpression, null)
                        }
                        toFinally?.let {
                            builder.nondeterministicJump(it, tryExpression, null)
                        }
                    }
                }
            }
        }

        override fun visitThrowExpression(expression: KtThrowExpression) {
            mark(expression)

            generateJumpsToCatchAndFinally()

            val thrownExpression = expression.thrownExpression ?: return
            generateInstructions(thrownExpression)

            val thrownValue = builder.getBoundValue(thrownExpression) ?: return
            builder.throwException(expression, thrownValue)
        }

        override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
            generateArrayAccess(expression, trace.get(BindingContext.INDEXED_LVALUE_GET, expression))
        }

        override fun visitIsExpression(expression: KtIsExpression) {
            mark(expression)
            val left = expression.leftHandSide
            generateInstructions(left)
            createNonSyntheticValue(expression, MagicKind.IS, left)
        }

        override fun visitWhenExpression(expression: KtWhenExpression) {
            mark(expression)

            val subjectExpression = expression.subjectExpression
            if (subjectExpression != null) {
                generateInstructions(subjectExpression)
            }

            val branches = ArrayList<KtExpression>()

            val doneLabel = builder.createUnboundLabel("after 'when' expression")

            var nextLabel: Label? = null
            val iterator = expression.entries.iterator()
            while (iterator.hasNext()) {
                val whenEntry = iterator.next()
                mark(whenEntry)

                val isElse = whenEntry.isElse
                if (isElse) {
                    if (iterator.hasNext()) {
                        trace.report(ELSE_MISPLACED_IN_WHEN.on(whenEntry))
                    }
                }
                val bodyLabel = builder.createUnboundLabel("'when' entry body")

                val conditions = whenEntry.conditions
                for (i in conditions.indices) {
                    val condition = conditions[i]
                    condition.accept(conditionVisitor)
                    if (i + 1 < conditions.size) {
                        builder.nondeterministicJump(bodyLabel, expression, builder.getBoundValue(condition))
                    }
                }

                if (!isElse) {
                    nextLabel = builder.createUnboundLabel("next 'when' entry")
                    val lastCondition = conditions.lastOrNull()
                    builder.nondeterministicJump(nextLabel, expression, builder.getBoundValue(lastCondition))
                }

                builder.bindLabel(bodyLabel)
                val whenEntryExpression = whenEntry.expression
                if (whenEntryExpression != null) {
                    generateInstructions(whenEntryExpression)
                    branches.add(whenEntryExpression)
                }
                builder.jump(doneLabel, expression)

                if (!isElse && nextLabel != null) {
                    builder.bindLabel(nextLabel)
                    // For the last entry of exhaustive when,
                    // attempt to jump further should lead to error, not to "done"
                    if (!iterator.hasNext() && WhenChecker.isWhenExhaustive(expression, trace)) {
                        builder.magic(expression, null, emptyList(), MagicKind.EXHAUSTIVE_WHEN_ELSE)
                    }
                }
            }
            builder.bindLabel(doneLabel)

            mergeValues(branches, expression)
            WhenChecker.checkDuplicatedLabels(expression, trace)
        }

        override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
            mark(expression)
            val declaration = expression.objectDeclaration
            generateInstructions(declaration)

            builder.createAnonymousObject(expression)
        }

        override fun visitObjectDeclaration(objectDeclaration: KtObjectDeclaration) {
            generateHeaderDelegationSpecifiers(objectDeclaration)
            generateInitializersForScriptClassOrObject(objectDeclaration)
            generateDeclarationForLocalClassOrObjectIfNeeded(objectDeclaration)
        }

        override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
            mark(expression)

            val inputExpressions = ArrayList<KtExpression>()
            for (entry in expression.entries) {
                if (entry is KtStringTemplateEntryWithExpression) {
                    val entryExpression = entry.getExpression()
                    generateInstructions(entryExpression)
                    if (entryExpression != null) {
                        inputExpressions.add(entryExpression)
                    }
                }
            }
            builder.loadStringTemplate(expression, elementsToValues(inputExpressions))
        }

        override fun visitTypeProjection(typeProjection: KtTypeProjection) {
            // TODO : Support Type Arguments. Companion object may be initialized at this point");
        }

        override fun visitAnonymousInitializer(classInitializer: KtAnonymousInitializer) {
            generateInstructions(classInitializer.body)
        }

        private fun generateHeaderDelegationSpecifiers(classOrObject: KtClassOrObject) {
            for (specifier in classOrObject.superTypeListEntries) {
                generateInstructions(specifier)
            }
        }

        private fun generateInitializersForScriptClassOrObject(classOrObject: KtDeclarationContainer) {
            for (declaration in classOrObject.declarations) {
                if (declaration is KtProperty || declaration is KtAnonymousInitializer) {
                    generateInstructions(declaration)
                }
            }
        }

        private fun processEntryOrObject(entryOrObject: KtClassOrObject) {
            val classDescriptor = trace[BindingContext.DECLARATION_TO_DESCRIPTOR, entryOrObject]
            if (classDescriptor is ClassDescriptor) {
                builder.declareEntryOrObject(entryOrObject)
                builder.write(
                    entryOrObject, entryOrObject, createSyntheticValue(entryOrObject, MagicKind.FAKE_INITIALIZER),
                    AccessTarget.Declaration(FakeCallableDescriptorForObject(classDescriptor)), emptyMap()
                )
                generateInstructions(entryOrObject)
            }
        }

        override fun visitClass(klass: KtClass) {
            if (klass.hasPrimaryConstructor()) {
                processParameters(klass.primaryConstructorParameters)

                // delegation specifiers of primary constructor, anonymous class and property initializers
                generateHeaderDelegationSpecifiers(klass)
                generateInitializersForScriptClassOrObject(klass)
            }

            generateDeclarationForLocalClassOrObjectIfNeeded(klass)

            if (klass.isEnum()) {
                klass.declarations.forEach {
                    when (it) {
                        is KtEnumEntry -> {
                            processEntryOrObject(it)
                        }
                        is KtObjectDeclaration -> if (it.isCompanion()) {
                            processEntryOrObject(it)
                        }
                    }
                }
            }
        }

        override fun visitScript(script: KtScript) {
            generateInitializersForScriptClassOrObject(script)
        }

        private fun generateDeclarationForLocalClassOrObjectIfNeeded(classOrObject: KtClassOrObject) {
            if (classOrObject.isLocal) {
                for (declaration in classOrObject.declarations) {
                    if (declaration is KtSecondaryConstructor ||
                        declaration is KtProperty ||
                        declaration is KtAnonymousInitializer
                    ) {
                        continue
                    }
                    generateInstructions(declaration)
                }
            }
        }

        private fun processParameters(parameters: List<KtParameter>) {
            for (parameter in parameters) {
                generateInstructions(parameter)
            }
        }

        override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
            val classOrObject =
                PsiTreeUtil.getParentOfType(constructor, KtClassOrObject::class.java) ?: error("Guaranteed by parsing contract")

            processParameters(constructor.valueParameters)
            generateCallOrMarkUnresolved(constructor.getDelegationCall())

            if (!constructor.getDelegationCall().isCallToThis) {
                generateInitializersForScriptClassOrObject(classOrObject)
            }

            generateInstructions(constructor.bodyExpression)
        }

        override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry) {
            generateCallOrMarkUnresolved(call)
        }

        override fun visitInitializerList(list: KtInitializerList) {
            list.acceptChildren(this)
        }

        private fun generateCallOrMarkUnresolved(call: KtCallElement) {
            if (!generateCall(call)) {
                val arguments = call.valueArguments.mapNotNull(ValueArgument::getArgumentExpression)

                for (argument in arguments) {
                    generateInstructions(argument)
                }
                createNonSyntheticValue(call, arguments, MagicKind.UNRESOLVED_CALL)
            }
        }

        override fun visitDelegatedSuperTypeEntry(specifier: KtDelegatedSuperTypeEntry) {
            val delegateExpression = specifier.delegateExpression
            generateInstructions(delegateExpression)
            if (delegateExpression != null) {
                createSyntheticValue(specifier, MagicKind.VALUE_CONSUMER, delegateExpression)
            }
        }

        override fun visitSuperTypeEntry(specifier: KtSuperTypeEntry) {
            // Do not generate UNSUPPORTED_ELEMENT here
        }

        override fun visitSuperTypeList(list: KtSuperTypeList) {
            list.acceptChildren(this)
        }

        override fun visitKtFile(file: KtFile) {
            for (declaration in file.declarations) {
                if (declaration is KtProperty) {
                    generateInstructions(declaration)
                }
            }
        }

        override fun visitDoubleColonExpression(expression: KtDoubleColonExpression) {
            mark(expression)
            val receiverExpression = expression.receiverExpression
            if (receiverExpression != null &&
                trace.bindingContext.get(BindingContext.DOUBLE_COLON_LHS, receiverExpression) is DoubleColonLHS.Expression
            ) {
                generateInstructions(receiverExpression)
                createNonSyntheticValue(expression, MagicKind.BOUND_CALLABLE_REFERENCE, receiverExpression)
            } else {
                createNonSyntheticValue(expression, MagicKind.UNBOUND_CALLABLE_REFERENCE)
            }
        }

        override fun visitKtElement(element: KtElement) {
            createNonSyntheticValue(element, MagicKind.UNSUPPORTED_ELEMENT)
        }

        private fun generateQualifier(expression: KtExpression, qualifier: Qualifier): Boolean {
            val qualifierDescriptor = qualifier.descriptor
            if (qualifierDescriptor is ClassDescriptor) {
                getFakeDescriptorForObject(qualifierDescriptor)?.let {
                    mark(expression)
                    builder.read(expression, AccessTarget.Declaration(it), emptyMap())
                    return true
                }
            }
            return false
        }

        private fun generateCall(callElement: KtElement): Boolean {
            val resolvedCall = callElement.getResolvedCall(trace.bindingContext)
            val callElementFromResolvedCall = resolvedCall?.call?.callElement ?: return false
            if (callElement.isAncestor(callElementFromResolvedCall, true)) return false
            return checkAndGenerateCall(resolvedCall)
        }

        private fun checkAndGenerateCall(resolvedCall: ResolvedCall<*>?): Boolean {
            if (resolvedCall == null) return false
            generateCall(resolvedCall)
            return true
        }

        private fun generateCall(resolvedCall: ResolvedCall<*>): InstructionWithValue {
            val callElement = resolvedCall.call.callElement

            val receivers = getReceiverValues(resolvedCall)

            deferredGeneratorsStack.push(mutableListOf())

            var parameterValues = SmartFMap.emptyMap<PseudoValue, ValueParameterDescriptor>()
            for (argument in resolvedCall.call.valueArguments) {
                val argumentMapping = resolvedCall.getArgumentMapping(argument)
                val argumentExpression = argument.getArgumentExpression()
                if (argumentMapping is ArgumentMatch) {
                    parameterValues = generateValueArgument(argument, argumentMapping.valueParameter, parameterValues)
                } else if (argumentExpression != null) {
                    generateInstructions(argumentExpression)
                    createSyntheticValue(argumentExpression, MagicKind.VALUE_CONSUMER, argumentExpression)
                }
            }

            if (resolvedCall.resultingDescriptor is VariableDescriptor) {
                // If a callee of the call is just a variable (without 'invoke'), 'read variable' is generated.
                // todo : process arguments for such a case (KT-5387)
                val callExpression =
                    callElement as? KtExpression ?: error("Variable-based call without callee expression: " + callElement.text)
                assert(parameterValues.isEmpty()) { "Variable-based call with non-empty argument list: " + callElement.text }
                return builder.readVariable(callExpression, resolvedCall, receivers)
            }

            mark(resolvedCall.call.callElement)
            val callInstruction = builder.call(callElement, resolvedCall, receivers, parameterValues)
            val deferredGeneratorsForCall = deferredGeneratorsStack.pop()
            deferredGeneratorsForCall.forEach { it.invoke(builder) }
            return callInstruction
        }

        private fun getReceiverValues(resolvedCall: ResolvedCall<*>): Map<PseudoValue, ReceiverValue> {
            var varCallResult: PseudoValue? = null
            var explicitReceiver: ReceiverValue? = null
            if (resolvedCall is VariableAsFunctionResolvedCall) {
                varCallResult = generateCall(resolvedCall.variableCall).outputValue

                val kind = resolvedCall.explicitReceiverKind
                //noinspection EnumSwitchStatementWhichMissesCases
                when (kind) {
                    ExplicitReceiverKind.DISPATCH_RECEIVER -> explicitReceiver = resolvedCall.dispatchReceiver
                    ExplicitReceiverKind.EXTENSION_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> explicitReceiver =
                            resolvedCall.extensionReceiver
                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> {
                    }
                }
            }

            var receiverValues = SmartFMap.emptyMap<PseudoValue, ReceiverValue>()
            if (explicitReceiver != null && varCallResult != null) {
                receiverValues = receiverValues.plus(varCallResult, explicitReceiver)
            }
            val callElement = resolvedCall.call.callElement
            receiverValues = getReceiverValues(callElement, resolvedCall.dispatchReceiver, receiverValues)
            receiverValues = getReceiverValues(callElement, resolvedCall.extensionReceiver, receiverValues)
            return receiverValues
        }

        private fun getReceiverValues(
            callElement: KtElement,
            receiver: ReceiverValue?,
            receiverValuesArg: SmartFMap<PseudoValue, ReceiverValue>
        ): SmartFMap<PseudoValue, ReceiverValue> {
            var receiverValues = receiverValuesArg
            if (receiver == null || receiverValues.containsValue(receiver)) return receiverValues

            when (receiver) {
                is ImplicitReceiver -> {
                    if (callElement is KtCallExpression) {
                        val declaration = receiver.declarationDescriptor
                        if (declaration is ClassDescriptor) {
                            val fakeDescriptor = getFakeDescriptorForObject(declaration)
                            val calleeExpression = callElement.calleeExpression
                            if (fakeDescriptor != null && calleeExpression != null) {
                                builder.read(calleeExpression, AccessTarget.Declaration(fakeDescriptor), emptyMap())
                            }
                        }
                    }
                    receiverValues = receiverValues.plus(createSyntheticValue(callElement, MagicKind.IMPLICIT_RECEIVER), receiver)
                }
                is ExpressionReceiver -> {
                    val expression = receiver.expression
                    if (builder.getBoundValue(expression) == null) {
                        generateInstructions(expression)
                    }

                    val receiverPseudoValue = getBoundOrUnreachableValue(expression)
                    if (receiverPseudoValue != null) {
                        receiverValues = receiverValues.plus(receiverPseudoValue, receiver)
                    }
                }
                is TransientReceiver -> {
                    // Do nothing
                }
                else -> {
                    throw IllegalArgumentException("Unknown receiver kind: " + receiver)
                }
            }

            return receiverValues
        }

        private fun generateValueArgument(
            valueArgument: ValueArgument,
            parameterDescriptor: ValueParameterDescriptor,
            parameterValuesArg: SmartFMap<PseudoValue, ValueParameterDescriptor>
        ): SmartFMap<PseudoValue, ValueParameterDescriptor> {
            var parameterValues = parameterValuesArg
            val expression = valueArgument.getArgumentExpression()
            if (expression != null) {
                if (!valueArgument.isExternal()) {
                    generateInstructions(expression)
                }

                val argValue = getBoundOrUnreachableValue(expression)
                if (argValue != null) {
                    parameterValues = parameterValues.plus(argValue, parameterDescriptor)
                }
            }
            return parameterValues
        }
    }
}