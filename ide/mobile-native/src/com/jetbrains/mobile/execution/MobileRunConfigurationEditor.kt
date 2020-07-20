package com.jetbrains.mobile.execution

import com.intellij.application.options.ModulesComboBox
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.mobile.MobileBundle
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

open class MobileRunConfigurationEditor(
    private val project: Project,
    private val modulePredicate: (Module) -> Boolean
) : SettingsEditor<MobileRunConfigurationBase>() {

    protected lateinit var modulesComboBox: ModulesComboBox

    override fun createEditor(): JComponent {
        val panel = JPanel(GridBagLayout())

        val g = GridBag()
            .setDefaultFill(GridBagConstraints.BOTH)
            .setDefaultAnchor(GridBagConstraints.CENTER)
            .setDefaultWeightX(1, 1.0)
            .setDefaultInsets(0, JBUI.insets(0, 0, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP))
            .setDefaultInsets(1, JBUI.insetsBottom(UIUtil.DEFAULT_VGAP))

        val modulesLabel = JBLabel(MobileBundle.message("run.configuration.editor.module"))
        panel.add(modulesLabel, g.nextLine().next())
        modulesComboBox = ModulesComboBox()
        modulesComboBox.setModules(project.allModules().filter(modulePredicate))
        panel.add(modulesComboBox, g.next().coverLine())
        modulesLabel.labelFor = modulesComboBox

        return panel
    }

    override fun applyEditorTo(runConfiguration: MobileRunConfigurationBase) {
        runConfiguration.module = modulesComboBox.selectedModule
    }

    override fun resetEditorFrom(runConfiguration: MobileRunConfigurationBase) {
        modulesComboBox.selectedModule = runConfiguration.module
    }
}