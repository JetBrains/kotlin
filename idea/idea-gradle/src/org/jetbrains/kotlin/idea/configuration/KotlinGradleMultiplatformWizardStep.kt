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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.ProjectWizardUtil
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.externalSystem.model.project.ProjectId
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.PanelWithAnchor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.event.DocumentEvent

class KotlinGradleMultiplatformWizardStep(
    private val builder: KotlinGradleMultiplatformModuleBuilder,
    private val wizardContext: WizardContext
) : ModuleWizardStep() {

    private val mainModuleNameComponent: LabeledComponent<JTextField> =
        LabeledComponent.create(JTextField(), "Main module name:", BorderLayout.WEST)
    private val jvmModuleNameComponent: LabeledComponent<JTextField> =
        LabeledComponent.create(JTextField(), "JVM module name:", BorderLayout.WEST)
    private val jsModuleNameComponent: LabeledComponent<JTextField> =
        LabeledComponent.create(JTextField(), "JS module name:", BorderLayout.WEST)

    private val panel: JPanel
    private var syncEditing: Boolean = true
    private var inSyncUpdate: Boolean = false

    init {
        panel = object : JPanel(GridBagLayout()), PanelWithAnchor {
            private var anchor: JComponent? = mainModuleNameComponent.anchor

            override fun getAnchor(): JComponent? = anchor

            override fun setAnchor(anchor: JComponent?) {
                this.anchor = anchor
                mainModuleNameComponent.anchor = anchor
                jvmModuleNameComponent.anchor = anchor
                jsModuleNameComponent.anchor = anchor
            }
        }
        val baseDir = wizardContext.projectFileDirectory
        val projectName = wizardContext.projectName
        val initialProjectName = projectName ?: ProjectWizardUtil.findNonExistingFileName(baseDir, "untitled", "")
        mainModuleNameComponent.component.text = initialProjectName
        mainModuleNameComponent.component.select(0, initialProjectName.length)

        updateDerivedModuleNames()

        mainModuleNameComponent.component.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent?) {
                if (syncEditing) {
                    updateDerivedModuleNames()
                }
            }
        })

        val stopSyncEditingListener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent?) {
                if (!inSyncUpdate) {
                    syncEditing = false
                }
            }
        }
        jvmModuleNameComponent.component.document.addDocumentListener(stopSyncEditingListener)
        jsModuleNameComponent.component.document.addDocumentListener(stopSyncEditingListener)

        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        panel.add(
            mainModuleNameComponent,
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.insets(10, 0, 0, 0), 0, 0
            )
        )
        panel.add(
            jvmModuleNameComponent,
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.emptyInsets(), 0, 0
            )
        )
        panel.add(
            jsModuleNameComponent,
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.emptyInsets(), 0, 0
            )
        )
        panel.add(
            JLabel(""),
            GridBagConstraints(
                0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                JBUI.emptyInsets(), 0, 0
            )
        )
        UIUtil.mergeComponentsWithAnchor(panel)
    }

    private fun updateDerivedModuleNames() {
        inSyncUpdate = true
        try {
            jvmModuleNameComponent.component.text = "$mainModuleName-jvm"
            jsModuleNameComponent.component.text = "$mainModuleName-js"

        } finally {
            inSyncUpdate = false
        }
    }

    override fun updateDataModel() {
        wizardContext.projectBuilder = builder
        wizardContext.projectName = mainModuleName

        builder.projectId = ProjectId("", mainModuleName, "")
        builder.jvmModuleName = jvmModuleName
        builder.jsModuleName = jsModuleName
    }

    override fun getComponent() = panel

    private val mainModuleName: String
        get() = mainModuleNameComponent.component.text
    private val jvmModuleName: String
        get() = jvmModuleNameComponent.component.text
    private val jsModuleName: String
        get() = jsModuleNameComponent.component.text

    override fun validate(): Boolean {
        if (mainModuleName.isEmpty()) {
            throw ConfigurationException("Please specify the main module name")
        }
        if (jvmModuleName.isNotEmpty() && (jvmModuleName == mainModuleName || jvmModuleName == jsModuleName)) {
            throw ConfigurationException("The JVM module name should be distinct")
        }
        if (jsModuleName.isNotEmpty() && jsModuleName == mainModuleName) {
            throw ConfigurationException("The IS module name should be distinct")
        }
        return true
    }
}
