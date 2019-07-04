/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.sampler

import com.intellij.ide.actions.ImportModuleAction
import com.intellij.ide.util.newProjectWizard.AddModuleWizard
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.ui.components.JBTextField
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.framework.SAMPLER_SYSTEM_ID
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportProvider
import java.io.File
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class KotlinSamplerModuleBuilder : AbstractExternalModuleBuilder<SamplerProjectSettings>(SAMPLER_SYSTEM_ID, SamplerProjectSettings()) {

    private val samples = mutableMapOf<String, SampleInfo>()

    private val settingsComponents = SampleTemplateList()

    private val searchField: JBTextField = JBTextField().apply {
        emptyText.text = "input some int"
    }

    override fun getGroupName(): String = "Kotlin"

    override fun getNodeIcon(): Icon = KotlinIcons.SMALL_LOGO

    override fun getBuilderId() = "KotlinSamplerBuilderId"

    override fun getModuleType(): ModuleType<*> = JavaModuleType.getModuleType()

    override fun createModule(moduleModel: ModifiableModuleModel): Module {
        val oldPath = moduleFilePath
        val file = File(oldPath)
        val path = file.parent + "/" + file.name.toLowerCase()
        moduleFilePath = path

        val settings = externalProjectSettings
        settings.externalProjectPath = path
        settings.isUseAutoImport = false
        settings.isCreateEmptyContentRootDirectories = false

        ModuleBuilder.deleteModuleFile(oldPath)

        val module: Module = moduleModel.newModule(path, moduleType.id)
        moduleModel.commit()

        setupModule(module)
        return module
    }

    // TODO
    override fun setupRootModel(model: ModifiableRootModel) {
        val info = settingsComponents.list.selectedValue
        val moduleDir = File(contentEntryPath!!)
        val sample = SamplerInteraction.getSample(info.id)!!
        createTemplate(sample, moduleDir)

        when (sample.type) {
            BuildSystemType.GRADLE -> {
                val buildFile = sample.files.find { it.extension == "gradle" }!!
                val gradleFile = File(moduleDir, buildFile.fullPath())
                val projectDataManager = ServiceManager.getService(ProjectDataManager::class.java)
                val gradleProjectImportBuilder = GradleProjectImportBuilder(projectDataManager)
                val gradleProjectImportProvider = GradleProjectImportProvider(gradleProjectImportBuilder)
                val wizard = AddModuleWizard(model.project, gradleFile.getPath(), gradleProjectImportProvider)
                if (wizard.stepCount <= 0 || wizard.showAndGet()) {
                    ImportModuleAction.createFromWizard(model.project, wizard)
                }
            }
            BuildSystemType.MAVEN -> TODO()
            BuildSystemType.NONE -> {
            }
        }
    }

    private fun createTemplate(sample: Sample, moduleDir: File) {
        sample.files.forEach { sf ->
            val file = File(
                moduleDir,
                "${sf.path}/${sf.name}.${sf.extension}"
            )
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
            file.printWriter().use { out -> out.print(sf.content) }
        }
    }

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
        settingsComponents.setItems(SamplerInteraction.getSamplerInfos())
        val step = object : ModuleWizardStep() {
            override fun updateDataModel() {
            }

            override fun getComponent(): JComponent = settingsComponents.mainPanel
        }

        searchField.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {
            }

            override fun insertUpdate(e: DocumentEvent?) {
            }

            override fun removeUpdate(e: DocumentEvent?) {
            }
        })

        return arrayOf(step)
    }

    private fun downloadSamples() {
        doWithProgress(
            // TODO report errors
            {
                SamplerInteraction.getSamplerInfos().forEach { samples[it.name] = it }
            },
            "Downloading list of templates..."
        )
    }


    private fun doWithProgress(body: () -> Unit, title: String) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            body, title, false, null
        )
    }
}


class SamplerProjectSettings : ExternalProjectSettings() {
    override fun clone(): ExternalProjectSettings {
        return SamplerProjectSettings()
    }
}
