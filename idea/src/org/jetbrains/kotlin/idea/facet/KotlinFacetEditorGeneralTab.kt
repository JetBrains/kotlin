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
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.TargetPlatformKind
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class KotlinFacetEditorGeneralTab(
        private val configuration: KotlinFacetConfiguration,
        private val editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager,
        private val compilerTab: KotlinFacetEditorCompilerTab
) : FacetEditorTab() {
    inner class VersionValidator : FacetEditorValidator() {
        override fun check(): ValidationResult {
            val apiLevel = apiVersionComboBox.selectedItem as? LanguageVersion? ?: return ValidationResult.OK
            val languageLevel = languageVersionComboBox.selectedItem as? LanguageVersion? ?: return ValidationResult.OK
            val targetPlatform = targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
            val libraryLevel = getLibraryLanguageLevel(editorContext.module, editorContext.rootModel, targetPlatform)
            if (languageLevel < apiLevel || libraryLevel < apiLevel) {
                return ValidationResult("Language version/Runtime version may not be less than API version", null)
            }
            return ValidationResult.OK
        }
    }

    private val useProjectSettingsCheckBox = JCheckBox("Use project settings")

    private val languageVersionComboBox =
            JComboBox<LanguageVersion>(LanguageVersion.values()).apply {
                setRenderer(DescriptionListCellRenderer())
            }

    private val apiVersionComboBox =
            JComboBox<LanguageVersion>(LanguageVersion.values()).apply {
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

        useProjectSettingsCheckBox.addActionListener {
            useProjectSettingsChanged()
        }

        languageVersionComboBox.addActionListener {
            validatorsManager.validate()
        }

        apiVersionComboBox.addActionListener {
            validatorsManager.validate()
        }

        targetPlatformComboBox.addActionListener {
            validatorsManager.validate()
            updateCompilerTab()
        }

        updateCompilerTab()

        reset()
    }

    private fun useProjectSettingsChanged() {
        val useModuleSpecific = !useProjectSettingsCheckBox.isSelected
        languageVersionComboBox.isEnabled = useModuleSpecific
        apiVersionComboBox.isEnabled = useModuleSpecific

        updateCompilerTab()
    }

    private fun updateCompilerTab() {
        compilerTab.compilerConfigurable.setTargetPlatform(chosenPlatform)
        UIUtil.setEnabled(compilerTab.compilerConfigurable.contentPane, !useProjectSettingsCheckBox.isSelected, true)
    }

    override fun isModified(): Boolean {
        if (useProjectSettingsCheckBox.isSelected != configuration.settings.useProjectSettings) return true

        return with(configuration.settings.versionInfo) {
            if (useProjectSettingsCheckBox.isSelected) return targetPlatformComboBox.selectedItem != targetPlatformKind

            languageVersionComboBox.selectedItem != languageLevel
            || targetPlatformComboBox.selectedItem != targetPlatformKind
            || apiVersionComboBox.selectedItem != apiLevel
        }
    }

    override fun reset() {
        useProjectSettingsCheckBox.isSelected = configuration.settings.useProjectSettings
        with(configuration.settings.versionInfo) {
            languageVersionComboBox.selectedItem = languageLevel
            apiVersionComboBox.selectedItem = apiLevel
            targetPlatformComboBox.selectedItem = targetPlatformKind
        }
        useProjectSettingsChanged()
    }

    override fun apply() {
        configuration.settings.useProjectSettings = useProjectSettingsCheckBox.isSelected
        with(configuration.settings.versionInfo) {
            if (!useProjectSettingsCheckBox.isSelected) {
                languageLevel = languageVersionComboBox.selectedItem as LanguageVersion?
                apiLevel = apiVersionComboBox.selectedItem as LanguageVersion?
            }
            targetPlatformKind = targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
        }
    }

    override fun getDisplayName() = "General"

    override fun createComponent(): JComponent {
       val mainPanel = JPanel(BorderLayout())
       val contentPanel = FormBuilder
               .createFormBuilder()
               .addComponent(useProjectSettingsCheckBox)
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
