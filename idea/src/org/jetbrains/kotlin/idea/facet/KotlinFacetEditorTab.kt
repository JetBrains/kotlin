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
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.facet.ui.libraries.FrameworkLibraryValidator
import com.intellij.framework.library.LibraryVersionProperties
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription
import com.intellij.util.ui.FormBuilder
import org.jetbrains.kotlin.idea.framework.JSLibraryStdDescription
import org.jetbrains.kotlin.idea.framework.JSLibraryStdPresentationProvider
import org.jetbrains.kotlin.idea.framework.JavaRuntimeLibraryDescription
import org.jetbrains.kotlin.idea.framework.getLibraryProperties
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

    private val targetPlatformComboBox =
            JComboBox<KotlinFacetConfiguration.TargetPlatform>(KotlinFacetConfiguration.TargetPlatform.values()).apply {
                setRenderer(DescriptionListCellRenderer())
            }

    private val validator: FrameworkLibraryValidator

    private fun getRuntimeLibraryVersions(
            libToProperties: Library.() -> LibraryVersionProperties?
    ): List<String> {
        return editorContext
                .rootModel
                .orderEntries
                .asSequence()
                .filterIsInstance<LibraryOrderEntry>()
                .mapNotNull { it.library?.libToProperties()?.versionString }
                .toList()
    }

    private fun getDefaultTargetPlatform(): KotlinFacetConfiguration.TargetPlatform {
        getRuntimeLibraryVersions { getLibraryProperties(JSLibraryStdPresentationProvider.getInstance(), this) }.firstOrNull()?.let { javaLib ->
            return KotlinFacetConfiguration.TargetPlatform.JS
        }

        val sdk = editorContext.rootModel.sdk
        val sdkVersion = (sdk?.sdkType as? JavaSdk)?.getVersion(sdk!!)
        return when {
            sdkVersion != null && sdkVersion <= JavaSdkVersion.JDK_1_6 -> KotlinFacetConfiguration.TargetPlatform.JVM_1_6
            else -> KotlinFacetConfiguration.TargetPlatform.JVM_1_8
        }
    }

    private fun getDefaultLanguageLevel(): KotlinFacetConfiguration.LanguageLevel {
        val libVersion = bundledRuntimeVersion()
        return when {
            libVersion.startsWith("1.0") -> KotlinFacetConfiguration.LanguageLevel.KOTLIN_1_0
            else -> KotlinFacetConfiguration.LanguageLevel.KOTLIN_1_1
        }
    }

    init {
        validator = FrameworkLibraryValidatorWithDynamicDescription(
                DelegatingLibrariesValidatorContext(editorContext),
                validatorsManager,
                "kotlin"
        ) { (targetPlatformComboBox.selectedItem as KotlinFacetConfiguration.TargetPlatform).libraryDescription }
        validatorsManager.registerValidator(validator)

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
        }

        reset()
    }

    override fun isModified(): Boolean {
        return languageVersionComboBox.selectedItem != configuration.state.languageLevel
               || targetPlatformComboBox.selectedItem != configuration.state.targetPlatformKind
    }

    override fun reset() {
        languageVersionComboBox.selectedItem = configuration.state.languageLevel
        targetPlatformComboBox.selectedItem = configuration.state.targetPlatformKind
    }

    override fun apply() {
        configuration.state.languageLevel = languageVersionComboBox.selectedItem as KotlinFacetConfiguration.LanguageLevel?
        configuration.state.targetPlatformKind = targetPlatformComboBox.selectedItem as KotlinFacetConfiguration.TargetPlatform?
    }

    override fun getDisplayName() = "Kotlin"

    override fun createComponent(): JComponent {
       val mainPanel = JPanel(BorderLayout())
       val contentPanel = FormBuilder
               .createFormBuilder()
               .addLabeledComponent("&Language version: ", languageVersionComboBox)
               .addLabeledComponent("&Target platform: ", targetPlatformComboBox)
               .panel
        mainPanel.add(contentPanel, BorderLayout.NORTH)
        return mainPanel
    }

    override fun disposeUIResources() {

    }
}
