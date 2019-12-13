/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors.CAST_NEVER_SUCCEEDS
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.typeBinding.createTypeBinding
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberType

class ReplacePrimitiveCastWithNumberConversionFix(
    element: KtBinaryExpressionWithTypeRHS,
    private val targetShortType: String
) : KotlinQuickFixAction<KtBinaryExpressionWithTypeRHS>(element) {

    override fun getText() = "Replace cast with call to 'to$targetShortType()'"
    override fun getFamilyName() = "Replace cast with primitive conversion method"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val factory = KtPsiFactory(element)
        val replaced = element.replaced(factory.createExpressionByPattern("$0.to$1()", element.left, targetShortType))

        editor?.caretModel?.moveToOffset(replaced.endOffset)
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = CAST_NEVER_SUCCEEDS.cast(diagnostic).psiElement as? KtOperationReferenceExpression ?: return null
            val binaryExpression = element.parent as? KtBinaryExpressionWithTypeRHS ?: return null

            val context = binaryExpression.analyze()

            val expressionType = binaryExpression.left.getType(context) ?: return null
            if (!expressionType.isPrimitiveNumberType()) return null

            val castType = binaryExpression.right?.createTypeBinding(context)?.type ?: return null
            if (!castType.isPrimitiveNumberType()) return null

            return ReplacePrimitiveCastWithNumberConversionFix(
                binaryExpression,
                SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(castType)
            )
        }
    }
}