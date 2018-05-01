package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

class TooLongCharLiteralToStringFix(
    element: KtConstantExpression
) : KotlinQuickFixAction<KtConstantExpression>(element) {
    override fun getText(): String = "Convert too long character literal to string"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val text = element.text
        if (!(text.startsWith("'") && text.endsWith("'") && text.length >= 2)) {
            return
        }

        val newStringContent = text
            .slice(1..text.length - 2)
            .replace("\\\"", "\"")
            .replace("\"", "\\\"")
        val newElement = KtPsiFactory(element).createStringTemplate(newStringContent)

        element.replace(newElement)
    }

    override fun getFamilyName(): String = text

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.psiElement as? KtConstantExpression ?: return null
            if (element.text == "'\\'") return null
            return TooLongCharLiteralToStringFix(element = element)
        }
    }
}