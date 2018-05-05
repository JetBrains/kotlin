/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.annotations.JVM_DEFAULT_FQ_NAME

class AddJvmDefaultAnnotation(declaration: KtCallableDeclaration) : KotlinQuickFixAction<KtCallableDeclaration>(declaration) {
    override fun getText(): String = "Add '@JvmDefault' annotation"

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.addAnnotation(JVM_DEFAULT_FQ_NAME)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val psiElement = diagnostic.psiElement
            if (psiElement is KtNamedFunction || psiElement is KtProperty)
                return AddJvmDefaultAnnotation(psiElement as KtCallableDeclaration)
            return null
        }
    }
}