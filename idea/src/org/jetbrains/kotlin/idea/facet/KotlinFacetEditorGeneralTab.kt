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
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerConfigurableTab
import java.awt.BorderLayout
import javax.swing.*

class KotlinFacetEditorGeneralTab(
        private val configuration: KotlinFacetConfiguration,
        private val editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager
) : FacetEditorTab() {
    inner class VersionValidator : FacetEditorValidator() {
        override fun check(): ValidationResult {
            val apiLevel = compilerConfigurable.apiVersionComboBox.selectedItem as? LanguageVersion?
                           ?: return ValidationResult.OK
            val languageLevel = compilerConfigurable.languageVersionComboBox.selectedItem as? LanguageVersion?
                                ?: return ValidationResult.OK
            val targetPlatform = targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
            val libraryLevel = getLibraryLanguageLevel(editorContext.module, editorContext.rootModel, targetPlatform)
            if (languageLevel < apiLevel || libraryLevel < apiLevel) {
                return ValidationResult("Language version/Runtime version may not be less than API version", null)
            }
            return ValidationResult.OK
        }
    }

    private val compilerConfigurable = with(configuration.settings.compilerInfo) {
        KotlinCompilerConfigurableTab(
                editorContext.project,
                commonCompilerArguments,
                k2jsCompilerArguments,
                compilerSettings,
                null,
                null,
                false
        )
    }

    private val useProjectSettingsCheckBox = JCheckBox("Use project settings")

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
            updateCompilerConfigurable()
        }

        compilerConfigurable.languageVersionComboBox.addActionListener {
            validatorsManager.validate()
        }

        compilerConfigurable.apiVersionComboBox.addActionListener {
            validatorsManager.validate()
        }

        targetPlatformComboBox.addActionListener {
            validatorsManager.validate()
            updateCompilerConfigurable()
        }

        updateCompilerConfigurable()

        reset()
    }

    private fun updateCompilerConfigurable() {
        compilerConfigurable.setTargetPlatform(chosenPlatform)
        UIUtil.setEnabled(compilerConfigurable.contentPane, !useProjectSettingsCheckBox.isSelected, true)
    }

    override fun isModified(): Boolean {
        if (useProjectSettingsCheckBox.isSelected != configuration.settings.useProjectSettings) return true
        if (targetPlatformComboBox.selectedItem != configuration.settings.versionInfo.targetPlatformKind) return true
        return !useProjectSettingsCheckBox.isSelected && compilerConfigurable.isModified
    }

    override fun reset() {
        useProjectSettingsCheckBox.isSelected = configuration.settings.useProjectSettings
        targetPlatformComboBox.selectedItem = configuration.settings.versionInfo.targetPlatformKind
        compilerConfigurable.reset()
        updateCompilerConfigurable()
    }

    override fun apply() {
        compilerConfigurable.apply()
        with(configuration.settings) {
            useProjectSettings = useProjectSettingsCheckBox.isSelected
            versionInfo.targetPlatformKind = targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
            versionInfo.languageLevel = LanguageVersion.fromVersionString(compilerInfo.commonCompilerArguments?.languageVersion)
            versionInfo.apiLevel = LanguageVersion.fromVersionString(compilerInfo.commonCompilerArguments?.apiVersion)
        }
    }

    override fun getDisplayName() = "General"

    override fun createComponent(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        val contentPanel = FormBuilder
                .createFormBuilder()
                .addComponent(useProjectSettingsCheckBox)
                .addLabeledComponent("&Target platform: ", targetPlatformComboBox)
                .addComponent(compilerConfigurable.createComponent()!!)
                .panel
        mainPanel.add(contentPanel, BorderLayout.NORTH)
        return mainPanel
    }

    override fun disposeUIResources() {
        compilerConfigurable.disposeUIResources()
    }

    val chosenPlatform: TargetPlatformKind<*>?
        get() = targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
}
