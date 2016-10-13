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
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.libraries.LibraryPresentationProvider
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription
import com.intellij.util.text.VersionComparatorUtil
import com.intellij.util.ui.FormBuilder
import org.jetbrains.kotlin.idea.framework.*
import org.jetbrains.kotlin.idea.util.DescriptionAware
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*

class KotlinFacetEditorTab(
        private val configuration: KotlinFacetConfiguration,
        private val editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager
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
            val apiLevel = apiVersionComboBox.selectedItem as? KotlinFacetConfiguration.LanguageLevel? ?: return ValidationResult.OK
            val languageLevel = languageVersionComboBox.selectedItem as? KotlinFacetConfiguration.LanguageLevel? ?: return ValidationResult.OK
            val libraryLevel = getLibraryLanguageLevel()
            if (languageLevel < apiLevel || libraryLevel < apiLevel) {
                return ValidationResult("Language version/Runtime version may not be less than API version", null)
            }
            return ValidationResult.OK
        }
    }

    private val KotlinFacetConfiguration.TargetPlatform.libraryDescription: CustomLibraryDescription
        get() {
            return when (this) {
                KotlinFacetConfiguration.TargetPlatform.JVM_1_6, KotlinFacetConfiguration.TargetPlatform.JVM_1_8 ->
                    JavaRuntimeLibraryDescription(editorContext.project)
                KotlinFacetConfiguration.TargetPlatform.JS ->
                    JSLibraryStdDescription(editorContext.project)
            }
        }

    private val languageVersionComboBox =
            JComboBox<KotlinFacetConfiguration.LanguageLevel>(KotlinFacetConfiguration.LanguageLevel.values()).apply {
                setRenderer(DescriptionListCellRenderer())
            }

    private val apiVersionComboBox =
            JComboBox<KotlinFacetConfiguration.LanguageLevel>(KotlinFacetConfiguration.LanguageLevel.values()).apply {
                setRenderer(DescriptionListCellRenderer())
            }

    private val targetPlatformComboBox =
            JComboBox<KotlinFacetConfiguration.TargetPlatform>(KotlinFacetConfiguration.TargetPlatform.values()).apply {
                setRenderer(DescriptionListCellRenderer())
            }

    private val libraryValidator: FrameworkLibraryValidator
    private val versionValidator = VersionValidator()

    private fun getRuntimeLibraryVersions(presentationProvider: LibraryPresentationProvider<LibraryVersionProperties>): List<String> {
        return editorContext
                .rootModel
                .orderEntries
                .asSequence()
                .filterIsInstance<LibraryOrderEntry>()
                .mapNotNull { it.library?.let { getLibraryProperties(presentationProvider, it) }?.versionString }
                .toList()
    }

    private fun getDefaultTargetPlatform(): KotlinFacetConfiguration.TargetPlatform {
        getRuntimeLibraryVersions(JSLibraryStdPresentationProvider.getInstance()).firstOrNull()?.let {
            return KotlinFacetConfiguration.TargetPlatform.JS
        }

        val sdk = editorContext.rootModel.sdk
        val sdkVersion = (sdk?.sdkType as? JavaSdk)?.getVersion(sdk!!)
        return when {
            sdkVersion != null && sdkVersion <= JavaSdkVersion.JDK_1_6 -> KotlinFacetConfiguration.TargetPlatform.JVM_1_6
            else -> KotlinFacetConfiguration.TargetPlatform.JVM_1_8
        }
    }

    private fun getDefaultLanguageLevel(libraryVersion: String? = null): KotlinFacetConfiguration.LanguageLevel {
        val libVersion = libraryVersion ?: bundledRuntimeVersion()
        return when {
            libVersion.startsWith("1.0") -> KotlinFacetConfiguration.LanguageLevel.KOTLIN_1_0
            else -> KotlinFacetConfiguration.LanguageLevel.KOTLIN_1_1
        }
    }

    private fun getLibraryLanguageLevel(): KotlinFacetConfiguration.LanguageLevel {
        val presentationProvider = when (targetPlatformComboBox.selectedItem as KotlinFacetConfiguration.TargetPlatform?) {
            KotlinFacetConfiguration.TargetPlatform.JS ->
                JSLibraryStdPresentationProvider.getInstance()
            KotlinFacetConfiguration.TargetPlatform.JVM_1_6,
            KotlinFacetConfiguration.TargetPlatform.JVM_1_8,
            null ->
                JavaRuntimePresentationProvider.getInstance()
        }
        val minVersion = getRuntimeLibraryVersions(presentationProvider).minWith(VersionComparatorUtil.COMPARATOR)
        return getDefaultLanguageLevel(minVersion)
    }

    init {
        libraryValidator = FrameworkLibraryValidatorWithDynamicDescription(
                DelegatingLibrariesValidatorContext(editorContext),
                validatorsManager,
                "kotlin"
        ) { (targetPlatformComboBox.selectedItem as KotlinFacetConfiguration.TargetPlatform).libraryDescription }

        validatorsManager.registerValidator(libraryValidator)
        validatorsManager.registerValidator(versionValidator)

        targetPlatformComboBox.addActionListener {
            validatorsManager.validate()
        }

        with(configuration.state) {
            if (targetPlatformKind == null) {
                targetPlatformKind = getDefaultTargetPlatform()
            }

            if (languageLevel == null) {
                languageLevel = getDefaultLanguageLevel()
            }

            if (apiLevel == null) {
                apiLevel = languageLevel!!.coerceAtMost(getLibraryLanguageLevel())
            }
        }

        reset()
    }

    override fun isModified(): Boolean {
        return languageVersionComboBox.selectedItem != configuration.state.languageLevel
               || targetPlatformComboBox.selectedItem != configuration.state.targetPlatformKind
               || apiVersionComboBox.selectedItem != configuration.state.apiLevel
    }

    override fun reset() {
        languageVersionComboBox.selectedItem = configuration.state.languageLevel
        targetPlatformComboBox.selectedItem = configuration.state.targetPlatformKind
        apiVersionComboBox.selectedItem = configuration.state.apiLevel
    }

    override fun apply() {
        configuration.state.languageLevel = languageVersionComboBox.selectedItem as KotlinFacetConfiguration.LanguageLevel?
        configuration.state.targetPlatformKind = targetPlatformComboBox.selectedItem as KotlinFacetConfiguration.TargetPlatform?
        configuration.state.apiLevel = apiVersionComboBox.selectedItem as KotlinFacetConfiguration.LanguageLevel?
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
}
