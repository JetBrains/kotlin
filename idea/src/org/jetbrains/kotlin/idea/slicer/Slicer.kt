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

package org.jetbrains.kotlin.idea.slicer

import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.cfg.pseudocode.getContainingPseudocode
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ReturnValueInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.findUsages.KotlinFunctionFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.processAllUsages
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.source.getPsi
import java.util.*

private fun KtFunction.processCalls(scope: SearchScope, processor: (UsageInfo) -> Unit) {
    processAllUsages(
            {
                KotlinFunctionFindUsagesOptions(project).apply {
                    isSearchForTextOccurrences = false
                    isSkipImportStatements = true
                    searchScope = scope.intersectWith(useScope)
                }
            },
            processor
    )
}

private fun KtDeclaration.processVariableAccesses(scope: SearchScope, isRead: Boolean, processor: (UsageInfo) -> Unit) {
    processAllUsages(
            {
                KotlinPropertyFindUsagesOptions(project).apply {
                    isReadAccess = isRead
                    isWriteAccess = !isRead
                    isReadWriteAccess = false
                    isSearchForTextOccurrences = false
                    isSkipImportStatements = true
                    searchScope = scope.intersectWith(useScope)
                }
            },
            processor
    )
}

private fun KtProperty.canProcess(): Boolean {
    if (hasDelegate()) return false
    if (isLocal) return true
    val descriptor = resolveToDescriptor() as? PropertyDescriptor ?: return false
    return descriptor.accessors.all { it.isDefault }
}

private fun KtParameter.canProcess(): Boolean {
    return !(isLoopParameter || isVarArg || hasValOrVar())
}

abstract class Slicer(
        protected val element: KtExpression,
        protected val processor: Processor<SliceUsage>,
        protected val parentUsage: SliceUsage
) {
    protected class PseudocodeCache {
        private val computedPseudocodes = HashMap<KtElement, Pseudocode>()

        operator fun get(element: KtElement): Pseudocode? {
            val container = element.containingDeclarationForPseudocode ?: return null
            return computedPseudocodes.getOrPut(container) {
                container.getContainingPseudocode(container.analyzeFully())?.apply { computedPseudocodes[container] = this } ?: return null
            }
        }
    }

    protected val pseudocodeCache = PseudocodeCache()

    protected fun PsiElement.passToProcessor() {
        processor.process(KotlinSliceUsage(this, parentUsage))
    }

    abstract fun processChildren()
}

class InflowSlicer(
        element: KtExpression,
        processor: Processor<SliceUsage>,
        parentUsage: SliceUsage
) : Slicer(element, processor, parentUsage) {
    private fun KtProperty.processProperty() {
        if (!canProcess()) return

        initializer?.passToProcessor()
        processVariableAccesses(parentUsage.scope.toSearchScope(), false) body@ {
            val refExpression = it.element as? KtExpression ?: return@body
            val rhs = KtPsiUtil.safeDeparenthesize(refExpression).getAssignmentByLHS()?.right ?: return@body
            rhs.passToProcessor()
        }
    }

    private fun KtParameter.processParameter() {
        if (!canProcess()) return

        val function = ownerFunction ?: return
        if (function.isOverridable()) return

        val parameterDescriptor = resolveToDescriptor() as ValueParameterDescriptor

        function.processCalls(parentUsage.scope.toSearchScope()) body@ {
            val refExpression = it.element as? KtExpression ?: return@body
            val callElement = refExpression.getParentOfTypeAndBranch<KtCallElement> { calleeExpression } ?: return@body
            val resolvedCall = callElement.getResolvedCall(callElement.analyze()) ?: return@body
            val resolvedArgument = resolvedCall.valueArguments[parameterDescriptor] ?: return@body
            val flownExpression = when (resolvedArgument) {
                                      is DefaultValueArgument -> defaultValue
                                      is ExpressionValueArgument -> resolvedArgument.valueArgument?.getArgumentExpression()
                                      else -> null
                                  } ?: return@body
            flownExpression.passToProcessor()
        }
    }

    private fun KtFunction.processFunction() {
        val pseudocode = pseudocodeCache[bodyExpression ?: return] ?: return
        pseudocode.traverse(TraversalOrder.FORWARD) { instr ->
            if (instr is ReturnValueInstruction && instr.subroutine == this) {
                (instr.returnExpressionIfAny?.returnedExpression ?: instr.element as? KtExpression)?.passToProcessor()
            }
        }
    }

    private fun Instruction.passInputsToProcessor() {
        inputValues.forEach { it.element?.passToProcessor() }
    }

    private fun KtExpression.processExpression() {
        val pseudocode = pseudocodeCache[this] ?: return
        val expressionValue = pseudocode.getElementValue(this) ?: return
        val createdAt = expressionValue.createdAt
        when (createdAt) {
            is ReadValueInstruction -> (createdAt.target.accessedDescriptor?.source?.getPsi() as? KtDeclaration)?.passToProcessor()

            is MergeInstruction -> createdAt.passInputsToProcessor()

            is MagicInstruction -> when (createdAt.kind) {
                MagicKind.NOT_NULL_ASSERTION, MagicKind.CAST -> createdAt.passInputsToProcessor()
                else -> return
            }

            is CallInstruction -> (createdAt.resolvedCall.resultingDescriptor.source.getPsi() as? KtDeclarationWithBody)?.passToProcessor()
        }
    }

    override fun processChildren() {
        when (element) {
            is KtProperty -> element.processProperty()
            is KtParameter -> element.processParameter()
            is KtFunction -> element.processFunction()
            else -> element.processExpression()
        }
    }
}

