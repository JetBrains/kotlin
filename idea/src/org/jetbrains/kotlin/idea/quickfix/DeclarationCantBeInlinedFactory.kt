/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.intentions.declarations.ConvertMemberToExtensionIntention
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClass

object DeclarationCantBeInlinedFactory : KotlinIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val function = diagnostic.psiElement as? KtNamedFunction ?: return emptyList()
        val containingClass = function.containingClass() ?: return emptyList()
        val fixes = mutableListOf<IntentionAction>()
        if (containingClass.isInterface()) {
            fixes.add(ConvertMemberToExtensionFix(function))
        } else if (function.hasModifier(KtTokens.OPEN_KEYWORD)) {
            fixes.add(RemoveModifierFix(function, KtTokens.OPEN_KEYWORD, false))
        }
        return fixes
    }
}

private class ConvertMemberToExtensionFix(element: KtNamedFunction) : KotlinQuickFixAction<KtNamedFunction>(element) {
    override fun getText() = "Convert member to extension"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.also { ConvertMemberToExtensionIntention.convert(it) }
    }
}
