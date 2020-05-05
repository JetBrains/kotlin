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
class KotlinReferencesTypeHintsProvider : KotlinAbstractHintsProvider<KotlinReferencesTypeHintsProvider.Settings>() {

    data class Settings(
        var propertyType: Boolean = false,
        var localVariableType: Boolean = false,
        var functionReturnType: Boolean = false,
        var parameterType: Boolean = false
    )

    override val name: String = KotlinBundle.message("hints.settings.types")

    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
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

    override fun createSettings(): Settings = Settings()

    override fun isElementSupported(resolved: HintType?, settings: Settings): Boolean {
        return when (resolved) {
            HintType.PROPERTY_HINT -> settings.propertyType
            HintType.LOCAL_VARIABLE_HINT -> settings.localVariableType
            HintType.FUNCTION_HINT -> settings.functionReturnType
            HintType.PARAMETER_TYPE_HINT -> settings.parameterType
            else -> false
        }
    }
}