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
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.MessageType
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import org.jetbrains.kotlin.idea.util.onTextChange
import javax.swing.*

class KotlinGradleMultiplatformWizardStep(
    private val builder: KotlinGradleMultiplatformModuleBuilder,
    private val wizardContext: WizardContext
) : ModuleWizardStep() {

    private val hierarchyKindComponent = ComboBox(
        arrayOf(
            "Flat, all created modules on the same level",
            "Hierarchical, platform modules under common one"
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
    private var showErrorBalloons: Boolean = true

    init {
        val baseDir = wizardContext.projectFileDirectory
        val projectName = wizardContext.projectName
        val initialProjectName = projectName ?: ProjectWizardUtil.findNonExistingFileName(baseDir, "untitled", "")
        rootModuleNameComponent.text = initialProjectName
        rootModuleNameComponent.select(0, initialProjectName.length)

        updateDerivedModuleNames()

        rootModuleNameComponent.onTextChange {
            val rootModuleError = getRootModuleError()
            showErrorBalloonIfNeeded(rootModuleNameComponent, rootModuleError)
            if (syncEditing) {
                showErrorBalloons = false
                try {
                    updateDerivedModuleNames()
                } finally {
                    showErrorBalloons = true
                    if (rootModuleError == null) {
                        showCommonModuleErrorIfNeeded()
                        showJvmModuleErrorIfNeeded()
                        showJsModuleErrorIfNeeded()
                    }
                }
            }
        }

        commonModuleNameComponent.onTextChange {
            stopSyncEditing()
            showCommonModuleErrorIfNeeded()
        }
        jvmModuleNameComponent.onTextChange {
            stopSyncEditing()
            showJvmModuleErrorIfNeeded()
        }
        jsModuleNameComponent.onTextChange {
            stopSyncEditing()
            showJsModuleErrorIfNeeded()
        }

        jdkComboBox.selectedJdk = jdkModel.projectSdk

        jvmCheckBox.addItemListener {
            jvmModuleNameComponent.isEnabled = jvmCheckBox.isSelected
            jdkComboBox.isEnabled = jvmCheckBox.isSelected
        }
        jsCheckBox.addItemListener {
            jsModuleNameComponent.isEnabled = jsCheckBox.isSelected
        }

        this.panel = panel(LCFlags.fillY) {
            row("Project structure:") { hierarchyKindComponent() }
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

    private fun showJsModuleErrorIfNeeded() {
        showErrorBalloonIfNeeded(jsModuleNameComponent, getJsModuleError())
    }

    private fun showJvmModuleErrorIfNeeded() {
        showErrorBalloonIfNeeded(jvmModuleNameComponent, getJvmModuleError())
    }

    private fun showCommonModuleErrorIfNeeded() {
        showErrorBalloonIfNeeded(commonModuleNameComponent, getCommonModuleError())
    }

    private fun showErrorBalloonIfNeeded(component: JComponent, errorMessage: String?) {
        if (showErrorBalloons && errorMessage != null) {
            ExternalSystemUiUtil.showBalloon(component, MessageType.ERROR, errorMessage)
        }
    }

    private fun stopSyncEditing() {
        if (!inSyncUpdate) {
            syncEditing = false
        }
    }

    private fun getRootModuleError() = if (rootModuleName.isEmpty()) "Please specify the root module name" else null

    private fun getCommonModuleError() = when {
        commonModuleName.isEmpty() ->
            "Please specify the common module name"

        commonModuleName.isNotEmpty() &&
                (commonModuleName == rootModuleName || commonModuleName == jvmModuleName || commonModuleName == jsModuleName) ->
            "The common module name should be distinct"

        else ->
            null
    }

    private fun getJvmModuleError() = when {
        jvmCheckBox.isSelected && jvmModuleName.isEmpty() ->
            "Please specify the JVM module name"

        jvmModuleName.isNotEmpty() &&
                (jvmModuleName == rootModuleName || jvmModuleName == commonModuleName || jvmModuleName == jsModuleName) ->
            "The JVM module name should be distinct"

        else ->
            null
    }

    private fun getJsModuleError()  = when {
        jsCheckBox.isSelected && jsModuleName.isEmpty() ->
            "Please specify the JS module name"

        jsModuleName.isNotEmpty() &&
                (jsModuleName == rootModuleName || jsModuleName == commonModuleName || jsModuleName == jvmModuleName) ->
            "The JS module name should be distinct"

        else ->
            null
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
        builder.commonModuleIsParent = commonModuleIsParent
        builder.commonModuleName = commonModuleName
        builder.jvmModuleName = jvmModuleName
        builder.jdk = jdk
        builder.jsModuleName = jsModuleName
    }

    override fun getComponent() = panel

    private val commonModuleIsParent: Boolean
        get() = hierarchyKindComponent.selectedIndex != 0
    private val rootModuleName: String
        get() = rootModuleNameComponent.text
    private val commonModuleName: String
        get() = commonModuleNameComponent.text
    private val jvmModuleName: String
        get() = if (jvmCheckBox.isSelected) jvmModuleNameComponent.text else ""
    private val jdk: Sdk?
        get() = if (jvmCheckBox.isSelected) jdkComboBox.selectedJdk else null
    private val jsModuleName: String
        get() = if (jsCheckBox.isSelected) jsModuleNameComponent.text else ""

    override fun validate(): Boolean {
        val errorMessage = getRootModuleError() ?: getCommonModuleError() ?: getJvmModuleError() ?: getJsModuleError() ?: return true
        throw ConfigurationException(errorMessage)
    }
}
