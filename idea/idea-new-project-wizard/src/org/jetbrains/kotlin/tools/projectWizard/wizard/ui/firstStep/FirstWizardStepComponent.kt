package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.TitledSeparator
import com.intellij.ui.layout.panel
import com.intellij.util.io.size
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.path
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeWizard
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor.ModulesEditorComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.TitledComponentsList
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.createSettingComponent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent

class FirstWizardStepComponent(wizard: IdeWizard) : WizardStepComponent(wizard.context) {
    private val context = wizard.context
    private val projectSettingsComponent = ProjectSettingsComponent(context).asSubComponent()
    private val projectPreviewComponent = ProjectPreviewComponent(context).asSubComponent()

    override val component: JComponent = borderPanel {
        addToCenter(projectSettingsComponent.component)
        addToRight(projectPreviewComponent.component)
    }
}

class ProjectSettingsComponent(context: Context) : DynamicComponent(context) {
    private val projectTemplateComponent = ProjectTemplateSettingComponent(context).asSubComponent()
    private val buildSystemSetting = BuildSystemTypeSettingComponent(context).asSubComponent()
    private val buildSystemAdditionalSettingsComponent = BuildSystemAdditionalSettingsComponent(context).asSubComponent()

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

class BuildSystemAdditionalSettingsComponent(context: Context) : DynamicComponent(context) {
    private val settingsList = TitledComponentsList(
        listOf(
            StructurePlugin::groupId.reference.createSettingComponent(context),
            StructurePlugin::artifactId.reference.createSettingComponent(context),
            StructurePlugin::version.reference.createSettingComponent(context)
        ),
        context
    ).asSubComponent()

    override val component: JComponent = HideableSection("Artifact Coordinates", settingsList.component)
}

@Suppress("SpellCheckingInspection")
private class HideableSection(text: String, private val component: JComponent) : BorderLayoutPanel() {
    private val titledSeparator = TitledSeparator(text)
    private var isExpanded = false

    init {
        addToTop(titledSeparator)
        addToCenter(component)
        update(isExpanded)

        addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) = update(!isExpanded)
        })
    }

    private fun update(isExpanded: Boolean) {
        this.isExpanded = isExpanded
        component.isVisible = this.isExpanded
        titledSeparator.label.icon = if (this.isExpanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
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