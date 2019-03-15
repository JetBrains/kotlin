/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSignedOrUnsignedNumberType

class NumberConversionFix(
    element: KtExpression,
    type: KotlinType,
    private val disableIfAvailable: IntentionAction? = null
) : KotlinQuickFixAction<KtExpression>(element) {
    private val isConversionAvailable: Boolean = run {
        val expressionType = element.analyze(BodyResolveMode.PARTIAL).getType(element)
        expressionType != null && expressionType != type &&
                expressionType.isSignedOrUnsignedNumberType() && type.isSignedOrUnsignedNumberType()
    }
    private val typePresentation = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type)

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile) =
        disableIfAvailable?.isAvailable(project, editor, file) != true && isConversionAvailable

    override fun getFamilyName() = "Insert number conversion"
    override fun getText() = "Convert expression to '$typePresentation'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val expressionToInsert = KtPsiFactory(file).createExpressionByPattern("$0.to$1()", element, typePresentation)
        val newExpression = element.replaced(expressionToInsert)
        editor?.caretModel?.moveToOffset(newExpression.endOffset)
    }
}