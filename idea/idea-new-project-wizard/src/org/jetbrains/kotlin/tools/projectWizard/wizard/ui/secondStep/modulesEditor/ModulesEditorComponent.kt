package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import com.intellij.ui.JBColor
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ListSettingType
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent


class ModulesEditorComponent(
    valuesReadingContext: ValuesReadingContext,
    oneEntrySelected: (data: DisplayableSettingItem?) -> Unit
) : SettingComponent<List<Module>, ListSettingType<Module>>(KotlinPlugin::modules.reference, valuesReadingContext) {
    private val tree: ModulesEditorTree =
        ModulesEditorTree(
            onSelected = { oneEntrySelected(it) },
            addModule = { component ->
                val isMppProject = KotlinPlugin::projectKind.value == ProjectKind.Multiplatform
                moduleCreator.create(
                    target = null, // The empty tree case
                    allowMultiplatform = isMppProject,
                    allowAndroid = isMppProject,
                    allowIos = isMppProject,
                    allModules = value ?: emptyList(),
                    createModule = { model.add(it) }
                )?.showInCenterOf(component)
            }
        )

    private val model = TargetsModel(tree, ::value)

    override fun onInit() {
        super.onInit()
        model.update()
    }

    private val moduleCreator = NewModuleCreator()

    private val toolbarDecorator = ModulesEditorToolbarDecorator(
        tree = tree,
        moduleCreator = moduleCreator,
        model = model,
        getModules = { value ?: emptyList() },
        isMultiplatformProject = { KotlinPlugin::projectKind.value == ProjectKind.Multiplatform }
    )

    override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            border = BorderFactory.createLineBorder(JBColor.border())
            add(toolbarDecorator.createToolPanel(), BorderLayout.CENTER)
            add(validationIndicator, BorderLayout.SOUTH)
        }
    }

    override val validationIndicator = ValidationIndicator(showText = true).apply {
        background = tree.background
    }
}
