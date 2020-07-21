package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.secondStep.modulesEditor

import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.projectWizard.UiEditorUsageStats
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ListSettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.customPanel
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
            context = context,
            isTreeEditable = editable,
            addModule = { component ->
                val isMppProject = KotlinPlugin::projectKind.value == ProjectKind.Singleplatform
                moduleCreator.create(
                    target = null, // The empty tree case
                    allowMultiplatform = isMppProject,
                    allowSinglePlatformJs = isMppProject,
                    allowAndroid = isMppProject,
                    allowIos = isMppProject,
                    allModules = value ?: emptyList(),
                    createModule = model::add
                )?.showInCenterOf(component)
            }
        ).apply {
            if (editable) {
                border = JBUI.Borders.emptyRight(10)
            }
        }

    private val model = TargetsModel(tree, ::value, context, uiEditorUsagesStats)

    override fun onInit() {
        super.onInit()
        updateModel()
        if (editable) {
            value?.firstOrNull()?.let(tree::selectModule)
        }
    }

    fun updateModel() {
        model.update()
    }

    override fun navigateTo(error: ValidationResult.ValidationError) {
        val targetModule = error.target as? Module ?: return
        tree.selectModule(targetModule)
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
        customPanel {
            if (needBorder) {
                border = BorderFactory.createLineBorder(JBColor.border())
            }
            add(createEditorComponent(), BorderLayout.CENTER)
        }
    }

    private fun createEditorComponent() =
        when {
            editable -> toolbarDecorator!!.createToolPanel()
            else -> ScrollPaneFactory.createScrollPane(tree, true).apply {
                viewport.background = JBColor.PanelBackground
            }
        }.apply {
            preferredSize = Dimension(TREE_WIDTH, preferredSize.height)
        }

    override val validationIndicator: ValidationIndicator? = null

    companion object {
        private const val TREE_WIDTH = 260
    }
}
