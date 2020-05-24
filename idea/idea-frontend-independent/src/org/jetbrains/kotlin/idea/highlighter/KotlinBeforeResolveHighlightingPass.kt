/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeHighlighting.*
import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
import com.intellij.lang.annotation.AnnotationSession
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class KotlinBeforeResolveHighlightingPass(
    private val file: KtFile,
    document: Document
) : TextEditorHighlightingPass(file.project, document), DumbAware {

    @Volatile
    private var annotationHolder: AnnotationHolderImpl? = null

    override fun doCollectInformation(progress: ProgressIndicator) {
        val annotationHolder = AnnotationHolderImpl(AnnotationSession(file))
        val visitor = BeforeResolveHighlightingVisitor(annotationHolder)
        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                element.accept(visitor)
            }
        })
        this.annotationHolder = annotationHolder
    }

    override fun doApplyInformationToEditor() {
        if (annotationHolder == null) return

        val infos = annotationHolder!!.map { HighlightInfo.fromAnnotation(it) }

        UpdateHighlightersUtil.setHighlightersToEditor(myProject, myDocument!!, 0, file.textLength, infos, colorsScheme, id)
        annotationHolder = null
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
                TextEditorHighlightingPassRegistrar.Anchor.BEFORE,
                Pass.UPDATE_FOLDING,
                false,
                false
            )
        }
    }

//    companion object {
//        val EP_NAME = ExtensionPointName.create<HighlightingVisitor>("org.jetbrains.kotlin.beforeResolveHighlightingVisitor")
//    }
}
