/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.idea.codeInsight.hints

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.settings.InlayHintsConfigurable
import com.intellij.ui.layout.panel
import org.jetbrains.kotlin.idea.KotlinBundle
import javax.swing.JComponent


typealias CompatibleInlayHintsConfigurable = InlayHintsConfigurable

fun createLambdaHintsImmediateConfigurable(settings: KotlinLambdasHintsProvider.Settings): ImmediateConfigurable {
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

fun createTypeHintsImmediateConfigurable(settings: KotlinReferencesTypeHintsProvider.Settings): ImmediateConfigurable {
    return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = panel {}

            override val mainCheckboxText: String = KotlinBundle.message("hints.settings.common.items")

            override val cases: List<ImmediateConfigurable.Case>
                get() = listOf(
                    ImmediateConfigurable.Case(
                        KotlinBundle.message("hints.settings.types.property"),
                        "hints.type.property",
                        settings::propertyType
                    ),
                    ImmediateConfigurable.Case(
                        KotlinBundle.message("hints.settings.types.variable"),
                        "hints.type.variable",
                        settings::localVariableType
                    ),
                    ImmediateConfigurable.Case(
                        KotlinBundle.message("hints.settings.types.return"),
                        "hints.type.function.return",
                        settings::functionReturnType
                    ),
                    ImmediateConfigurable.Case(
                        KotlinBundle.message("hints.settings.types.parameter"),
                        "hints.type.function.parameter",
                        settings::parameterType
                    ),
                )
        }
}