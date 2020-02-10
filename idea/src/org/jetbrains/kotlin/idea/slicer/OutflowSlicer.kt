package org.jetbrains.kotlin.idea.slicer

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.light.LightMemberReference
import com.intellij.slicer.SliceUsage
import com.intellij.util.Processor
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ReturnValueInstruction
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReadWriteAccessDetector
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.source.getPsi

class OutflowSlicer(
    element: KtExpression,
    processor: Processor<SliceUsage>,
    parentUsage: KotlinSliceUsage
) : Slicer(element, processor, parentUsage) {

    override fun processChildren() {
        if (parentUsage.forcedExpressionMode) return processExpression(element)

        when (element) {
            is KtProperty -> processVariable(element)
            is KtParameter -> processVariable(element)
            is KtFunction -> processFunction(element)
            is KtPropertyAccessor -> if (element.isGetter) processVariable(element.property)
            else -> processExpression(element)
        }
    }

    private fun processVariable(variable: KtCallableDeclaration) {
        if (variable is KtParameter && !canProcessParameter(variable)) return

        val withDereferences = parentUsage.params.showInstanceDereferences
        val accessKind = if (withDereferences) AccessKind.READ_OR_WRITE else AccessKind.READ_ONLY
        processVariableAccesses(
            variable,
            analysisScope,
            accessKind
        ) body@{
            val refElement = it.element
            if (refElement !is KtExpression) {
                refElement?.passToProcessor()
                return@body
            }

            val refExpression = KtPsiUtil.safeDeparenthesize(refElement)
            if (withDereferences) {
                refExpression.processDereferences()
            }
            if (!withDereferences || KotlinReadWriteAccessDetector.INSTANCE.getExpressionAccess(refExpression) == ReadWriteAccessDetector.Access.Read) {
                refExpression.passToProcessor()
            }
        }
    }

    private fun processFunction(function: KtFunction) {
        if (function is KtConstructor<*> || function is KtNamedFunction && function.name != null) {
            function.processCalls(analysisScope, includeOverriders = false) {
                when (val refElement = it.element) {
                    null -> (it.reference as? LightMemberReference)?.element?.passToProcessor()
                    is KtExpression -> {
                        refElement.getCallElementForExactCallee()?.passToProcessor()
                        refElement.getCallableReferenceForExactCallee()?.passToProcessor(parentUsage.lambdaLevel + 1)
                    }
                    else -> refElement.passToProcessor()
                }
            }
            return
        }

        val funExpression = when (function) {
            is KtFunctionLiteral -> function.parent as? KtLambdaExpression
            is KtNamedFunction -> function
            else -> null
        } ?: return
        (funExpression as PsiElement).passToProcessor(parentUsage.lambdaLevel + 1, true)
    }

    private fun processExpression(expression: KtExpression) {
        expression.processPseudocodeUsages { pseudoValue, instr ->
            when (instr) {
                is WriteValueInstruction -> instr.target.accessedDescriptor?.originalSource?.getPsi()?.passToProcessor()
                is CallInstruction -> {
                    if (parentUsage.lambdaLevel > 0 && instr.receiverValues[pseudoValue] != null) {
                        instr.element.passToProcessor(parentUsage.lambdaLevel - 1)
                    } else {
                        instr.arguments[pseudoValue]?.originalSource?.getPsi()?.passToProcessor()
                    }
                }
                is ReturnValueInstruction -> instr.subroutine.passToProcessor()
                is MagicInstruction -> when (instr.kind) {
                    MagicKind.NOT_NULL_ASSERTION, MagicKind.CAST -> instr.outputValue.element?.passToProcessor()
                    else -> {
                    }
                }
            }
        }
    }

    private fun PsiElement.getCallElementForExactCallee(): PsiElement? {
        if (this is KtArrayAccessExpression) return this

        val operationRefExpr = getNonStrictParentOfType<KtOperationReferenceExpression>()
        if (operationRefExpr != null) return operationRefExpr.parent as? KtOperationExpression

        val parentCall = getParentOfTypeAndBranch<KtCallElement> { calleeExpression } ?: return null
        val callee = parentCall.calleeExpression?.let { KtPsiUtil.safeDeparenthesize(it) }
        if (callee == this || callee is KtConstructorCalleeExpression && callee.isAncestor(this, true)) return parentCall

        return null
    }

    private fun PsiElement.getCallableReferenceForExactCallee(): KtCallableReferenceExpression? {
        val callableRef = getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference } ?: return null
        val callee = KtPsiUtil.safeDeparenthesize(callableRef.callableReference)
        return if (callee == this) callableRef else null
    }

    private fun processDereferenceIfNeeded(
        expression: KtExpression,
        pseudoValue: PseudoValue,
        instr: InstructionWithReceivers
    ) {
        if (!parentUsage.params.showInstanceDereferences) return

        val receiver = instr.receiverValues[pseudoValue]
        val resolvedCall = when (instr) {
            is CallInstruction -> instr.resolvedCall
            is ReadValueInstruction -> (instr.target as? AccessTarget.Call)?.resolvedCall
            else -> null
        } ?: return

        if (receiver != null && resolvedCall.dispatchReceiver == receiver) {
            processor.process(
                KotlinSliceDereferenceUsage(
                    expression,
                    parentUsage,
                    parentUsage.lambdaLevel
                )
            )
        }
    }

    private fun KtExpression.processPseudocodeUsages(processor: (PseudoValue, Instruction) -> Unit) {
        val pseudocode = pseudocodeCache[this] ?: return
        val pseudoValue = pseudocode.getElementValue(this) ?: return
        pseudocode.getUsages(pseudoValue).forEach { processor(pseudoValue, it) }
    }

    private fun KtExpression.processDereferences() {
        processPseudocodeUsages { pseudoValue, instr ->
            when (instr) {
                is ReadValueInstruction -> processDereferenceIfNeeded(this, pseudoValue, instr)
                is CallInstruction -> processDereferenceIfNeeded(this, pseudoValue, instr)
            }
        }
    }
}