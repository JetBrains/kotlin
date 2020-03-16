package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import com.intellij.ui.JBColor
import org.jetbrains.kotlin.idea.projectWizard.UiEditorUsageStats
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ListSettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JComponent

class ModulesEditorComponent(
    context: Context,
    uiEditorUsagesStats: UiEditorUsageStats?,
    needBorder: Boolean,
    private val editable: Boolean,
    oneEntrySelected: (data: DisplayableSettingItem?) -> Unit
) : SettingComponent<List<Module>, ListSettingType<Module>>(KotlinPlugin::modules.reference, context) {
    private val tree: ModulesEditorTree =
        ModulesEditorTree(
            onSelected = { oneEntrySelected(it) },
            addModule = { component ->
                val isMppProject = KotlinPlugin::projectKind.value == ProjectKind.Singleplatform
                moduleCreator.create(
                    target = null, // The empty tree case
                    allowMultiplatform = isMppProject,
                    allowSinglepaltformJs = isMppProject,
                    allowAndroid = isMppProject,
                    allowIos = isMppProject,
                    allModules = value ?: emptyList(),
                    createModule = model::add
                )?.showInCenterOf(component)
            }
        )

    private val model = TargetsModel(tree, ::value, context, uiEditorUsagesStats)

    override fun onInit() {
        super.onInit()
        updateModel()
    }

    fun updateModel() {
        model.update()
    }

    private val moduleCreator = NewModuleCreator()

    private val toolbarDecorator = if (editable) ModulesEditorToolbarDecorator(
        tree = tree,
        moduleCreator = moduleCreator,
        model = model,
        getModules = { value ?: emptyList() },
        isMultiplatformProject = { KotlinPlugin::projectKind.value != ProjectKind.Singleplatform }
    ) else null

    override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            if (needBorder) {
                border = BorderFactory.createLineBorder(JBColor.border())
            }
            add(createEditorComponent(), BorderLayout.CENTER)
        }
    }

    private fun createEditorComponent() =
        if (editable) toolbarDecorator!!.createToolPanel()
        else tree.apply {
            preferredSize = Dimension(TREE_WIDTH, tree.height)
        }

    override val validationIndicator: ValidationIndicator? = null

    companion object {
        private const val TREE_WIDTH = 260
    }
}
