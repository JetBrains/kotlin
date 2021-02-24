/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.hints

import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.util.application.invokeLater
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import java.awt.Graphics
import java.awt.Rectangle

/**
 * Please note that the executable test class is currently not generated. The thing is that [KotlinLambdasHintsProvider] utilizes a hack
 * preventing it from being testable (see [handlePresentations]). Nevertheless [AbstractKotlinLambdasHintsProvider] and corresponding
 * [testData][idea/testData/codeInsight/hints/lambda] exist.

 * To run the tests in "imaginary" environment (might still be valuable):
 * 1. Comment out [handlePresentations]
 * 2. Add the following code snippet next to the similar ones at [GenerateTests.kt]:
 *   ```
 *   testClass<AbstractKotlinLambdasHintsProvider> {
 *      model("codeInsight/hints/lambda")
 *   }
 *  ```
 *  3. Run "Generate All Tests". You're expected to get `KotlinLambdasHintsProviderGenerated` class.
 */
@Suppress("UnstableApiUsage")
class KotlinLambdasHintsProvider : KotlinAbstractHintsProvider<KotlinLambdasHintsProvider.Settings>() {

    data class Settings(
        var returnExpressions: Boolean = true,
        var implicitReceiversAndParams: Boolean = true,
    )

    override val name: String = KotlinBundle.message("hints.settings.lambdas")

    override fun isElementSupported(resolved: HintType?, settings: Settings): Boolean {
        return when (resolved) {
            HintType.LAMBDA_RETURN_EXPRESSION -> settings.returnExpressions
            HintType.LAMBDA_IMPLICIT_PARAMETER_RECEIVER -> settings.implicitReceiversAndParams
            else -> false
        }
    }

    override fun handlePresentations(presentations: List<PresentationAndSettings>, editor: Editor, sink: InlayHintsSink) {
        // sink should remain empty for the outer infrastructure - we place hints ourselves
        invokeLater {
            presentations.forEach { p ->
                val logicalLine = editor.offsetToLogicalPosition(p.offset).line
                editor.inlayModel.getAfterLineEndElementsForLogicalLine(logicalLine)
                    .filter { it.renderer is LambdaHintsRenderer }
                    .forEach { it.dispose() }
                editor.inlayModel.addAfterLineEndElement(p.offset, p.relatesToPrecedingText, LambdaHintsRenderer(p.presentation))
            }
        }
    }

    override fun handleAfterLineEndHintsRemoval(editor: Editor, resolved: HintType, element: PsiElement) {
        invokeLater {
            val offset = when (resolved) {
                HintType.LAMBDA_IMPLICIT_PARAMETER_RECEIVER -> {
                    val lambdaExpression = (element as? KtFunctionLiteral)?.parent as? KtLambdaExpression
                    lambdaExpression?.leftCurlyBrace?.textRange?.endOffset
                }
                HintType.LAMBDA_RETURN_EXPRESSION -> (element as? KtExpression)?.endOffset
                else -> null
            }

            offset?.let {
                val logicalLine = editor.offsetToLogicalPosition(offset).line
                editor.inlayModel.getAfterLineEndElementsForLogicalLine(logicalLine)
                    .filter { it.renderer is LambdaHintsRenderer }
                    .forEach { it.dispose() }
            }
        }
    }

    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
        return createLambdaHintsImmediateConfigurable(settings)
    }

    override fun createSettings(): Settings = Settings()

    override val previewText: String? = """
        val lambda = { i: Int ->
            i + 10
            i + 20
        }

        fun someFun() {    
            GlobalScope.launch {
                // someSuspendingFun()
            }
        }
    """.trimIndent()


    /**
     * This renderer is not more than just a filter criterion. [PresentationRenderer] is not extensible, instead delegation is used.
     */
    private class LambdaHintsRenderer(presentation: InlayPresentation) : EditorCustomElementRenderer {
        private val delegate: PresentationRenderer = PresentationRenderer(presentation)

        override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
            delegate.paint(inlay, g, targetRegion, textAttributes)
        }

        override fun calcWidthInPixels(inlay: Inlay<*>): Int {
            return delegate.calcWidthInPixels(inlay)
        }

        // this should not be shown anywhere
        override fun getContextMenuGroupId(inlay: Inlay<*>): String {
            return "DummyActionGroup"
        }

        override fun toString(): String {
            return delegate.toString()
        }
    }
}