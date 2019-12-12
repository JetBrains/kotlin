package org.jetbrains.kotlin.tools.projectWizard.wizard

import com.intellij.ide.projectWizard.ProjectSettingsStep
import com.intellij.ide.util.projectWizard.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.ui.Messages
import com.intellij.util.SystemProperties
import org.jetbrains.kotlin.idea.framework.KotlinModuleSettingStep
import org.jetbrains.kotlin.idea.framework.KotlinTemplatesFactory
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.tools.projectWizard.core.Failure
import org.jetbrains.kotlin.tools.projectWizard.core.Success
import org.jetbrains.kotlin.tools.projectWizard.core.isSuccess
import org.jetbrains.kotlin.tools.projectWizard.core.onFailure
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.Plugins
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.PomWizardStepComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep.FirstWizardStepComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.SecondStepWizardComponent
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.nio.file.Paths
import javax.swing.JComponent
import com.intellij.openapi.module.Module as IdeaModule


class NewProjectWizardModuleBuilder : ModuleBuilder() {
    private val wizard = IdeWizard(Plugins.allPlugins, listOf(IdeaAndroidService()))

    companion object {
        const val MODULE_BUILDER_ID = "kotlin.newProjectWizard.builder"
    }

    override fun isAvailable(): Boolean = NewProjectWizardService.isEnabled

    private var wizardContext: WizardContext? = null

    override fun getModuleType(): ModuleType<*> = NewProjectWizardModuleType()
    override fun getParentGroup(): String = KotlinTemplatesFactory.KOTLIN_PARENT_GROUP_NAME

    override fun createWizardSteps(
        wizardContext: WizardContext,
        modulesProvider: ModulesProvider
    ): Array<ModuleWizardStep> {
        this.wizardContext = wizardContext
        return arrayOf(ModuleNewWizardSecondStep(wizard))
    }

    override fun commit(
        project: Project,
        model: ModifiableModuleModel?,
        modulesProvider: ModulesProvider?
    ): List<IdeaModule>? {
        val modulesModel = model ?: ModuleManager.getInstance(project).modifiableModel
        val success = wizard.apply(
            services = listOf(
                IdeaMavenService(project),
                IdeaGradleService(project),
                IdeaJpsService(project, modulesModel),
                IdeaFileSystemService(),
                IdeaAndroidService()
            ),
            phases = GenerationPhase.startingFrom(GenerationPhase.FIRST_STEP)
        ).onFailure { errors ->
            val errorMessages = errors.joinToString(separator = "\n") { it.message }
            Messages.showErrorDialog(project, errorMessages, "The following errors arose during project generation")
        }.isSuccess
        return when {
            !success -> null
            wizard.buildSystemType == BuildSystemType.Jps -> runWriteAction {
                modulesModel.modules.toList().onEach { setupModule(it) }
            }
            else -> emptyList()
        }
    }

    override fun modifySettingsStep(settingsStep: SettingsStep): ModuleWizardStep {
        updateProjectNameAndPomDate(settingsStep)

        return when (wizard.buildSystemType) {
            BuildSystemType.Jps -> {
                KotlinModuleSettingStep(
                    JvmPlatforms.defaultJvmPlatform,
                    this,
                    settingsStep,
                    wizardContext
                )
            }
            else -> PomWizardStep(settingsStep, wizard)
        }
    }

    private fun updateProjectNameAndPomDate(settingsStep: SettingsStep) {
        val suggestedProjectName = with(wizard.valuesReadingContext) {
            ProjectTemplatesPlugin::template.settingValue.suggestedProjectName.decapitalize()
        }
        settingsStep.moduleNameLocationSettings?.apply {
            val projectParentDirectory = moduleContentRoot.let { Paths.get(it).parent.toString() }
            moduleName = ProjectWizardUtil.findNonExistingFileName(projectParentDirectory, suggestedProjectName, "")
        }

        settingsStep.safeAs<ProjectSettingsStep>()?.bindModuleSettings()

        wizard.artifactId = suggestedProjectName
        wizard.groupId = SystemProperties.getUserName()?.let { "me.$it" } ?: suggestedProjectName
    }

    override fun getCustomOptionsStep(context: WizardContext?, parentDisposable: Disposable?) =
        ModuleNewWizardFirstStep(wizard)

    override fun setName(name: String) {
        wizard.projectName = name
    }


    override fun setModuleFilePath(path: String) = Unit

    override fun setContentEntryPath(moduleRootPath: String) {
        wizard.projectPath = Paths.get(moduleRootPath)
    }
}

abstract class WizardStep(protected val wizard: IdeWizard, private val phase: GenerationPhase) : ModuleWizardStep() {
    override fun updateDataModel() = Unit // model is updated on every UI action
    override fun validate(): Boolean =
        when (val result = with(wizard.valuesReadingContext) { with(wizard) { validate(setOf(phase)) } }) {
            is Success<*> -> true
            is Failure -> {
                val messages = result.errors.joinToString(separator = "\n") { it.message }
                throw ConfigurationException(messages)
            }
        }
}

private class PomWizardStep(
    originalSettingStep: SettingsStep,
    wizard: IdeWizard
) : WizardStep(wizard, GenerationPhase.PROJECT_GENERATION) {
    private val pomWizardStepComponent = PomWizardStepComponent(wizard.valuesReadingContext)

    init {
        originalSettingStep.addSettingsComponent(component)
        pomWizardStepComponent.onInit()

    }

    override fun getComponent(): JComponent = pomWizardStepComponent.component
}


class ModuleNewWizardFirstStep(wizard: IdeWizard) : WizardStep(wizard, GenerationPhase.FIRST_STEP) {
    private val component = FirstWizardStepComponent(wizard)
    override fun getComponent(): JComponent = component.component

    init {
        runPreparePhase()
        component.onInit()
    }

    private fun runPreparePhase() = ProgressManager.getInstance().runProcessWithProgressSynchronously(
        {
            wizard.apply(emptyList(), setOf(GenerationPhase.PREPARE)) { task ->
                ProgressManager.getInstance().progressIndicator.text = task.title ?: ""
            }
        },
        "",
        true,
        null
    )
}

class ModuleNewWizardSecondStep(wizard: IdeWizard) : WizardStep(wizard, GenerationPhase.SECOND_STEP) {
    private val component = SecondStepWizardComponent(wizard)
    override fun getComponent(): JComponent = component.component

    override fun _init() {
        component.onInit()
    }
}