/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.hints

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.ui.components.JBCheckBox
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
        return createTypeHintsImmediateConfigurable(settings)
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

    override val previewText: String? = """
        val property = listOf(1, 2, 3).filter { num -> num % 2 == 0 }
        
        fun someFun(arg: Int) = print(arg)
        
        fun anotherFun(a: Int = 10, b: Int = 5): Int {
            val variable = a + b
            return variable * 2
        }

        fun yetAnotherFun() {
            Stream.of(1, 2, 3)
                .map { i -> i + 12 }
                .filter { i -> i % 2 == 0 }
                .collect(Collectors.toList())
        }
    """.trimIndent()
}