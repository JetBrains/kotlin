/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.facet

import com.intellij.facet.impl.ui.libraries.DelegatingLibrariesValidatorContext
import com.intellij.facet.ui.*
import com.intellij.facet.ui.libraries.FrameworkLibraryValidator
import com.intellij.util.ui.FormBuilder
import org.jetbrains.kotlin.idea.util.DescriptionAware
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*

class KotlinFacetEditorGeneralTab(
        private val configuration: KotlinFacetConfiguration,
        private val editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager,
        private val compilerTab: KotlinFacetEditorCompilerTab
) : FacetEditorTab() {
    class DescriptionListCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).apply {
                text = (value as? DescriptionAware)?.description ?: ""
            }
        }
    }

    inner class VersionValidator : FacetEditorValidator() {
        override fun check(): ValidationResult {
            val apiLevel = apiVersionComboBox.selectedItem as? LanguageLevel? ?: return ValidationResult.OK
            val languageLevel = languageVersionComboBox.selectedItem as? LanguageLevel? ?: return ValidationResult.OK
            val targetPlatform = targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
            val libraryLevel = getLibraryLanguageLevel(editorContext.module, editorContext.rootModel, targetPlatform)
            if (languageLevel < apiLevel || libraryLevel < apiLevel) {
                return ValidationResult("Language version/Runtime version may not be less than API version", null)
            }
            return ValidationResult.OK
        }
    }

    private val languageVersionComboBox =
            JComboBox<LanguageLevel>(LanguageLevel.values()).apply {
                setRenderer(DescriptionListCellRenderer())
            }

    private val apiVersionComboBox =
            JComboBox<LanguageLevel>(LanguageLevel.values()).apply {
                setRenderer(DescriptionListCellRenderer())
            }

    private val targetPlatformComboBox =
            JComboBox<TargetPlatformKind<*>>(TargetPlatformKind.ALL_PLATFORMS.toTypedArray()).apply {
                setRenderer(DescriptionListCellRenderer())
            }

    private val libraryValidator: FrameworkLibraryValidator
    private val versionValidator = VersionValidator()

    init {
        libraryValidator = FrameworkLibraryValidatorWithDynamicDescription(
                DelegatingLibrariesValidatorContext(editorContext),
                validatorsManager,
                "kotlin"
        ) { targetPlatformComboBox.selectedItem as TargetPlatformKind<*> }

        validatorsManager.registerValidator(libraryValidator)
        validatorsManager.registerValidator(versionValidator)

        targetPlatformComboBox.addActionListener {
            validatorsManager.validate()
            updateCompilerTab()
        }

        updateCompilerTab()

        reset()
    }

    private fun updateCompilerTab() {
        compilerTab.compilerConfigurable.setTargetPlatform(chosenPlatform)
    }

    override fun isModified(): Boolean {
        return with(configuration.state.versionInfo) {
            languageVersionComboBox.selectedItem != languageLevel
            || targetPlatformComboBox.selectedItem != targetPlatformKindKind
            || apiVersionComboBox.selectedItem != apiLevel
        }
    }

    override fun reset() {
        with(configuration.state.versionInfo) {
            languageVersionComboBox.selectedItem = languageLevel
            targetPlatformComboBox.selectedItem = targetPlatformKindKind
            apiVersionComboBox.selectedItem = apiLevel
        }
    }

    override fun apply() {
        with(configuration.state.versionInfo) {
            languageLevel = languageVersionComboBox.selectedItem as LanguageLevel?
            targetPlatformKindKind = targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
            apiLevel = apiVersionComboBox.selectedItem as LanguageLevel?
        }
    }

    override fun getDisplayName() = "General"

    override fun createComponent(): JComponent {
       val mainPanel = JPanel(BorderLayout())
       val contentPanel = FormBuilder
               .createFormBuilder()
               .addLabeledComponent("&Language version: ", languageVersionComboBox)
               .addLabeledComponent("&Standard library API version: ", apiVersionComboBox)
               .addLabeledComponent("&Target platform: ", targetPlatformComboBox)
               .panel
        mainPanel.add(contentPanel, BorderLayout.NORTH)
        return mainPanel
    }

    override fun disposeUIResources() {

    }

    val chosenPlatform: TargetPlatformKind<*>?
        get() = targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
}
