/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.context

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyseInFakeAnalysisSession
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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

        return when (val reference = (position.parent as? KtSimpleNameExpression)?.mainReference) {
            null -> FirUnknownPositionContext(position)
            else -> {
                val nameExpression = reference.expression.takeIf { it !is KtLabelReferenceExpression }
                    ?: return FirUnknownPositionContext(position)
                val explicitReceiver = nameExpression.getReceiverExpression()
                FirNameReferencePositionContext(position, reference, nameExpression, explicitReceiver)
            }
        }
    }

    inline fun analyseInContext(
        basicContext: FirBasicCompletionContext,
        positionContext: FirPositionCompletionContext,
        action: KtAnalysisSession.() -> Unit
    ) {
        return when (positionContext) {
            is FirNameReferencePositionContext -> analyseInFakeAnalysisSession(
                basicContext.originalKtFile,
                positionContext.nameExpression,
                action
            )
            is FirUnknownPositionContext -> {
                // TODO
            }
        }
    }
}