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
    private fun KtDeclaration.processVariable() {
        processHierarchyUpward(parentUsage.scope) {
            if (this is KtParameter && !canProcess()) return@processHierarchyUpward

            val withDereferences = parentUsage.params.showInstanceDereferences
            val accessKind = if (withDereferences) AccessKind.READ_OR_WRITE else AccessKind.READ_ONLY
            (this as? KtDeclaration)?.processVariableAccesses(parentUsage.scope.toSearchScope(), accessKind) body@{
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

    private fun KtFunction.processFunction() {
        if (this is KtConstructor<*> || this is KtNamedFunction && name != null) {
            processCalls(parentUsage.scope.toSearchScope(), includeOverriders = false) {
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

        val funExpression = when (this) {
            is KtFunctionLiteral -> parent as? KtLambdaExpression
            is KtNamedFunction -> this
            else -> null
        } ?: return
        (funExpression as PsiElement).passToProcessor(parentUsage.lambdaLevel + 1, true)
    }

    private fun processDereferenceIsNeeded(
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
                is ReadValueInstruction -> processDereferenceIsNeeded(this, pseudoValue, instr)
                is CallInstruction -> processDereferenceIsNeeded(this, pseudoValue, instr)
            }
        }
    }

    private fun KtExpression.processExpression() {
        processPseudocodeUsages { pseudoValue, instr ->
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

    override fun processChildren() {
        if (parentUsage.forcedExpressionMode) return element.processExpression()

        when (element) {
            is KtProperty, is KtParameter -> (element as KtDeclaration).processVariable()
            is KtFunction -> element.processFunction()
            is KtPropertyAccessor -> if (element.isGetter) {
                element.property.processVariable()
            }
            else -> element.processExpression()
        }
    }
}