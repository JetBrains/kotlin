package org.jetbrains.kotlin.tools.projectWizard.wizard

import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.projectWizard.ProjectSettingsStep
import com.intellij.ide.util.projectWizard.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.ui.Messages
import com.intellij.util.SystemProperties
import org.jetbrains.kotlin.idea.configuration.ExperimentalFeatures
import org.jetbrains.kotlin.idea.framework.KotlinModuleSettingStep
import org.jetbrains.kotlin.idea.framework.KotlinTemplatesFactory
import org.jetbrains.kotlin.idea.projectWizard.ProjectCreationStats
import org.jetbrains.kotlin.idea.projectWizard.UiEditorUsageStats
import org.jetbrains.kotlin.idea.projectWizard.WizardStatsService
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.StringValidators
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.Plugins
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.service.IdeaServices
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.PomWizardStepComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.asHtml
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep.FirstWizardStepComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.runWithProgressBar
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.SecondStepWizardComponent
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
import java.nio.file.Paths
import javax.swing.JComponent
import com.intellij.openapi.module.Module as IdeaModule

/*
Have to override EmptyModuleBuilder here instead of just ModuleBuilder
As EmptyModuleBuilder has not expert panel option which are redundant
 */
class NewProjectWizardModuleBuilder : EmptyModuleBuilder() {
    private val wizard = IdeWizard(Plugins.allPlugins, IdeaServices.PROJECT_INDEPENDENT, isUnitTestMode = false)
    private val uiEditorUsagesStats = UiEditorUsageStats()

    override fun isOpenProjectSettingsAfter(): Boolean = false
    override fun canCreateModule(): Boolean = false
    override fun getPresentableName(): String = moduleType.name
    override fun getDescription(): String? = moduleType.description
    override fun getGroupName(): String? = moduleType.name
    override fun isTemplateBased(): Boolean = false

    companion object {
        const val MODULE_BUILDER_ID = "kotlin.newProjectWizard.builder"
        private const val DEFAULT_GROUP_ID = "me.user"
        private val projectNameValidator = StringValidators.shouldBeValidIdentifier("Project name", setOf('-', '_'))
        private const val INVALID_PROJECT_NAME_MESSAGE = "Invalid project name"
    }

    override fun isAvailable(): Boolean = ExperimentalFeatures.NewWizard.isEnabled

    private var wizardContext: WizardContext? = null
    private var pomValuesAreSet: Boolean = false

    override fun getModuleType(): ModuleType<*> = NewProjectWizardModuleType()
    override fun getParentGroup(): String = KotlinTemplatesFactory.KOTLIN_PARENT_GROUP_NAME

    override fun createWizardSteps(
        wizardContext: WizardContext,
        modulesProvider: ModulesProvider
    ): Array<ModuleWizardStep> {
        this.wizardContext = wizardContext
        return arrayOf(ModuleNewWizardSecondStep(wizard, uiEditorUsagesStats))
    }

