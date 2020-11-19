/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeHighlighting.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection
import org.jetbrains.kotlin.idea.isMainFunction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter

open class KotlinHighlightingPass(file: KtFile, document: Document) : AbstractKotlinHighlightingPass(file, document) {

    override fun shouldSuppressUnusedParameter(parameter: KtParameter): Boolean {
        val grandParent = parameter.parent.parent as? KtNamedFunction ?: return false
        if (!UnusedSymbolInspection.isEntryPoint(grandParent)) return false
        return !grandParent.isMainFunction()
    }

    class Factory : TextEditorHighlightingPassFactory {
        override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
            if (file !is KtFile) return null
            return KotlinHighlightingPass(file, editor.document)
        }
    }

    class Registrar : TextEditorHighlightingPassFactoryRegistrar {
        override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
            registrar.registerTextEditorHighlightingPass(
                Factory(),
                /* runAfterCompletionOf = */ null,
                /* runAfterStartingOf = */ intArrayOf(Pass.UPDATE_ALL),
                /* runIntentionsPassAfter = */false,
                /* forcedPassId = */-1
            )
        }
    }
}