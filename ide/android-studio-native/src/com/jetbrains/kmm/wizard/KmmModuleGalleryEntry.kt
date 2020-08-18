/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import com.android.sdklib.SdkVersionInfo
import com.android.tools.adtui.validation.ValidatorPanel
import com.android.tools.idea.device.FormFactor
import com.android.tools.idea.npw.model.ExistingProjectModelData
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.module.ConfigureModuleStep
import com.android.tools.idea.npw.module.ModuleGalleryEntry
import com.android.tools.idea.npw.module.ModuleModel
import com.android.tools.idea.npw.platform.AndroidVersionsInfo
import com.android.tools.idea.observable.core.BoolValueProperty
import com.android.tools.idea.observable.core.OptionalValueProperty
import com.android.tools.idea.ui.wizard.StudioWizardStepPanel
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.Recipe
import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBCheckBox
import com.jetbrains.kmm.KmmBundle
import javax.swing.Icon
import javax.swing.JTextField


class KmmModuleModel(
    project: Project,
    projectSyncInvoker: ProjectSyncInvoker
) : ModuleModel(
    KmmBundle.message("wizard.module.title"),
    KmmBundle.message("wizard.module.createCommand"),
    true,
    ExistingProjectModelData(project, projectSyncInvoker)
) {
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
                setModuleRoots(template.get().paths, project.basePath!!, moduleName.get(), this@KmmModuleModel.packageName.get())
            }
        }

        override val loggingEvent = AndroidStudioEvent.TemplateRenderer.CUSTOM_TEMPLATE_RENDERER

        override val recipe: Recipe =
            { td -> generateKmmModule(project, td as ModuleTemplateData, generateTests.get(), generatePackTask.get()) }
    }
}


class KmmConfigureModuleStep(model: KmmModuleModel) : ConfigureModuleStep<KmmModuleModel>(
    model,
    FormFactor.MOBILE,
    title = KmmBundle.message("wizard.module.title")
) {
    private val generateTestsCheck = JBCheckBox(KmmBundle.message("wizard.module.generateTestsLabel"))
    private val generatePackTaskCheck = JBCheckBox(KmmBundle.message("wizard.module.generatePackForXcodeLabel"))
    private val packageNameField = JTextField()

    init {
        bindModuleStepSettings(bindings, listeners, model, packageNameField, generateTestsCheck, generatePackTaskCheck)
    }

    private val panel = moduleStepDialogPanel(
        moduleName,
        packageName,
        packageNameField,
        generateTestsCheck,
        generatePackTaskCheck)

    override val validatorPanel = ValidatorPanel(this, StudioWizardStepPanel.wrappedWithVScroll(panel))
}

internal class KmmModuleGalleryEntry : ModuleGalleryEntry {
    override val icon: Icon = IconLoader.findIcon("/META-INF/kmm-project-logo.png")!!
    override val name: String = KmmBundle.message("wizard.module.title")
    override val description: String = KmmBundle.message("wizard.module.description")
    override fun toString(): String = name

    override fun createStep(project: Project, projectSyncInvoker: ProjectSyncInvoker, moduleParent: String?): SkippableWizardStep<*> {
        return KmmConfigureModuleStep(
            KmmModuleModel(
                project,
                projectSyncInvoker
            )
        )
    }
}