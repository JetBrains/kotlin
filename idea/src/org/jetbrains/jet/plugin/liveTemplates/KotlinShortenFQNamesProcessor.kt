package org.jetbrains.jet.plugin.liveTemplates

import com.intellij.codeInsight.template.impl.TemplateOptionalProcessor
import com.intellij.openapi.project.Project
import com.intellij.codeInsight.template.Template
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.Editor
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.jet.lang.psi.JetFile

public class KotlinShortenFQNamesProcessor : TemplateOptionalProcessor {
    override fun processText(project: Project, template: Template, document: Document, templateRange: RangeMarker, editor: Editor) {
        if (!template.isToShortenLongNames()) return

        PsiDocumentManager.getInstance(project).commitDocument(document)

        val file = PsiUtilBase.getPsiFileInEditor(editor, project) as? JetFile ?: return
        ShortenReferences.process(file, templateRange.getStartOffset(), templateRange.getEndOffset())

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
    }

    override fun getOptionName(): String {
        return CodeInsightBundle.message("dialog.edit.template.checkbox.shorten.fq.names")!!
    }

    override fun isEnabled(template: Template) = template.isToShortenLongNames()

    override fun setEnabled(template: Template, value: Boolean) {
    }

    override fun isVisible(template: Template) = false
}