class OutflowSlicer(
        element: KtExpression,
        processor: Processor<SliceUsage>,
        parentUsage: SliceUsage
) : Slicer(element, processor, parentUsage) {
    private fun KtDeclaration.processVariable() {
        when (this) {
            is KtProperty -> if (!canProcess()) return
            is KtParameter -> if (!canProcess()) return
            else -> return
        }
        processVariableAccesses(parentUsage.scope.toSearchScope(),true) body@ {
            val refExpression = (it.element as? KtExpression)?.let { KtPsiUtil.safeDeparenthesize(it) } ?: return@body
            refExpression.processExpression()
        }
    }

    private fun PsiElement.getCallElementForExactCallee(): KtElement? {
        if (this is KtArrayAccessExpression) return this

        val operationRefExpr = getNonStrictParentOfType<KtOperationReferenceExpression>()
        if (operationRefExpr != null) return operationRefExpr.parent as? KtOperationExpression

        val parentCall = getParentOfTypeAndBranch<KtCallElement> { calleeExpression } ?: return null
        val callee = parentCall.calleeExpression?.let { KtPsiUtil.safeDeparenthesize(it) }
        if (callee == this || callee is KtConstructorCalleeExpression && callee.isAncestor(this, true)) return parentCall
        return null
    }

    private fun KtFunction.processFunctionCalls() {
        processCalls(parentUsage.scope.toSearchScope()) {
            it.element?.getCallElementForExactCallee()?.passToProcessor()
        }
    }

    private fun KtExpression.processExpression() {
        val pseudocode = pseudocodeCache[this] ?: return
        val pseudoValue = pseudocode.getElementValue(this) ?: return
        pseudocode.getUsages(pseudoValue).forEach { instr ->
            when (instr) {
                is WriteValueInstruction -> (instr.target.accessedDescriptor?.source?.getPsi() as? KtDeclaration)?.passToProcessor()
                is CallInstruction -> instr.arguments[pseudoValue]?.source?.getPsi()?.passToProcessor()
                is ReturnValueInstruction -> (instr.owner.correspondingElement as? KtFunction)?.passToProcessor()
                is MagicInstruction -> when (instr.kind) {
                    MagicKind.NOT_NULL_ASSERTION, MagicKind.CAST -> instr.outputValue.element?.passToProcessor()
                    else -> return
                }
            }
        }
    }

    override fun processChildren() {
        when (element) {
            is KtProperty, is KtParameter -> (element as KtDeclaration).processVariable()
            is KtFunction -> element.processFunctionCalls()
            else -> element.processExpression()
        }
    }
}