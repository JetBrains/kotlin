/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.context

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.analyseInDependedAnalysisSession
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression

internal sealed class FirPositionCompletionContext {
    abstract val position: PsiElement
}

internal class FirNameReferencePositionContext(
    override val position: PsiElement,
    val reference: KtSimpleNameReference,
    val nameExpression: KtSimpleNameExpression,
    val explicitReceiver: KtExpression?
) : FirPositionCompletionContext()

internal class FirUnknownPositionContext(
    override val position: PsiElement
) : FirPositionCompletionContext()

internal object FirPositionCompletionContextDetector {
    fun detect(basicContext: FirBasicCompletionContext): FirPositionCompletionContext {
        val position = basicContext.parameters.position
        val reference = (position.parent as? KtSimpleNameExpression)?.mainReference
            ?: return FirUnknownPositionContext(position)
        val nameExpression = reference.expression.takeIf { it !is KtLabelReferenceExpression }
            ?: return FirUnknownPositionContext(position)
        val explicitReceiver = nameExpression.getReceiverExpression()
        return FirNameReferencePositionContext(position, reference, nameExpression, explicitReceiver)
    }

    inline fun analyseInContext(
        basicContext: FirBasicCompletionContext,
        positionContext: FirPositionCompletionContext,
        action: KtAnalysisSession.() -> Unit
    ) {
        return when (positionContext) {
            is FirNameReferencePositionContext -> analyseInDependedAnalysisSession(
                basicContext.originalKtFile,
                positionContext.nameExpression,
                action
            )
            is FirUnknownPositionContext -> {
                analyse(basicContext.originalKtFile, action)
            }
        }
    }
}