/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import javax.swing.JCheckBox

class KotlinIndentOptionsEditor : SmartIndentOptionsEditor() {
    private val useContinuationIndentInParameterList = JCheckBox("Use continuation indent in parameter lists")
    private val useContinuationIndentForExpressionBodies = JCheckBox("Use continuation indent for expression bodies")
    private val useContinuationIndentForChainedCalls = JCheckBox("Use continuation indent for chained calls")

    override fun addComponents() {
        super.addComponents()
        add(useContinuationIndentInParameterList)
        add(useContinuationIndentForExpressionBodies)
        add(useContinuationIndentForChainedCalls)
    }

    override fun isModified(settings: CodeStyleSettings, options: CommonCodeStyleSettings.IndentOptions): Boolean {
        var isModified = super.isModified(settings, options)
        val kotlinSettings = settings.getCustomSettings(KotlinCodeStyleSettings::class.java)
        isModified = isModified || IndentOptionsEditor.isFieldModified(useContinuationIndentInParameterList,
                                                                       kotlinSettings.CONTINUATION_INDENT_IN_PARAMETER_LISTS)
        isModified = isModified || IndentOptionsEditor.isFieldModified(useContinuationIndentForExpressionBodies,
                                                                       kotlinSettings.CONTINUATION_INDENT_FOR_EXPRESSION_BODIES)
        isModified = isModified || IndentOptionsEditor.isFieldModified(useContinuationIndentForChainedCalls,
                                                                       kotlinSettings.CONTINUATION_INDENT_FOR_CHAINED_CALLS)
        return isModified
    }

    override fun apply(settings: CodeStyleSettings, options: CommonCodeStyleSettings.IndentOptions) {
        super.apply(settings, options)
        val kotlinSettings = settings.getCustomSettings(KotlinCodeStyleSettings::class.java)
        kotlinSettings.CONTINUATION_INDENT_IN_PARAMETER_LISTS = useContinuationIndentInParameterList.isSelected
        kotlinSettings.CONTINUATION_INDENT_FOR_EXPRESSION_BODIES = useContinuationIndentForExpressionBodies.isSelected
        kotlinSettings.CONTINUATION_INDENT_FOR_CHAINED_CALLS = useContinuationIndentForChainedCalls.isSelected
    }

    override fun reset(settings: CodeStyleSettings, options: CommonCodeStyleSettings.IndentOptions) {
        super.reset(settings, options)
        val kotlinSettings = settings.getCustomSettings(KotlinCodeStyleSettings::class.java)
        useContinuationIndentInParameterList.isSelected = kotlinSettings.CONTINUATION_INDENT_IN_PARAMETER_LISTS
        useContinuationIndentForExpressionBodies.isSelected = kotlinSettings.CONTINUATION_INDENT_FOR_EXPRESSION_BODIES
        useContinuationIndentForChainedCalls.isSelected = kotlinSettings.CONTINUATION_INDENT_FOR_CHAINED_CALLS
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        useContinuationIndentInParameterList.isEnabled = enabled
        useContinuationIndentForExpressionBodies.isEnabled = enabled
        useContinuationIndentForChainedCalls.isEnabled = enabled
    }
}