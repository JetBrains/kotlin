/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.core.dropEnclosingParenthesesIfPossible
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class RemoveUselessCastFix(element: KtBinaryExpressionWithTypeRHS) : KotlinQuickFixAction<KtBinaryExpressionWithTypeRHS>(element),
    CleanupFix {
    override fun getFamilyName() = "Remove useless cast"

    override fun getText(): String = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        invoke(element ?: return)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        operator fun invoke(element: KtBinaryExpressionWithTypeRHS) = dropEnclosingParenthesesIfPossible(element.replaced(element.left))

        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtBinaryExpressionWithTypeRHS>? {
            val expression = diagnostic.psiElement.getNonStrictParentOfType<KtBinaryExpressionWithTypeRHS>() ?: return null
            return RemoveUselessCastFix(expression)
        }
    }
}

