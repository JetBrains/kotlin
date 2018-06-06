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
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleBuilder
import org.jetbrains.plugins.gradle.service.settings.GradleProjectSettingsControl
import javax.swing.Icon

class KotlinGradleMultiplatformModuleBuilder : GradleModuleBuilder() {
    var commonModuleName: String? = null
    var jvmModuleName: String? = null
    var jdk: Sdk? = null
    var jsModuleName: String? = null

    override fun getBuilderId() = "kotlin.gradle.multiplatform"

    override fun getNodeIcon(): Icon = KotlinIcons.MPP

    override fun getPresentableName() = "Kotlin (Multiplatform - Experimental)"

    override fun getDescription() =
        "Multiplatform projects allow reusing the same code between multiple platforms supported by Kotlin. Such projects are built with Gradle."

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
        super.createWizardSteps(wizardContext, modulesProvider)  // initializes GradleModuleBuilder.myWizardContext
        return arrayOf(
            KotlinGradleMultiplatformWizardStep(this, wizardContext),
            ExternalModuleSettingsStep(
                wizardContext, this, GradleProjectSettingsControl(externalProjectSettings)
            )
        )
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        super.setupRootModel(modifiableRootModel)
        if (commonModuleName.isNullOrEmpty()) {
            val module = modifiableRootModel.module
            val buildScriptData = getBuildScriptData(module) ?: return
            val sdk = modifiableRootModel.sdk
            GradleKotlinMPPCommonFrameworkSupportProvider().addSupport(buildScriptData, module, sdk, true)
        }
    }

    override fun setupModule(module: Module) {
        try {
            module.gradleModuleBuilder = this

            super.setupModule(module)

            val contentRoot = module.rootManager.contentRoots.firstOrNull() ?: return
            setupCommonModule(module, contentRoot)
            setupPlatformModule(module, contentRoot, jvmModuleName, GradleKotlinMPPJavaFrameworkSupportProvider(), jdk)
            setupPlatformModule(module, contentRoot, jsModuleName, GradleKotlinMPPJSFrameworkSupportProvider())

            updateSettingsScript(module) {
                val includedModules = listOfNotNull(commonModuleName, jvmModuleName, jsModuleName).filter { it.isNotEmpty() }
                if (includedModules.isNotEmpty()) {
                    it.addIncludedModules(includedModules)
                }
            }
        } finally {
            flushSettingsGradleCopy(module)
        }
    }

    private fun setupChildModule(
        rootModule: Module,
        contentRoot: VirtualFile,
        childModuleName: String?,
        sdk: Sdk? = null,
        extendScript: (BuildScriptDataBuilder, Sdk?) -> Unit = { _, _ -> }
    ) {
        if (childModuleName.isNullOrEmpty()) return

        val moduleDir = contentRoot.createChildDirectory(this, childModuleName!!)
        val buildGradle = moduleDir.createChildData(null, "build.gradle")
        val buildScriptData = BuildScriptDataBuilder(buildGradle)
        extendScript(buildScriptData, sdk ?: rootModule.rootManager.sdk)
        VfsUtil.saveText(buildGradle, buildScriptData.buildConfigurationPart() + buildScriptData.buildMainPart())
    }

    private fun setupCommonModule(
        rootModule: Module,
        contentRoot: VirtualFile
    ) = setupChildModule(rootModule, contentRoot, commonModuleName) { builder, sdk ->
        GradleKotlinMPPCommonFrameworkSupportProvider().addSupport(builder, rootModule, sdk, true)
    }

    private fun setupPlatformModule(
        rootModule: Module,
        contentRoot: VirtualFile,
        platformModuleName: String?,
        supportProvider: GradleKotlinFrameworkSupportProvider,
        sdk: Sdk? = null
    ) = setupChildModule(rootModule, contentRoot, platformModuleName, sdk) { builder, finalSdk ->
        supportProvider.addSupport(builder, rootModule, finalSdk, !commonModuleName.isNullOrEmpty())
        val dependency = commonModuleName ?: ""
        builder.addDependencyNotation("expectedBy project(\":$dependency\")")
    }
}
