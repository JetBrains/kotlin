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
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.externalSystem.service.project.wizard.ExternalModuleSettingsStep
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleBuilder
import org.jetbrains.plugins.gradle.service.settings.GradleProjectSettingsControl

class KotlinGradleMultiplatformModuleBuilder : GradleModuleBuilder() {
    var jvmModuleName: String? = null
    var jsModuleName: String? = null

    override fun getBuilderId() = "kotlin.gradle.multiplatform"

    override fun getNodeIcon() = KotlinIcons.MPP

    override fun getPresentableName() = "Kotlin (Multiplatform - Experimental)"

    override fun getDescription() =
            "Multiplatform projects allow reusing the same code between multiple platforms supported by Kotlin. Such projects are built with Gradle."

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
        super.createWizardSteps(wizardContext, modulesProvider)  // initializes GradleModuleBuilder.myWizardContext
        return arrayOf(KotlinGradleMultiplatformWizardStep(this, wizardContext),
                       ExternalModuleSettingsStep(
                               wizardContext, this, GradleProjectSettingsControl(externalProjectSettings))
        )
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        super.setupRootModel(modifiableRootModel)
        val module = modifiableRootModel.module
        val buildScriptData = getBuildScriptData(module) ?: return
        val sdk = modifiableRootModel.sdk
        GradleKotlinMPPCommonFrameworkSupportProvider().addSupport(buildScriptData, sdk)
    }

    override fun setupModule(module: Module) {
        super.setupModule(module)

        val contentRoot = module.rootManager.contentRoots.firstOrNull() ?: return
        setupPlatformModule(module, contentRoot, jvmModuleName, GradleKotlinMPPJavaFrameworkSupportProvider())
        setupPlatformModule(module, contentRoot, jsModuleName, GradleKotlinMPPJSFrameworkSupportProvider())

        val settingsGradle = contentRoot.findChild("settings.gradle")
        settingsGradle?.let {
            module.project.executeCommand("Update settings.gradle") {
                val doc = FileDocumentManager.getInstance().getDocument(it) ?: return@executeCommand
                doc.insertString(doc.textLength, "include '$jvmModuleName', '$jsModuleName'")
                FileDocumentManager.getInstance().saveDocument(doc)
            }
        }
    }

    private fun setupPlatformModule(module: Module,
                                    contentRoot: VirtualFile,
                                    platformModuleName: String?,
                                    supportProvider: GradleKotlinFrameworkSupportProvider) {
        if (platformModuleName.isNullOrEmpty()) return

        val sdk = module.rootManager.sdk
        val moduleDir = contentRoot.createChildDirectory(this, platformModuleName!!)
        val buildGradle = moduleDir.createChildData(null, "build.gradle")
        val buildScriptData = BuildScriptDataBuilder(buildGradle)
        supportProvider.addSupport(buildScriptData, sdk)
        buildScriptData.addDependencyNotation("expectedBy project(\":\")")
        VfsUtil.saveText(buildGradle, buildScriptData.build())
    }
}
