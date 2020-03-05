/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.util.addConstructorKeyword
import org.jetbrains.kotlin.idea.util.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

class MissingConstructorKeywordFix(element: KtPrimaryConstructor) : KotlinQuickFixAction<KtPrimaryConstructor>(element), CleanupFix {
    override fun getFamilyName(): String = text
    override fun getText(): String = "Add 'constructor' keyword"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.addConstructorKeyword()
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? =
            diagnostic.createIntentionForFirstParentOfType(::MissingConstructorKeywordFix)
    }
}
