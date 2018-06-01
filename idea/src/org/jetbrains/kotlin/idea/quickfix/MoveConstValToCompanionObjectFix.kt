/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.intentions.MoveMemberToCompanionObjectIntention
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class MoveConstValToCompanionObjectFix(property: KtProperty) : KotlinQuickFixAction<KtProperty>(property) {

    override fun getText() = "Move to companion object"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val property = element ?: return
        MoveMemberToCompanionObjectIntention().applyTo(property, editor)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtProperty>? {
            val property = diagnostic.psiElement.getStrictParentOfType<KtProperty>() ?: return null
            if (MoveMemberToCompanionObjectIntention().applicabilityRange(property) == null) return null
            return MoveConstValToCompanionObjectFix(property)
        }
    }

}