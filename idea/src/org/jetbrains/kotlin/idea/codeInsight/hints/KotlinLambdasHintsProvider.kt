/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.hints

import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.util.application.invokeLater

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
                editor.inlayModel.getAfterLineEndElementsInRange(p.offset, p.offset).singleOrNull()?.dispose()
                editor.inlayModel.addAfterLineEndElement(p.offset, p.relatesToPrecedingText, PresentationRenderer(p.presentation))
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
}