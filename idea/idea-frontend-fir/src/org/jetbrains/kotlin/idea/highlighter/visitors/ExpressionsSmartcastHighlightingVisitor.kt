/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.visitors

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinIdeaAnalysisBundle
import org.jetbrains.kotlin.idea.frontend.api.FrontendAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.ImplicitReceiverSmartcastKind
import org.jetbrains.kotlin.psi.*

internal class ExpressionsSmartcastHighlightingVisitor(
    analysisSession: FrontendAnalysisSession,
    holder: AnnotationHolder
) : FirAfterResolveHighlightingVisitor(analysisSession, holder) {
    override fun visitExpression(expression: KtExpression) {
        analysisSession.getImplicitReceiverSmartCasts(expression).forEach { (types, kind) ->
            val receiverName = when (kind) {
                ImplicitReceiverSmartcastKind.EXTENSION -> KotlinIdeaAnalysisBundle.message("extension.implicit.receiver")
                ImplicitReceiverSmartcastKind.DISPATCH -> KotlinIdeaAnalysisBundle.message("implicit.receiver")
            }

            types.forEach { type ->
                createInfoAnnotation(
                    expression,
                    KotlinIdeaAnalysisBundle.message(
                        "0.smart.cast.to.1",
                        receiverName,
                        analysisSession.renderType(type)
                    )
                ).textAttributes = org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors.SMART_CAST_RECEIVER
            }
        }
        analysisSession.getSmartCastedToTypes(expression)?.forEach { type ->
            createInfoAnnotation(
                getSmartCastTarget(expression),
                KotlinIdeaAnalysisBundle.message(
                    "smart.cast.to.0",
                    analysisSession.renderType(type)
                )
            ).textAttributes = org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors.SMART_CAST_VALUE
        }

        //todo smartcast to null

        super.visitExpression(expression)
    }
}


private fun getSmartCastTarget(expression: KtExpression): PsiElement {
    var target: PsiElement = expression
    if (target is KtParenthesizedExpression) {
        target = KtPsiUtil.deparenthesize(target) ?: expression
    }
    return when (target) {
        is KtIfExpression -> target.ifKeyword
        is KtWhenExpression -> target.whenKeyword
        is KtBinaryExpression -> target.operationReference
        else -> target
    }
}