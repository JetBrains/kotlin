/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.psiUtil.parents

class MoveTypeAliasToTopLevelFix(element: KtTypeAlias) : KotlinQuickFixAction<KtTypeAlias>(element) {
    override fun getText() = KotlinBundle.message("fix.move.typealias.to.top.level")

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val typeAlias = element ?: return
        val parents = typeAlias.parents.toList().reversed()
        val containingFile = parents.firstOrNull() as? KtFile ?: return
        val target = parents.getOrNull(1) ?: return
        containingFile.addAfter(typeAlias, target)
        containingFile.addAfter(KtPsiFactory(typeAlias).createNewLine(2), target)
        typeAlias.delete()
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) = (diagnostic.psiElement as? KtTypeAlias)?.let { MoveTypeAliasToTopLevelFix(it) }
    }
}
