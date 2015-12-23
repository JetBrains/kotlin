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

import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.refactoring.ValVarExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

class AddValVarToConstructorParameterIntention : SelfTargetingRangeIntention<KtParameter>(KtParameter::class.java,
                                                                                          "Add val/var to primary constructor parameter") {
    override fun applicabilityRange(element: KtParameter): TextRange? {
        if (element.valOrVarKeyword != null) return null
        if ((element.parent as? KtParameterList)?.parent !is KtPrimaryConstructor) return null
        text = "Add val/var to parameter '${element.name ?: ""}'"
        return element.nameIdentifier?.textRange
    }

    override fun applyTo(element: KtParameter, editor: Editor) {
        val project = element.project

        element.addAfter(KtPsiFactory(project).createValKeyword(), null)

        val parameter = element.createSmartPointer().let {
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
            it.element
        } ?: return

        TemplateBuilderImpl(parameter)
                .apply { replaceElement(parameter.valOrVarKeyword!!, ValVarExpression) }
                .buildInlineTemplate()
                .let { TemplateManager.getInstance(project).startTemplate(editor, it) }
    }
}