    override fun commit(
        project: Project,
        model: ModifiableModuleModel?,
        modulesProvider: ModulesProvider?
    ): List<IdeaModule>? {
        val modulesModel = model ?: ModuleManager.getInstance(project).modifiableModel
        val success = wizard.apply(
            services = IdeaServices.createScopeDependent(project, modulesModel) +
                    IdeaServices.PROJECT_INDEPENDENT,
            phases = GenerationPhase.startingFrom(GenerationPhase.FIRST_STEP)
        ).onFailure { errors ->
            val errorMessages = errors.joinToString(separator = "\n") { it.message }
            Messages.showErrorDialog(project, errorMessages, "The following errors arose during project generation")
        }.isSuccess
        if (success) {
            val projectCreationStats = ProjectCreationStats(
                wizard.projectTemplate!!.title,
                wizard.buildSystemType!!.text
            )
            WizardStatsService.logDataOnProjectGenerated(
                projectCreationStats,
                uiEditorUsagesStats
            )
        }
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

    override fun validateModuleName(moduleName: String): Boolean {
        when (val validationResult = wizard.context.read {
            projectNameValidator.validate(this, moduleName)
        }) {
            ValidationResult.OK -> return true
            is ValidationResult.ValidationError -> {
                val message = validationResult.messages.firstOrNull() ?: INVALID_PROJECT_NAME_MESSAGE
                throw ConfigurationException(message, INVALID_PROJECT_NAME_MESSAGE)
            }
        }
    }

    private fun updateProjectNameAndPomDate(settingsStep: SettingsStep) {
        if (pomValuesAreSet) return
        val suggestedProjectName = wizard.context.read {
            ProjectTemplatesPlugin::template.settingValue.suggestedProjectName.decapitalize()
        }
        settingsStep.moduleNameLocationSettings?.apply {
            moduleName = wizard.projectName!!
            moduleContentRoot = wizard.projectPath!!.toString()
        }

        settingsStep.safeAs<ProjectSettingsStep>()?.bindModuleSettings()

        wizard.artifactId = suggestedProjectName
        wizard.groupId = suggestGroupId()
        pomValuesAreSet = true
    }

    private fun suggestGroupId(): String {
        val username = SystemProperties.getUserName() ?: return DEFAULT_GROUP_ID
        if (!username.matches("[\\w\\s]+".toRegex())) return DEFAULT_GROUP_ID
        val usernameAsGroupId = username.trim().toLowerCase().split("\\s+".toRegex()).joinToString(separator = ".")
        return "me.$usernameAsGroupId"
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
        when (val result = wizard.context.read { with(wizard) { validate(setOf(phase)) } }) {
            ValidationResult.OK -> true
            is ValidationResult.ValidationError -> {
                handleErrors(result)
                false
            }
        }

    protected open fun handleErrors(error: ValidationResult.ValidationError) {
        throw ConfigurationException(error.asHtml(), "Validation Error")
    }
}

private class PomWizardStep(
    originalSettingStep: SettingsStep,
    wizard: IdeWizard
) : WizardStep(wizard, GenerationPhase.PROJECT_GENERATION) {
    private val pomWizardStepComponent = PomWizardStepComponent(wizard.context)

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
        initDefaultProjectNameAndPathValues()
        component.onInit()
    }

    private fun runPreparePhase() = runWithProgressBar(title = "") {
        wizard.apply(emptyList(), setOf(GenerationPhase.PREPARE)) { task ->
            ProgressManager.getInstance().progressIndicator.text = task.title ?: ""
        }
    }

    private fun initDefaultProjectNameAndPathValues() {
        val suggestedProjectParentLocation = suggestProjectLocation()
        val suggestedProjectName = ProjectWizardUtil.findNonExistingFileName(suggestedProjectParentLocation, "untitled", "")
        wizard.context.writeSettings {
            StructurePlugin::name.reference.setValue(suggestedProjectName)
            StructurePlugin::projectPath.reference.setValue(suggestedProjectParentLocation / suggestedProjectName)
        }
    }

    // copied from com.intellij.ide.util.projectWizard.WizardContext.getProjectFileDirectory
    private fun suggestProjectLocation(): String {
        val lastProjectLocation = RecentProjectsManager.getInstance().lastProjectCreationLocation
        if (lastProjectLocation != null) {
            return lastProjectLocation.replace('/', File.separatorChar)
        }
        val userHome = SystemProperties.getUserHome()
        val productName = ApplicationNamesInfo.getInstance().lowercaseProductName
        return userHome.replace('/', File.separatorChar) + File.separator + productName.replace(" ", "") + "Projects"
    }
}

class ModuleNewWizardSecondStep(
    wizard: IdeWizard,
    uiEditorUsagesStats: UiEditorUsageStats
) : WizardStep(wizard, GenerationPhase.SECOND_STEP) {
    private val component = SecondStepWizardComponent(wizard, uiEditorUsagesStats)
    override fun getComponent(): JComponent = component.component

    override fun _init() {
        component.onInit()
    }

    override fun handleErrors(error: ValidationResult.ValidationError) {
        component.navigateTo(error)
    }
}