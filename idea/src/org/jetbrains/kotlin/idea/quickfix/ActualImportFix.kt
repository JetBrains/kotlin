/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.codeInsight.KotlinCodeInsightWorkspaceSettings
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

internal class ImportFix(expression: KtSimpleNameExpression): AbstractImportFix(expression, MyFactory) {
    override fun fixSilently(editor: Editor): Boolean {
        if (isOutdated()) return false
        val element = element ?: return false
        val project = element.project
        if (!KotlinCodeInsightWorkspaceSettings.getInstance(project).addUnambiguousImportsOnTheFly) return false
        val addImportAction = createAction(project, editor, element)
        if (addImportAction.isUnambiguous()) {
            addImportAction.execute()
            return true
        }
        return false
    }

    companion object MyFactory : Factory() {
        override fun createImportAction(diagnostic: Diagnostic) =
            (diagnostic.psiElement as? KtSimpleNameExpression)?.let(::ImportFix)
    }
}