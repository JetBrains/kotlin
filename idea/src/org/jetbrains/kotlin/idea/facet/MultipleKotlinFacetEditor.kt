/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.facet

import com.intellij.facet.ui.FacetEditor
import com.intellij.facet.ui.FacetEditorsFactory
import com.intellij.facet.ui.MultipleFacetSettingsEditor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerConfigurableTab
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import javax.swing.JComponent

class MultipleKotlinFacetEditor(
    private val project: Project,
    private val editors: Array<out FacetEditor>
) : MultipleFacetSettingsEditor() {
    private val helper = FacetEditorsFactory.getInstance().createMultipleFacetEditorHelper()

    private val FacetEditor.tabEditor: KotlinFacetEditorGeneralTab.EditorComponent
        get() = editorTabs.firstIsInstance<KotlinFacetEditorGeneralTab>().editor

    private val FacetEditor.compilerConfigurable: KotlinCompilerConfigurableTab
        get() = tabEditor.compilerConfigurable

    override fun createComponent(): JComponent? {
        return KotlinFacetEditorGeneralTab.EditorComponent(project, null).apply {
            initialize()
            editors.flatMap { it.editorTabs.filterIsInstance<KotlinFacetEditorGeneralTab>() }.forEach { it.initializeIfNeeded() }
            helper.bind(useProjectSettingsCheckBox, editors) { it.tabEditor.useProjectSettingsCheckBox }
            //TODO(auskov): Support bulk editing target platforms?
            with(compilerConfigurable) {
                helper.bind(reportWarningsCheckBox, editors) { it.compilerConfigurable.reportWarningsCheckBox }
                helper.bind(additionalArgsOptionsField.textField, editors) { it.compilerConfigurable.additionalArgsOptionsField.textField }
                helper.bind(generateSourceMapsCheckBox, editors) { it.compilerConfigurable.generateSourceMapsCheckBox }
                helper.bind(outputPrefixFile.textField, editors) { it.compilerConfigurable.outputPrefixFile.textField }
                helper.bind(outputPostfixFile.textField, editors) { it.compilerConfigurable.outputPostfixFile.textField }
                helper.bind(outputDirectory.textField, editors) { it.compilerConfigurable.outputDirectory.textField }
                helper.bind(copyRuntimeFilesCheckBox, editors) { it.compilerConfigurable.copyRuntimeFilesCheckBox }
                helper.bind(keepAliveCheckBox, editors) { it.compilerConfigurable.keepAliveCheckBox }
                helper.bind(moduleKindComboBox, editors) { it.compilerConfigurable.moduleKindComboBox }
                helper.bind(scriptTemplatesField, editors) { it.compilerConfigurable.scriptTemplatesField }
                helper.bind(scriptTemplatesClasspathField, editors) { it.compilerConfigurable.scriptTemplatesClasspathField }
                helper.bind(languageVersionComboBox, editors) { it.compilerConfigurable.languageVersionComboBox }
                helper.bind(apiVersionComboBox, editors) { it.compilerConfigurable.apiVersionComboBox }
            }
        }
    }

    override fun disposeUIResources() {
        helper.unbind()
        // Reset tabs with selected "Use project settings" after switching off the multi-editor mode.
        // Their settings might have changed to non-project one due to UI control binding
        editors.map { it.tabEditor }.filter { it.useProjectSettingsCheckBox.isSelected }.forEach { it.updateCompilerConfigurable() }
    }
}
