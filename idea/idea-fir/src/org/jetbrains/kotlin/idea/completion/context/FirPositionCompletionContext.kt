/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.context

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
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

internal class FirTypeConstraintNameInWhereClausePositionContext(
    override val position: PsiElement,
    val typeParametersOwner: KtTypeParameterListOwner
) : FirRawPositionCompletionContext()

internal sealed class FirNameReferencePositionContext : FirRawPositionCompletionContext() {
    abstract val reference: KtSimpleNameReference
    abstract val nameExpression: KtSimpleNameExpression
    abstract val explicitReceiver: KtExpression?
}

internal class FirImportDirectivePositionContext(
    override val position: PsiElement,
    override val reference: KtSimpleNameReference,
    override val nameExpression: KtSimpleNameExpression,
    override val explicitReceiver: KtExpression?,
) : FirNameReferencePositionContext()

internal class FirPackageDirectivePositionContext(
    override val position: PsiElement,
    override val reference: KtSimpleNameReference,
    override val nameExpression: KtSimpleNameExpression,
    override val explicitReceiver: KtExpression?,
) : FirNameReferencePositionContext()


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

internal class FirSuperTypeCallNameReferencePositionContext(
    override val position: PsiElement,
    override val reference: KtSimpleNameReference,
    override val nameExpression: KtSimpleNameExpression,
    override val explicitReceiver: KtExpression?,
    val superExpression: KtSuperExpression,
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
        val parent = nameExpression.parent

        return when {
            parent is KtUserType -> {
                detectForTypeContext(parent, position, reference, nameExpression, explicitReceiver)
            }
            nameExpression.isReferenceExpressionInImportDirective() -> {
                FirImportDirectivePositionContext(
                    position,
                    reference,
                    nameExpression,
                    explicitReceiver,
                )
            }

            nameExpression.isImportExpressionInsidePackageDirective() -> {
                FirPackageDirectivePositionContext(
                    position,
                    reference,
                    nameExpression,
                    explicitReceiver,
                )
            }
            parent is KtTypeConstraint -> FirTypeConstraintNameInWhereClausePositionContext(
                position,
                position.parentOfType()!!,
            )
            else -> {
                FirExpressionNameReferencePositionContext(position, reference, nameExpression, explicitReceiver)
            }
        }
    }

    private fun KtExpression.isReferenceExpressionInImportDirective() = when (val parent = parent) {
        is KtImportDirective -> parent.importedReference == this
        is KtDotQualifiedExpression -> {
            val importDirective = parent.parent as? KtImportDirective
            importDirective?.importedReference == parent
        }
        else -> false
    }

    private fun KtExpression.isImportExpressionInsidePackageDirective() = when (val parent = parent) {
        is KtPackageDirective -> parent.packageNameExpression == this
        is KtDotQualifiedExpression -> {
            val packageDirective = parent.parent as? KtPackageDirective
            packageDirective?.packageNameExpression == parent
        }
        else -> false
    }

    private fun detectForTypeContext(
        userType: KtUserType,
        position: PsiElement,
        reference: KtSimpleNameReference,
        nameExpression: KtSimpleNameExpression,
        explicitReceiver: KtExpression?
    ): FirNameReferencePositionContext {
        val typeReference = (userType.parent as? KtTypeReference)?.takeIf { it.typeElement == userType }
        return when (val typeReferenceOwner = typeReference?.parent) {
            is KtConstructorCalleeExpression -> {
                val constructorCall = typeReferenceOwner.takeIf { it.typeReference == typeReference }
                val annotationEntry = (constructorCall?.parent as? KtAnnotationEntry)?.takeIf { it.calleeExpression == constructorCall }
                annotationEntry?.let {
                    FirAnnotationTypeNameReferencePositionContext(position, reference, nameExpression, explicitReceiver, it)
                }
            }
            is KtSuperExpression -> {
                val superTypeCallEntry = typeReferenceOwner.takeIf { it.superTypeQualifier == typeReference }
                superTypeCallEntry?.let {
                    FirSuperTypeCallNameReferencePositionContext(position, reference, nameExpression, explicitReceiver, it)
                }
            }
            else -> null
        } ?: FirTypeNameReferencePositionContext(position, reference, nameExpression, explicitReceiver)
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
            is FirUnknownPositionContext,
            is FirImportDirectivePositionContext,
            is FirPackageDirectivePositionContext,
            is FirTypeConstraintNameInWhereClausePositionContext -> {
                analyse(basicContext.originalKtFile, action)
            }
        }
    }
}