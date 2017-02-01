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
import com.intellij.openapi.project.Project
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.ThreeStateCheckBox
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.KotlinCompilerInfo
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
    class EditorComponent(
            project: Project,
            configuration: KotlinFacetConfiguration?
    ) : JPanel(BorderLayout()) {
        private val isMultiEditor = configuration == null

        private val compilerInfo = configuration?.settings?.compilerInfo
                           ?: KotlinCompilerInfo()
                                   .apply {
                                       commonCompilerArguments = object : CommonCompilerArguments() {}
                                       k2jsCompilerArguments = K2JSCompilerArguments()
                                       compilerSettings = CompilerSettings()
                                   }
        val compilerConfigurable = with(compilerInfo) {
            KotlinCompilerConfigurableTab(
                    project,
                    commonCompilerArguments,
                    k2jsCompilerArguments,
                    compilerSettings,
                    null,
                    null,
                    false,
                    isMultiEditor
            )
        }

        val useProjectSettingsCheckBox = ThreeStateCheckBox("Use project settings").apply { isThirdStateEnabled = isMultiEditor }

        val targetPlatformComboBox =
                JComboBox<TargetPlatformKind<*>>(TargetPlatformKind.ALL_PLATFORMS.toTypedArray()).apply {
                    setRenderer(DescriptionListCellRenderer())
                }

        init {
            val contentPanel = FormBuilder
                    .createFormBuilder()
                    .addComponent(useProjectSettingsCheckBox)
                    .addLabeledComponent("&Target platform: ", targetPlatformComboBox)
                    .addComponent(compilerConfigurable.createComponent()!!)
                    .panel
            add(contentPanel, BorderLayout.NORTH)

            useProjectSettingsCheckBox.addActionListener {
                updateCompilerConfigurable()
            }

            targetPlatformComboBox.addActionListener {
                updateCompilerConfigurable()
            }
        }

        internal fun updateCompilerConfigurable() {
            compilerConfigurable.setTargetPlatform(chosenPlatform)
            UIUtil.setEnabled(compilerConfigurable.contentPane, !useProjectSettingsCheckBox.isSelected, true)
        }

        val chosenPlatform: TargetPlatformKind<*>?
            get() = targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
    }

    inner class VersionValidator : FacetEditorValidator() {
        override fun check(): ValidationResult {
            val apiLevel = editor.compilerConfigurable.apiVersionComboBox.selectedItem as? LanguageVersion?
                           ?: return ValidationResult.OK
            val languageLevel = editor.compilerConfigurable.languageVersionComboBox.selectedItem as? LanguageVersion?
                                ?: return ValidationResult.OK
            val targetPlatform = editor.targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
            val libraryLevel = getLibraryLanguageLevel(editorContext.module, editorContext.rootModel, targetPlatform)
            if (languageLevel < apiLevel || libraryLevel < apiLevel) {
                return ValidationResult("Language version/Runtime version may not be less than API version", null)
            }
            return ValidationResult.OK
        }
    }

    val editor = EditorComponent(editorContext.project, configuration)

    private val libraryValidator: FrameworkLibraryValidator
    private val versionValidator = VersionValidator()

    init {
        libraryValidator = FrameworkLibraryValidatorWithDynamicDescription(
                DelegatingLibrariesValidatorContext(editorContext),
                validatorsManager,
                "kotlin"
        ) { editor.targetPlatformComboBox.selectedItem as TargetPlatformKind<*> }

        validatorsManager.registerValidator(libraryValidator)
        validatorsManager.registerValidator(versionValidator)

        editor.compilerConfigurable.languageVersionComboBox.addActionListener {
            validatorsManager.validate()
        }

        editor.compilerConfigurable.apiVersionComboBox.addActionListener {
            validatorsManager.validate()
        }

        editor.targetPlatformComboBox.addActionListener {
            validatorsManager.validate()
        }

        editor.updateCompilerConfigurable()

        reset()
    }

    override fun isModified(): Boolean {
        if (editor.useProjectSettingsCheckBox.isSelected != configuration.settings.useProjectSettings) return true
        if (editor.targetPlatformComboBox.selectedItem != configuration.settings.versionInfo.targetPlatformKind) return true
        return !editor.useProjectSettingsCheckBox.isSelected && editor.compilerConfigurable.isModified
    }

    override fun reset() {
        editor.useProjectSettingsCheckBox.isSelected = configuration.settings.useProjectSettings
        editor.targetPlatformComboBox.selectedItem = configuration.settings.versionInfo.targetPlatformKind
        editor.compilerConfigurable.reset()
        editor.updateCompilerConfigurable()
    }

    override fun apply() {
        editor.compilerConfigurable.apply()
        with(configuration.settings) {
            useProjectSettings = editor.useProjectSettingsCheckBox.isSelected
            versionInfo.targetPlatformKind = editor.targetPlatformComboBox.selectedItem as TargetPlatformKind<*>?
            versionInfo.languageLevel = LanguageVersion.fromVersionString(compilerInfo.commonCompilerArguments?.languageVersion)
            versionInfo.apiLevel = LanguageVersion.fromVersionString(compilerInfo.commonCompilerArguments?.apiVersion)
        }
    }

    override fun getDisplayName() = "General"

    override fun createComponent(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        val contentPanel = FormBuilder
                .createFormBuilder()
                .addComponent(editor.useProjectSettingsCheckBox)
                .addLabeledComponent("&Target platform: ", editor.targetPlatformComboBox)
                .addComponent(editor.compilerConfigurable.createComponent()!!)
                .panel
        mainPanel.add(contentPanel, BorderLayout.NORTH)
        return mainPanel
    }

    override fun disposeUIResources() {
        editor.compilerConfigurable.disposeUIResources()
    }
}
