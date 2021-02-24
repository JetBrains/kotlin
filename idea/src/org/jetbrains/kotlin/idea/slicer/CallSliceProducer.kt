/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.light.LightMemberReference
import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

object CallSliceProducer : SliceProducer {
    override fun produce(usage: UsageInfo, mode: KotlinSliceAnalysisMode, parent: SliceUsage): Collection<SliceUsage>? {
        if ((parent as? KotlinSliceUsage)?.mode?.currentBehaviour is LambdaCallsBehaviour) {
            // UsageInfo produced by LambdaCallsBehaviour has full call-element and does not require any processing
            return null
        }

        when (val refElement = usage.element) {
            null -> {
                val element = (usage.reference as? LightMemberReference)?.element ?: return emptyList()
                return listOf(KotlinSliceUsage(element, parent, mode, false))
            }

            is KtExpression -> {
                return mutableListOf<SliceUsage>().apply {
                    refElement.getCallElementForExactCallee()
                        ?.let { this += KotlinSliceUsage(it, parent, mode, false) }
                    refElement.getCallableReferenceForExactCallee()
                        ?.let {
                            this += KotlinSliceUsage(
                                it,
                                parent,
                                mode.withBehaviour(LambdaCallsBehaviour(SliceProducer.Trivial)),
                                forcedExpressionMode = true
                            )
                        }
                }
            }

            else -> {
                return null // unknown type of usage - return null to process it "as is"
            }
        }
    }

    override val testPresentation: String?
        get() = null

    override fun equals(other: Any?) = other === this
    override fun hashCode() = 0

    private fun PsiElement.getCallElementForExactCallee(): PsiElement? {
        if (this is KtArrayAccessExpression) return this

        val operationRefExpr = getNonStrictParentOfType<KtOperationReferenceExpression>()
        if (operationRefExpr != null) return operationRefExpr.parent as? KtOperationExpression

        val parentCall = getParentOfTypeAndBranch<KtCallElement> { calleeExpression } ?: return null
        val callee = parentCall.calleeExpression?.let { KtPsiUtil.safeDeparenthesize(it) }
        if (callee == this || callee is KtConstructorCalleeExpression && callee.isAncestor(this, strict = true)) return parentCall

        return null
    }

    private fun PsiElement.getCallableReferenceForExactCallee(): KtCallableReferenceExpression? {
        val callableRef = getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference } ?: return null
        val callee = KtPsiUtil.safeDeparenthesize(callableRef.callableReference)
        return if (callee == this) callableRef else null
    }
}