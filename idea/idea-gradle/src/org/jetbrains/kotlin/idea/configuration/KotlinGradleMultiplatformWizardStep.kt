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
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import javax.swing.*
import javax.swing.event.DocumentEvent

class KotlinGradleMultiplatformWizardStep(
    private val builder: KotlinGradleMultiplatformModuleBuilder,
    private val wizardContext: WizardContext
) : ModuleWizardStep() {

    private val hierarchyKindComponent = ComboBox(
        arrayOf(
            "Root empty module with common & platform children",
            "Root common module with children platform modules"
        ), 400
    )
    private val rootModuleNameComponent = JTextField()
    private val commonModuleNameComponent = JTextField()
    private val jvmCheckBox = JCheckBox("", true)
    private val jdkModel = ProjectSdksModel().also {
        it.reset(ProjectManager.getInstance().defaultProject)
    }
    private val jdkComboBox = JdkComboBox(jdkModel) { it is JavaSdk }
    private val jvmModuleNameComponent = JTextField()
    private val jsCheckBox = JCheckBox("", true)
    private val jsModuleNameComponent = JTextField()

    private val panel: JPanel
    private var syncEditing: Boolean = true
    private var inSyncUpdate: Boolean = false

    init {
        val baseDir = wizardContext.projectFileDirectory
        val projectName = wizardContext.projectName
        val initialProjectName = projectName ?: ProjectWizardUtil.findNonExistingFileName(baseDir, "untitled", "")
        rootModuleNameComponent.text = initialProjectName
        rootModuleNameComponent.select(0, initialProjectName.length)

        updateDerivedModuleNames()

        rootModuleNameComponent.document.addDocumentListener(object : DocumentAdapter() {
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
        commonModuleNameComponent.document.addDocumentListener(stopSyncEditingListener)
        jvmModuleNameComponent.document.addDocumentListener(stopSyncEditingListener)
        jsModuleNameComponent.document.addDocumentListener(stopSyncEditingListener)

        jdkComboBox.selectedJdk = jdkModel.projectSdk

        hierarchyKindComponent.addActionListener {
            commonModuleNameComponent.isEnabled = !commonModuleIsRoot
        }
        jvmCheckBox.addItemListener {
            jvmModuleNameComponent.isEnabled = jvmCheckBox.isSelected
            jdkComboBox.isEnabled = jvmCheckBox.isSelected
        }
        jsCheckBox.addItemListener {
            jsModuleNameComponent.isEnabled = jsCheckBox.isSelected
        }

        this.panel = panel(LCFlags.fillY) {
            row("Hierarchy kind:") { hierarchyKindComponent() }
            row("Root module name:") { rootModuleNameComponent() }
            row("Common module name:") { commonModuleNameComponent() }
            row("Create JVM module:") { jvmCheckBox() }
            row("      JVM module JDK:") { jdkComboBox() }
            row("      JVM module name:") { jvmModuleNameComponent() }
            row("Create JS module:") { jsCheckBox() }
            row("      JS module name:") { jsModuleNameComponent() }
            row { JLabel("")(CCFlags.pushY, CCFlags.growY) }
        }
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    }

    private fun updateDerivedModuleNames() {
        inSyncUpdate = true
        try {
            commonModuleNameComponent.text = "$rootModuleName-common"
            jvmModuleNameComponent.text = "$rootModuleName-jvm"
            jsModuleNameComponent.text = "$rootModuleName-js"

        } finally {
            inSyncUpdate = false
        }
    }

    override fun updateDataModel() {
        wizardContext.projectBuilder = builder
        wizardContext.projectName = rootModuleName

        builder.projectId = ProjectId("", rootModuleName, "")
        builder.commonModuleName = commonModuleName
        builder.jvmModuleName = jvmModuleName
        builder.jdk = jdk
        builder.jsModuleName = jsModuleName
    }

    override fun getComponent() = panel

    private val commonModuleIsRoot: Boolean
        get() = hierarchyKindComponent.selectedIndex != 0
    private val rootModuleName: String
        get() = rootModuleNameComponent.text
    private val commonModuleName: String
        get() = if (commonModuleIsRoot) "" else commonModuleNameComponent.text
    private val jvmModuleName: String
        get() = if (jvmCheckBox.isSelected) jvmModuleNameComponent.text else ""
    private val jdk: Sdk?
        get() = if (jvmCheckBox.isSelected) jdkComboBox.selectedJdk else null
    private val jsModuleName: String
        get() = if (jsCheckBox.isSelected) jsModuleNameComponent.text else ""

    override fun validate(): Boolean {
        if (rootModuleName.isEmpty()) {
            throw ConfigurationException("Please specify the root module name")
        }
        if (!commonModuleIsRoot && commonModuleName.isEmpty()) {
            throw ConfigurationException("Please specify the common module name")
        }
        if (jvmCheckBox.isSelected && jvmModuleName.isEmpty()) {
            throw ConfigurationException("Please specify the JVM module name")
        }
        if (jsCheckBox.isSelected && jsModuleName.isEmpty()) {
            throw ConfigurationException("Please specify the JS module name")
        }
        if (commonModuleName.isNotEmpty()
            && (commonModuleName == rootModuleName || commonModuleName == jvmModuleName || commonModuleName == jsModuleName)
        ) {
            throw ConfigurationException("The common module name should be distinct")
        }
        if (jvmModuleName.isNotEmpty() && (jvmModuleName == rootModuleName || jvmModuleName == jsModuleName)) {
            throw ConfigurationException("The JVM module name should be distinct")
        }
        if (jsModuleName.isNotEmpty() && jsModuleName == rootModuleName) {
            throw ConfigurationException("The IS module name should be distinct")
        }
        return true
    }
}
