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

package org.jetbrains.kotlin.idea.liveTemplates

import com.intellij.codeInsight.template.impl.TemplateOptionalProcessor
import com.intellij.openapi.project.Project
import com.intellij.codeInsight.template.Template
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.Editor
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.core.ShortenReferences
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.kotlin.psi.KtFile

class KotlinShortenFQNamesProcessor : TemplateOptionalProcessor {
    override fun processText(project: Project, template: Template, document: Document, templateRange: RangeMarker, editor: Editor) {
        if (!template.isToShortenLongNames) return

        PsiDocumentManager.getInstance(project).commitDocument(document)

        val file = PsiUtilBase.getPsiFileInEditor(editor, project) as? KtFile ?: return
        ShortenReferences.DEFAULT.process(file, templateRange.startOffset, templateRange.endOffset)

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
    }

    override fun getOptionName(): String {
        return CodeInsightBundle.message("dialog.edit.template.checkbox.shorten.fq.names")!!
    }

    override fun isEnabled(template: Template) = template.isToShortenLongNames

    override fun setEnabled(template: Template, value: Boolean) {
    }

    override fun isVisible(template: Template) = false
}
