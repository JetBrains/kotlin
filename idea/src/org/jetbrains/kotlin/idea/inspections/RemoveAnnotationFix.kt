/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile

class RemoveAnnotationFix(private val text: String, annotationEntry: KtAnnotationEntry) :
    KotlinQuickFixAction<KtAnnotationEntry>(annotationEntry) {

    override fun getText() = text

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.delete()
    }

    object JvmOverloads : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): RemoveAnnotationFix? {
            val annotationEntry = diagnostic.psiElement as? KtAnnotationEntry ?: return null
            return RemoveAnnotationFix("Remove @JvmOverloads annotation", annotationEntry)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): RemoveAnnotationFix? {
            val annotationEntry = diagnostic.psiElement as? KtAnnotationEntry ?: return null
            return RemoveAnnotationFix("Remove annotation", annotationEntry = annotationEntry)
        }
    }
}