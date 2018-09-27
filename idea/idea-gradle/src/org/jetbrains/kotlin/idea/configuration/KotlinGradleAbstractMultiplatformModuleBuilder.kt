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
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.TargetSupportException
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import org.jetbrains.plugins.gradle.service.project.wizard.GradleModuleBuilder
import org.jetbrains.plugins.gradle.service.settings.GradleProjectSettingsControl
import org.jetbrains.plugins.gradle.settings.DistributionType
import javax.swing.Icon

abstract class KotlinGradleAbstractMultiplatformModuleBuilder(
    val mppInApplication: Boolean = false
) : GradleModuleBuilder() {
    override fun getNodeIcon(): Icon = KotlinIcons.MPP

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
        super.createWizardSteps(wizardContext, modulesProvider)  // initializes GradleModuleBuilder.myWizardContext
        return arrayOf(
            // Let us have to edit project name only
            ExternalModuleSettingsStep(wizardContext, this, GradleProjectSettingsControl(externalProjectSettings))
        )
    }

    private fun setupMppModule(module: Module, parentDir: VirtualFile): VirtualFile? {
        val moduleDir = parentDir.createChildDirectory(this, "app")
        val buildGradle = moduleDir.createChildData(null, "build.gradle")
        val builder = BuildScriptDataBuilder(buildGradle)
        builder.setupAdditionalDependenciesForApplication()
        GradleKotlinMPPFrameworkSupportProvider().addSupport(builder, module, sdk = null, specifyPluginVersionIfNeeded = true)
        VfsUtil.saveText(buildGradle, builder.buildConfigurationPart() + builder.buildMainPart() + buildMultiPlatformPart())
        return moduleDir
    }

    private fun enableGradleMetadataPreview(rootDir: VirtualFile) {
        val settingsGradle = rootDir.findOrCreateChildData(null, "settings.gradle")
        val previousText = settingsGradle.inputStream.bufferedReader().use { it.readText() }
        settingsGradle.bufferedWriter().use {
            it.write(previousText)
            if (previousText.isNotEmpty()) {
                it.newLine()
            }
            it.write("enableFeaturePreview('GRADLE_METADATA')")
            it.newLine()
        }
    }

    override fun setupModule(module: Module) {
        try {
            module.gradleModuleBuilder = this
            super.setupModule(module)

            val rootDir = module.rootManager.contentRoots.firstOrNull() ?: return
            val buildGradle = rootDir.findOrCreateChildData(null, "build.gradle")
            if (mppInApplication) {
                setupMppModule(module, rootDir)
            }
            val builder = BuildScriptDataBuilder(buildGradle)
            builder.setupAdditionalDependencies()
            if (shouldEnableGradleMetadataPreview) {
                enableGradleMetadataPreview(rootDir)
            }
            val buildGradleText = if (!mppInApplication) {
                GradleKotlinMPPFrameworkSupportProvider().addSupport(builder, module, sdk = null, specifyPluginVersionIfNeeded = true)
                builder.buildConfigurationPart() + builder.buildMainPart() + buildMultiPlatformPart()
            } else {
                builder.buildConfigurationPart() + builder.buildMainPart()
            }
            VfsUtil.saveText(buildGradle, buildGradleText)
            if (mppInApplication) {
                updateSettingsScript(module) {
                    it.addIncludedModules(listOf(":app"))
                }
            }
            createProjectSkeleton(module, rootDir)
            if (externalProjectSettings.distributionType == DistributionType.DEFAULT_WRAPPED) {
                setGradleWrapperToUseVersion(rootDir, "4.7")
            }

            if (notImportedCommonSourceSets) GradlePropertiesFileFacade.forProject(module.project).addNotImportedCommonSourceSetsProperty()
        } finally {
            flushSettingsGradleCopy(module)
        }
    }

    private fun setGradleWrapperToUseVersion(rootDir: VirtualFile, version: String) {
        val wrapperDir = rootDir.createChildDirectory(null, "gradle").createChildDirectory(null, "wrapper")
        val wrapperProperties = wrapperDir.createChildData(null, "gradle-wrapper.properties").bufferedWriter()
        wrapperProperties.use {
            it.write(
                """
    distributionBase=GRADLE_USER_HOME
    distributionPath=wrapper/dists
    distributionUrl=https\://services.gradle.org/distributions/gradle-$version-bin.zip
    zipStoreBase=GRADLE_USER_HOME
    zipStorePath=wrapper/dists
                """.trimIndent()
            )
        }
    }

    protected abstract fun buildMultiPlatformPart(): String

    protected open fun BuildScriptDataBuilder.setupAdditionalDependencies() {}

    protected open fun BuildScriptDataBuilder.setupAdditionalDependenciesForApplication() {}

    protected fun VirtualFile.bufferedWriter() = getOutputStream(this).bufferedWriter()

    protected fun VirtualFile.createKotlinSampleFileWriter(
        sourceRootName: String,
        languageName: String = "kotlin",
        fileName: String = "Sample.kt"
    ) = createChildDirectory(this, sourceRootName)
        .createChildDirectory(this, languageName)
        .createChildDirectory(this, "sample")
        .createChildData(this, fileName)
        .bufferedWriter()

    protected open fun createProjectSkeleton(module: Module, rootDir: VirtualFile) {}

    protected open val notImportedCommonSourceSets = false

    protected open val shouldEnableGradleMetadataPreview = false

    protected val defaultNativeTarget by lazy {
        try {
            HostManager.host
        } catch (e: TargetSupportException) {
            KonanTarget.IOS_X64
        }
    }

    // Examples: ios_x64 -> ios, macos_x64 -> macos, wasm32 -> wasm.
    protected val KonanTarget.userTargetName: String
        get() {
            val index = name.indexOfAny("_0123456789".toCharArray())
            return if (index > 0) name.substring(0, index) else name
        }

    companion object {
        const val productionSuffix = "Main"

        const val testSuffix = "Test"
    }
}