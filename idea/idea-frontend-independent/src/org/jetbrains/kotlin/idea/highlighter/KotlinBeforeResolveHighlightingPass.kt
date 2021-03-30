/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeHighlighting.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.psi.KtFile

class KotlinBeforeResolveHighlightingPass(file: KtFile, document: Document) : AbstractHighlightingPassBase(file, document) {

    override fun runAnnotatorWithContext(element: PsiElement, holder: AnnotationHolder) {
        val visitor = BeforeResolveHighlightingVisitor(holder)
        val extensions = EP_NAME.extensionList.map { it.createVisitor(holder) }

        element.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                element.accept(visitor)
                extensions.forEach(element::accept)
                super.visitElement(element)
            }
        })
    }

    class Factory : TextEditorHighlightingPassFactory {
        override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
            if (file !is KtFile) return null
            return KotlinBeforeResolveHighlightingPass(file, editor.document)
        }
    }

    class Registrar : TextEditorHighlightingPassFactoryRegistrar {
        override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
            registrar.registerTextEditorHighlightingPass(
                Factory(),
                /* anchor = */ TextEditorHighlightingPassRegistrar.Anchor.BEFORE,
                /* anchorPassId = */ Pass.UPDATE_FOLDING,
                /* needAdditionalIntentionsPass = */ false,
                /* inPostHighlightingPass = */ false
            )
        }
    }

    companion object {
        val EP_NAME = ExtensionPointName.create<BeforeResolveHighlightingExtension>("org.jetbrains.kotlin.beforeResolveHighlightingVisitor")
    }
}

interface BeforeResolveHighlightingExtension {
    fun createVisitor(holder: AnnotationHolder): AbstractHighlightingVisitor
}