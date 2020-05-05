/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.hints

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.ui.layout.panel
import org.jetbrains.kotlin.idea.KotlinBundle
import javax.swing.JComponent

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

    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = panel {}

            override val mainCheckboxText: String = KotlinBundle.message("hints.settings.common.items")

            override val cases: List<ImmediateConfigurable.Case>
                get() = listOf(
                    ImmediateConfigurable.Case(
                        KotlinBundle.message("hints.settings.lambda.return"),
                        "hints.lambda.return",
                        settings::returnExpressions
                    ),
                    ImmediateConfigurable.Case(
                        KotlinBundle.message("hints.settings.lambda.receivers.parameters"),
                        "hints.lambda.receivers.parameters",
                        settings::implicitReceiversAndParams
                    )
                )
        }
    }

    override fun createSettings(): Settings = Settings()
}