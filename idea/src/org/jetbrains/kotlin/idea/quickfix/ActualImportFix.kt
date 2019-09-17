/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

internal class ImportFix(expression: KtSimpleNameExpression): AbstractImportFix(expression, MyFactory) {
    override fun fixSilently(editor: Editor): Boolean {
        if (!CodeInsightSettings.getInstance().ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY) return false
        if (isOutdated()) return false
        val element = element ?: return false
        val addImportAction = createAction(element.project, editor, element)
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