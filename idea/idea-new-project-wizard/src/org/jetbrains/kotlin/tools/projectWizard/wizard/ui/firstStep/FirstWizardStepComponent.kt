package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import com.intellij.icons.AllIcons
import com.intellij.ide.util.projectWizard.JavaModuleBuilder
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.util.Condition
import com.intellij.ui.JBColor
import com.intellij.ui.TitledSeparator
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.kotlin.idea.framework.KotlinModuleBuilder
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.path
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor.ModulesEditorComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.TitledComponentsList
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.createSettingComponent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent

class FirstWizardStepComponent(ideWizard: IdeWizard) : WizardStepComponent(ideWizard.context) {
    private val context = ideWizard.context
    private val projectSettingsComponent = ProjectSettingsComponent(ideWizard).asSubComponent()
    private val projectPreviewComponent = ProjectPreviewComponent(context).asSubComponent()

    override val component: JComponent = borderPanel {
        addToCenter(projectSettingsComponent.component)
        addToRight(projectPreviewComponent.component)
    }
}

class ProjectSettingsComponent(ideWizard: IdeWizard) : DynamicComponent(ideWizard.context) {
    private val context = ideWizard.context
    private val projectTemplateComponent = ProjectTemplateSettingComponent(context).asSubComponent()
    private val buildSystemSetting = BuildSystemTypeSettingComponent(context).asSubComponent()
    private val buildSystemAdditionalSettingsComponent = BuildSystemAdditionalSettingsComponent(ideWizard).asSubComponent()

    private val nameAndLocationComponent = TitledComponentsList(
        listOf(
            StructurePlugin::name.reference.createSettingComponent(context),
            StructurePlugin::projectPath.reference.createSettingComponent(context),
            projectTemplateComponent,
            buildSystemSetting
        ),
        context
    ).asSubComponent()

    override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            row {
                nameAndLocationComponent.component(growX)
            }
            row {
                buildSystemAdditionalSettingsComponent.component(growX)
            }
        }.apply {
            border = JBUI.Borders.empty(10)
        }
    }

    private var locationWasUpdatedByHand: Boolean = false
    private var artifactIdWasUpdatedByHand: Boolean = false

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        when (reference?.path) {
            StructurePlugin::name.path -> {
                tryUpdateLocationByProjectName()
                tryArtifactIdByProjectName()
            }
            StructurePlugin::artifactId.path -> {
                artifactIdWasUpdatedByHand = true
            }
            StructurePlugin::projectPath.path -> {
                locationWasUpdatedByHand = true
            }
        }
    }

    private fun tryUpdateLocationByProjectName() {
        if (!locationWasUpdatedByHand) {
            val location = read { StructurePlugin::projectPath.settingValue }
            if (location.parent != null) modify {
                StructurePlugin::projectPath.reference.setValue(location.parent.resolve(StructurePlugin::name.settingValue))
                locationWasUpdatedByHand = false
            }
        }
    }

    private fun tryArtifactIdByProjectName() {
        if (!artifactIdWasUpdatedByHand) modify {
            StructurePlugin::artifactId.reference.setValue(StructurePlugin::name.settingValue)
            artifactIdWasUpdatedByHand = false
        }
    }
}

class BuildSystemAdditionalSettingsComponent(ideWizard: IdeWizard) : DynamicComponent(ideWizard.context) {
    private val pomSettingsList = PomSettingsComponent(ideWizard.context).asSubComponent()
    private val kotlinJpsRuntimeComponent = KotlinJpsRuntimeComponent(ideWizard).asSubComponent()

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        if (reference == BuildSystemPlugin::type.reference) {
            updateBuildSystemComponent()
        }
    }

    override fun onInit() {
        super.onInit()
        updateBuildSystemComponent()
    }

    private fun updateBuildSystemComponent() {
        val buildSystemType = read { BuildSystemPlugin::type.settingValue() }
        val state = buildSystemType.state()
        section.updateTitleAndComponent(state.sectionTitle, state.component)
    }

    private enum class State(val sectionTitle: String) {
        POM("Artifact Coordinates"), JPS("Kotlin Runtime")
    }

    private fun BuildSystemType.state() =
        if (this == BuildSystemType.Jps) State.JPS
        else State.POM

    private val State.component
        get() = when (this) {
            State.POM -> pomSettingsList.component
            State.JPS -> kotlinJpsRuntimeComponent.component
        }

    private val section = HideableSection(State.POM.sectionTitle, State.POM.component)

    override val component: JComponent = section
}

private class PomSettingsComponent(context: Context) : TitledComponentsList(
    listOf(
        StructurePlugin::groupId.reference.createSettingComponent(context),
        StructurePlugin::artifactId.reference.createSettingComponent(context),
        StructurePlugin::version.reference.createSettingComponent(context)
    ),
    context
)

class KotlinJpsRuntimeComponent(ideWizard: IdeWizard) : DynamicComponent(ideWizard.context) {
    private val javaModuleBuilder = JavaModuleBuilder()
    private val jdkComboBox = JdkComboBox(
        ProjectSdksModel().apply { reset(null) },
        Condition(javaModuleBuilder::isSuitableSdkType)
    ).apply {
        ideWizard.jpsData.jdk = selectedJdk
        addActionListener {
            ideWizard.jpsData.jdk = selectedJdk
        }
    }
    override val component: JComponent = panel {
        row("Project JDK:") {
            borderPanel { addToCenter(jdkComboBox) }.addBorder(JBUI.Borders.empty(0, 7))(growX)
        }
        row("Kotlin Runtime:") {
            ideWizard.jpsData.libraryOptionsPanel.simplePanel(growX)
        }
    }
}

@Suppress("SpellCheckingInspection")
private class HideableSection(text: String, private var component: JComponent) : BorderLayoutPanel() {
    private val titledSeparator = TitledSeparator(text)
    private var isExpanded = false

    init {
        updateComponent(component)
        titledSeparator.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) = update(!isExpanded)
        })
    }

    fun updateTitleAndComponent(newTitle: String, newComponent: JComponent) {
        titledSeparator.text = newTitle
        updateComponent(newComponent)
    }

    private fun updateComponent(newComponent: JComponent) {
        component = newComponent
        removeAll()
        addToTop(titledSeparator)
        addToCenter(newComponent)
        update(isExpanded)
    }

    private fun update(isExpanded: Boolean) {
        this.isExpanded = isExpanded
        component.isVisible = isExpanded
        titledSeparator.label.icon = if (isExpanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
    }
}

class ProjectPreviewComponent(context: Context) : DynamicComponent(context) {
    private val modulesEditorComponent = ModulesEditorComponent(
        context,
        null,
        needBorder = false,
        editable = false,
        oneEntrySelected = {}
    ).asSubComponent()

    override val component: JComponent = borderPanel {
        border = JBUI.Borders.empty(10)
        addToTop(label("Preview", bold = true).addBorder(JBUI.Borders.emptyBottom(5)))
        addToCenter(modulesEditorComponent.component)
    }.addBorder(JBUI.Borders.customLine(JBColor.border(), 0, /*left*/1, 0, 0))

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        if (reference == ProjectTemplatesPlugin::template.reference) {
            modulesEditorComponent.updateModel()
        }
    }
}