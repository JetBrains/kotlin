/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.highlighter.visitors

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinIdeaAnalysisBundle
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.ImplicitReceiverSmartcastKind
import org.jetbrains.kotlin.psi.*

internal class ExpressionsSmartcastHighlightingVisitor(
    analysisSession: KtAnalysisSession,
    holder: AnnotationHolder
) : FirAfterResolveHighlightingVisitor(analysisSession, holder) {
    override fun visitExpression(expression: KtExpression) = with(analysisSession) {
        expression.getImplicitReceiverSmartCasts().forEach { (types, kind) ->
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
                        type.asStringForDebugging()
                    )
                ).textAttributes = org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors.SMART_CAST_RECEIVER
            }
        }
        expression.getSmartCasts()?.forEach { type ->
            createInfoAnnotation(
                getSmartCastTarget(expression),
                KotlinIdeaAnalysisBundle.message(
                    "smart.cast.to.0",
                    type.asStringForDebugging()
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