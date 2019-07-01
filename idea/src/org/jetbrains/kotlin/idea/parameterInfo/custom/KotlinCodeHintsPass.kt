/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo.custom

import com.intellij.codeHighlighting.EditorBoundHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.hints.InlayParameterHintsExtension
import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.parameterInfo.HintType
import org.jetbrains.kotlin.idea.parameterInfo.TYPE_INFO_PREFIX
import org.jetbrains.kotlin.idea.parameterInfo.provideLambdaReturnValueHints
import org.jetbrains.kotlin.psi.KtExpression
import java.util.*

class KotlinCodeHintsPass(private val myRootElement: PsiElement, editor: Editor) :
    EditorBoundHighlightingPass(editor, myRootElement.containingFile, true) {

    private val myTraverser: SyntaxTraverser<PsiElement> = SyntaxTraverser.psiTraverser(myRootElement)

    override fun doCollectInformation(progress: ProgressIndicator) {
        if (myFile.language != KotlinLanguage.INSTANCE) return
        if (myDocument == null) return

        val kotlinCodeHintsModel = KotlinCodeHintsModel.getInstance(myRootElement.project)

        val provider = InlayParameterHintsExtension.forLanguage(KotlinLanguage.INSTANCE)
        if (provider == null || !provider.canShowHintsWhenDisabled() && !isEnabled || DiffUtil.isDiffEditor(myEditor)) {
            kotlinCodeHintsModel.removeAll(myDocument)
            return
        }

        if (HintType.LAMBDA_RETURN_EXPRESSION.enabled) {
            val actualHints = HashMap<PsiElement, String>()
            myTraverser.forEach { element -> processLambdaReturnHints(element, actualHints) }

            kotlinCodeHintsModel.update(myDocument, actualHints)
        } else {
            kotlinCodeHintsModel.removeAll(myDocument)
        }
    }

    private fun processLambdaReturnHints(element: PsiElement, actualElements: MutableMap<PsiElement, String>) {
        if (element !is KtExpression) return

        for (returnHint in provideLambdaReturnValueHints(element)) {
            val offset = returnHint.offset

            if (!canShowHintsAtOffset(offset)) continue

            actualElements[element] = returnHint.text.substringAfter(TYPE_INFO_PREFIX)
        }
    }

    override fun doApplyInformationToEditor() {
        // Information will be painted with org.jetbrains.kotlin.idea.parameterInfo.custom.ReturnHintLinePainter
    }

    /**
     * Adding hints on the borders of root element (at startOffset or endOffset)
     * is allowed only in the case when root element is a document
     *
     * @return true iff a given offset can be used for hint rendering
     */
    private fun canShowHintsAtOffset(offset: Int): Boolean {
        val rootRange = myRootElement.textRange

        if (rootRange.startOffset < offset && offset < rootRange.endOffset) {
            return true
        }

        return myDocument != null && myDocument.textLength == rootRange.length
    }

    companion object {
        class Factory(registrar: TextEditorHighlightingPassRegistrar) : ProjectComponent, TextEditorHighlightingPassFactory {
            init {
                registrar.registerTextEditorHighlightingPass(this, null, null, false, -1)
            }

            override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
                if (file.language != KotlinLanguage.INSTANCE) return null
                return KotlinCodeHintsPass(file, editor)
            }
        }

        private val isEnabled: Boolean
            get() =
                EditorSettingsExternalizable.getInstance().isShowParameterNameHints
    }
}