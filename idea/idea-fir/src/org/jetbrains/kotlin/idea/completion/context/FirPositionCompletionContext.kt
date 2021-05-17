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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression

internal sealed class FirRawPositionCompletionContext {
    abstract val position: PsiElement
}

internal sealed class FirNameReferencePositionContext : FirRawPositionCompletionContext() {
    abstract val reference: KtSimpleNameReference
    abstract val nameExpression: KtSimpleNameExpression
    abstract val explicitReceiver: KtExpression?
}

internal class FirTypeNameReferencePositionContext(
    override val position: PsiElement,
    override val reference: KtSimpleNameReference,
    override val nameExpression: KtSimpleNameExpression,
    override val explicitReceiver: KtExpression?
) : FirNameReferencePositionContext()

internal class FirAnnotationTypeNameReferencePositionContext(
    override val position: PsiElement,
    override val reference: KtSimpleNameReference,
    override val nameExpression: KtSimpleNameExpression,
    override val explicitReceiver: KtExpression?,
    val annotationEntry: KtAnnotationEntry,
) : FirNameReferencePositionContext()


internal class FirExpressionNameReferencePositionContext(
    override val position: PsiElement,
    override val reference: KtSimpleNameReference,
    override val nameExpression: KtSimpleNameExpression,
    override val explicitReceiver: KtExpression?
) : FirNameReferencePositionContext()


internal class FirUnknownPositionContext(
    override val position: PsiElement
) : FirRawPositionCompletionContext()

internal object FirPositionCompletionContextDetector {
    fun detect(basicContext: FirBasicCompletionContext): FirRawPositionCompletionContext {
        val position = basicContext.parameters.position
        val reference = (position.parent as? KtSimpleNameExpression)?.mainReference
            ?: return FirUnknownPositionContext(position)
        val nameExpression = reference.expression.takeIf { it !is KtLabelReferenceExpression }
            ?: return FirUnknownPositionContext(position)
        val explicitReceiver = nameExpression.getReceiverExpression()

        return when (val parent = nameExpression.parent) {
            is KtUserType -> {
                detectForTypeContext(parent, position, reference, nameExpression, explicitReceiver)
            }
            else -> {
                FirExpressionNameReferencePositionContext(position, reference, nameExpression, explicitReceiver)
            }
        }
    }

    private fun detectForTypeContext(
        userType: KtUserType,
        position: PsiElement,
        reference: KtSimpleNameReference,
        nameExpression: KtSimpleNameExpression,
        explicitReceiver: KtExpression?
    ): FirNameReferencePositionContext {
        val annotationEntry = userType.annotationEntry()
            ?: return FirTypeNameReferencePositionContext(position, reference, nameExpression, explicitReceiver)
        return FirAnnotationTypeNameReferencePositionContext(position, reference, nameExpression, explicitReceiver, annotationEntry)
    }

    private fun KtUserType.annotationEntry(): KtAnnotationEntry? {
        val typeReference = (parent as? KtTypeReference)?.takeIf { it.typeElement == this }
        val constructorCall = (typeReference?.parent as? KtConstructorCalleeExpression)?.takeIf { it.typeReference == typeReference }
        return (constructorCall?.parent as? KtAnnotationEntry)?.takeIf { it.calleeExpression == constructorCall }
    }

    inline fun analyseInContext(
        basicContext: FirBasicCompletionContext,
        positionContext: FirRawPositionCompletionContext,
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