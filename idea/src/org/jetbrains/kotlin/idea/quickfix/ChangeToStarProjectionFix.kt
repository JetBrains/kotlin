/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class ChangeToStarProjectionFix(element: KtTypeElement) : KotlinQuickFixAction<KtTypeElement>(element) {
    override fun getFamilyName() = KotlinBundle.message("fix.change.to.star.projection.family")

    override fun getText(): String {
        val element = this.element

        return when {
            element != null -> {
                val type = element.typeArgumentsAsTypes.joinToString { "*" }
                KotlinBundle.message("fix.change.to.star.projection.text", "<$type>")
            }
            else -> null
        } ?: ""
    }

    public override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val star = KtPsiFactory(file).createStar()
        element.typeArgumentsAsTypes.forEach { it?.replace(star) }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val binaryExpr = diagnostic.psiElement.getNonStrictParentOfType<KtBinaryExpressionWithTypeRHS>()
            val typeReference = binaryExpr?.right ?: diagnostic.psiElement.getNonStrictParentOfType<KtTypeReference>()
            val typeElement = typeReference?.typeElement ?: return null
            if (typeElement is KtFunctionType) return null

            if (binaryExpr?.operationReference?.isAsKeyword() == true) {
                val parent = binaryExpr.getParentOfTypes(
                    true,
                    KtValueArgument::class.java,
                    KtQualifiedExpression::class.java,
                    KtCallableDeclaration::class.java
                )
                if (parent is KtCallableDeclaration
                    && parent.typeReference.typeArguments().any { it.projectionKind != KtProjectionKind.STAR }
                    && typeReference.typeArguments().isNotEmpty()
                    && binaryExpr.isUsedAsExpression(binaryExpr.analyze(BodyResolveMode.PARTIAL_WITH_CFA))
                ) return null
                val type = when (parent) {
                    is KtValueArgument -> {
                        val callExpr = parent.getStrictParentOfType<KtCallExpression>()
                        (callExpr?.resolveToCall()?.getArgumentMapping(parent) as? ArgumentMatch)?.valueParameter?.original?.type
                    }
                    is KtQualifiedExpression ->
                        if (KtPsiUtil.safeDeparenthesize(parent.receiverExpression) == binaryExpr)
                            parent.resolveToCall()?.resultingDescriptor?.extensionReceiverParameter?.value?.original?.type
                        else
                            null
                    else ->
                        null
                }
                if (type?.arguments?.any { !it.isStarProjection && !it.type.isTypeParameter() } == true) return null
            }

            if (typeElement.typeArgumentsAsTypes.isNotEmpty()) {
                return ChangeToStarProjectionFix(typeElement)
            }
            return null
        }

        private fun KtSimpleNameExpression.isAsKeyword(): Boolean {
            val elementType = getReferencedNameElementType()
            return elementType == KtTokens.AS_KEYWORD || elementType == KtTokens.AS_SAFE
        }

        private fun KtTypeReference?.typeArguments(): List<KtTypeProjection> {
            return (this?.typeElement as? KtUserType)?.typeArguments.orEmpty()
        }
    }
}
