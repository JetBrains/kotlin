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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors.CAST_NEVER_SUCCEEDS
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES
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

            return ReplacePrimitiveCastWithNumberConversionFix(binaryExpression, SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(castType))
        }
    }
}