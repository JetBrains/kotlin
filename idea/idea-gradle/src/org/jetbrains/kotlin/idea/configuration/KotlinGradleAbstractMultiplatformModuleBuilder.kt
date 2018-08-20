/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.externalSystem.service.project.wizard.ExternalModuleSettingsStep
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleBuilder
import org.jetbrains.plugins.gradle.service.settings.GradleProjectSettingsControl
import javax.swing.Icon

abstract class KotlinGradleAbstractMultiplatformModuleBuilder : GradleModuleBuilder() {
    override fun getNodeIcon(): Icon = KotlinIcons.MPP

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
        super.createWizardSteps(wizardContext, modulesProvider)  // initializes GradleModuleBuilder.myWizardContext
        return arrayOf(
            // Let us have to edit project name only
            ExternalModuleSettingsStep(wizardContext, this, GradleProjectSettingsControl(externalProjectSettings))
        )
    }

    override fun setupModule(module: Module) {
        try {
            module.gradleModuleBuilder = this
            super.setupModule(module)

            val rootDir = module.rootManager.contentRoots.firstOrNull() ?: return
            val buildGradle = rootDir.findOrCreateChildData(null, "build.gradle")
            val builder = BuildScriptDataBuilder(buildGradle)
            GradleKotlinMPPFrameworkSupportProvider().addSupport(builder, module, sdk = null, specifyPluginVersionIfNeeded = true)
            VfsUtil.saveText(buildGradle, builder.buildConfigurationPart() + builder.buildMainPart() + buildMultiPlatformPart())
        } finally {
            flushSettingsGradleCopy(module)
        }
    }

    protected abstract fun buildMultiPlatformPart(): String

    companion object {
        const val productionSuffix = "Main"

        const val testSuffix = "Test"
    }
}