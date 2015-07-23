/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
import com.intellij.lang.annotation.AnnotationSession
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.idea.kdoc.KDocHighlightingVisitor
import org.jetbrains.kotlin.psi.JetFile

public class KotlinBeforeResolveHighlightingPass(
        private val file: JetFile,
        document: Document
) : TextEditorHighlightingPass(file.project, document), DumbAware {

    private volatile var annotationHolder: AnnotationHolderImpl? = null

    override fun doCollectInformation(progress: ProgressIndicator) {
        val annotationHolder = AnnotationHolderImpl(AnnotationSession(file))
        val visitors = listOf(
                SoftKeywordsHighlightingVisitor(annotationHolder),
                LabelsHighlightingVisitor(annotationHolder),
                KDocHighlightingVisitor(annotationHolder)
        )
        file.accept(object : PsiRecursiveElementVisitor(){
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                visitors.forEach { element.accept(it) }
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

    public class Factory(project: Project, registrar: TextEditorHighlightingPassRegistrar) : AbstractProjectComponent(project), TextEditorHighlightingPassFactory {
        init {
            registrar.registerTextEditorHighlightingPass(this, TextEditorHighlightingPassRegistrar.Anchor.BEFORE, Pass.UPDATE_FOLDING, false, false)
        }

        override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
            if (file !is JetFile) return null
            return KotlinBeforeResolveHighlightingPass(file, editor.document)
        }
    }
}
