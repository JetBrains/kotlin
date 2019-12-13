/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.migration

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class MigrateTypeParameterListFix(typeParameterList: KtTypeParameterList) : KotlinQuickFixAction<KtTypeParameterList>(typeParameterList),
    CleanupFix {

    override fun getFamilyName(): String = "Migrate type parameter list syntax"
    override fun getText(): String = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val function = element.getStrictParentOfType<KtNamedFunction>() ?: return
        function.addBefore(element, function.receiverTypeReference ?: function.nameIdentifier)
        element.delete()
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val typeParameterList = diagnostic.psiElement as? KtTypeParameterList ?: return null
            return MigrateTypeParameterListFix(typeParameterList)
        }
    }
}
