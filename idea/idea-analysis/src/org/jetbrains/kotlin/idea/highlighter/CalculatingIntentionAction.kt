/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.intention.AbstractEmptyIntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinIdeaAnalysisBundle
import javax.swing.Icon

class CalculatingIntentionAction : AbstractEmptyIntentionAction(), LowPriorityAction, Iconable {
    override fun getText(): String = KotlinIdeaAnalysisBundle.message("intention.calculating.text")

    override fun getFamilyName(): String = KotlinIdeaAnalysisBundle.message("intention.calculating.text")

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = true

    override fun equals(other: Any?): Boolean = this === other || other is CalculatingIntentionAction

    override fun hashCode(): Int = 42

    override fun getIcon(@Iconable.IconFlags flags: Int): Icon = AllIcons.Actions.Preview
}
