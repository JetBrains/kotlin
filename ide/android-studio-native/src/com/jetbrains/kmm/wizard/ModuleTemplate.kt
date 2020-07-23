/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import com.android.sdklib.SdkVersionInfo
import com.android.tools.adtui.validation.ValidatorPanel
import com.android.tools.idea.device.FormFactor
import com.android.tools.idea.npw.labelFor
import com.android.tools.idea.npw.model.ExistingProjectModelData
import com.android.tools.idea.npw.model.NewProjectModel
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.module.ConfigureModuleStep
import com.android.tools.idea.npw.module.ModuleDescriptionProvider
import com.android.tools.idea.npw.module.ModuleGalleryEntry
import com.android.tools.idea.npw.module.ModuleModel
import com.android.tools.idea.npw.platform.AndroidVersionsInfo
import com.android.tools.idea.observable.core.BoolProperty
import com.android.tools.idea.observable.core.BoolValueProperty
import com.android.tools.idea.observable.core.OptionalValueProperty
import com.android.tools.idea.observable.expressions.Expression
import com.android.tools.idea.observable.ui.SelectedProperty
import com.android.tools.idea.observable.ui.TextProperty
import com.android.tools.idea.ui.wizard.StudioWizardStepPanel
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.Recipe
import com.android.tools.idea.wizard.template.RecipeExecutor
import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.panel
import com.jetbrains.kmm.KMM_LOG
import com.jetbrains.kmm.wizard.templates.*
import org.jetbrains.android.refactoring.getProjectProperties
import javax.swing.Icon
import javax.swing.JTextField


private const val KMM_MODULE_NAME = "KMM Module"
private const val HMMP_SUPPORT_KEY = "kotlin.mpp.enableGranularSourceSetsMetadata"

private fun String.asDirectory(): String = this.replace(".", "/")

fun RecipeExecutor.generateKmmModule(project: Project, data: ModuleTemplateData, generateTests: Boolean, generatePackTask: Boolean) {
    val packageName = data.packageName
    val moduleDir = data.rootDir
    val srcDir = moduleDir.resolve("src")

    addIncludeToSettings(data.name)

    val propertiesFile = project.getProjectProperties(true)

    if (propertiesFile != null) {
        propertiesFile.findPropertyByKey(HMMP_SUPPORT_KEY)?.setValue("true") ?: propertiesFile.addProperty(HMMP_SUPPORT_KEY, "true")
    } else {
        KMM_LOG.error("Failed to update gradle.properties during $KMM_MODULE_NAME instantiation")
    }

    val templates = mutableListOf(CommonMainPlatformKt, CommonMainGreetingKt, IosMainPlatformKt, AndroidMainPlatformKt)

    if (generateTests) {
        templates += listOf(IosTestKt, AndroidTestKt)
    }

    for (template in templates) {
        val sourceSetOut = srcDir.resolve(template.sourceSet).resolve("kotlin").resolve(packageName.asDirectory())
        createDirectory(sourceSetOut)
        save(template.render(packageName), sourceSetOut.resolve(template.fileName))
    }

    val entryPointsPackage = "$packageName.android"

    createDirectory(srcDir.resolve("androidMain/kotlin/" + entryPointsPackage.asDirectory()))
    save(buildFileKts(data.name, generateTests, generatePackTask), moduleDir.resolve("build.gradle.kts"))
    save(androidManifestXml(entryPointsPackage), srcDir.resolve("androidMain/AndroidManifest.xml"))
}

class KmmWizardModel(project: Project, moduleParent: String, projectSyncInvoker: ProjectSyncInvoker) :
    ModuleModel("KMM Module", "Create new KMM Module", true, ExistingProjectModelData(project, projectSyncInvoker)) {

    override val androidSdkInfo = OptionalValueProperty(
        AndroidVersionsInfo().apply { loadLocalVersions() }
            .getKnownTargetVersions(FormFactor.MOBILE, SdkVersionInfo.LOWEST_ACTIVE_API)
            .first() // we don't care which one do we use, we just have to pass something, it is not going to be used
    )

    var generateTests = BoolValueProperty(false)

    var generatePackTask = BoolValueProperty(false)

    override val renderer = object : ModuleTemplateRenderer() {

        override fun init() {
            super.init()

            moduleTemplateDataBuilder.apply {
                setModuleRoots(template.get().paths, project.basePath!!, moduleName.get(), this@KmmWizardModel.packageName.get())
            }
        }

        override val loggingEvent = AndroidStudioEvent.TemplateRenderer.CUSTOM_TEMPLATE_RENDERER

        override val recipe: Recipe =
            { td -> generateKmmModule(project, td as ModuleTemplateData, generateTests.get(), generatePackTask.get()) }
    }
}

class ConfigureKmmModuleStep(model: KmmWizardModel) :
    ConfigureModuleStep<KmmWizardModel>(model, FormFactor.MOBILE, title = KMM_MODULE_NAME) {

    private val generateTestsCheck = JBCheckBox("Generate Test Stubs")
    private val generatePackTaskCheck = JBCheckBox("Generate packForXcode")
    private val packageNameField = JTextField()

    init {
        bindings.bindTwoWay(SelectedProperty(generateTestsCheck), model.generateTests)
        bindings.bindTwoWay(SelectedProperty(generatePackTaskCheck), model.generatePackTask)

        val isPackageNameSynced: BoolProperty = BoolValueProperty(true)

        val basePackage = NewProjectModel.getSuggestedProjectPackage()
        val computedPackageName: Expression<String> = object : Expression<String>(model.moduleName) {
            override fun get() = "${basePackage}.${NewProjectModel.nameToJavaPackage(model.moduleName.get())}"
        }

        val packageNameText = TextProperty(packageNameField)
        bindings.bind(model.packageName, packageNameText)
        bindings.bind(packageNameText, computedPackageName, isPackageNameSynced)

        listeners.listen(packageNameText) { value: String -> isPackageNameSynced.set(value == computedPackageName.get()) }
    }

    private val panel: DialogPanel = panel {
        row {
            cell {
                labelFor("Module Name", moduleName)
            }
            moduleName()
        }

        row {
            labelFor("Package Name", packageName)
            packageNameField()
        }

        row {
            cell {
                generateTestsCheck()
            }
        }

        row {
            cell {
                generatePackTaskCheck()
            }
        }
    }

    override val validatorPanel = ValidatorPanel(this, StudioWizardStepPanel.wrappedWithVScroll(panel))
}

private class KmmModuleTemplateGalleryEntry : ModuleGalleryEntry {
    override val icon: Icon = IconLoader.findIcon("/META-INF/kmm-project-logo.png")!!
    override val name: String = KMM_MODULE_NAME

    override val description: String = "Kotlin Multiplatform Mobile module for sharing logic between iOS and Android mobile applications"
    override fun toString(): String = name

    override fun createStep(project: Project, projectSyncInvoker: ProjectSyncInvoker, moduleParent: String?): SkippableWizardStep<*> {
        return ConfigureKmmModuleStep(KmmWizardModel(project, moduleParent ?: "No parent", projectSyncInvoker))
    }
}

class ModuleTemplate : ModuleDescriptionProvider {
    override fun getDescriptions(project: Project): Collection<ModuleGalleryEntry> = listOf(KmmModuleTemplateGalleryEntry())
}