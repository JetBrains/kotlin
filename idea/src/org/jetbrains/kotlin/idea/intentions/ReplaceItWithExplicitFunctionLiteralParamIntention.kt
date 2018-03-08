/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.rename.KotlinVariableInplaceRenameHandler
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class ReplaceItWithExplicitFunctionLiteralParamIntention : SelfTargetingOffsetIndependentIntention<KtNameReferenceExpression> (
        KtNameReferenceExpression::class.java, "Replace 'it' with explicit parameter"
), LowPriorityAction {
    override fun isApplicableTo(element: KtNameReferenceExpression)
            = isAutoCreatedItUsage(element)

    override fun applyTo(element: KtNameReferenceExpression, editor: Editor?) {
        if (editor == null) throw IllegalArgumentException("This intention requires an editor")
        val target = element.mainReference.resolveToDescriptors(element.analyze()).single()

        val functionLiteral = DescriptorToSourceUtils.descriptorToDeclaration(target.containingDeclaration!!) as KtFunctionLiteral

        val newExpr = KtPsiFactory(element).createExpression("{ it -> }") as KtLambdaExpression
        functionLiteral.addRangeAfter(
                newExpr.functionLiteral.valueParameterList,
                newExpr.functionLiteral.arrow!!,
                functionLiteral.lBrace)
        PsiDocumentManager.getInstance(element.project).doPostponedOperationsAndUnblockDocument(editor.document)

        val paramToRename = functionLiteral.valueParameters.single()
        editor.caretModel.moveToOffset(element.textOffset)
        KotlinVariableInplaceRenameHandler().doRename(paramToRename, editor, null)
    }
}
