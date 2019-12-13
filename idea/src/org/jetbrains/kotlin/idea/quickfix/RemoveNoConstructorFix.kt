/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class RemoveNoConstructorFix(constructor: KtValueArgumentList) : KotlinQuickFixAction<KtValueArgumentList>(constructor) {

    override fun getText() = "Remove constructor call"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val superTypeCallEntry = element?.getStrictParentOfType<KtSuperTypeCallEntry>() ?: return
        val superTypeEntry = KtPsiFactory(project).createSuperTypeEntry(superTypeCallEntry.firstChild.text)
        superTypeCallEntry.replaced(superTypeEntry)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtValueArgumentList>? =
            (diagnostic.psiElement as? KtValueArgumentList)?.let { RemoveNoConstructorFix(it) }
    }

}
