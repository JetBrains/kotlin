package com.jetbrains.mobile.execution

import com.intellij.application.options.ModulesComboBox
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.mobile.MobileBundle
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ItemEvent
import java.util.*
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel

open class MobileRunConfigurationEditor(
    private val project: Project,
    private val modulePredicate: (Module) -> Boolean
) : SettingsEditor<MobileRunConfigurationBase>() {

    protected lateinit var modulesComboBox: ModulesComboBox
    private lateinit var executionTargetNames: SortedSet<String>
    private lateinit var appleDeviceComboBox: ComboBox<DeviceWrapper>
    private lateinit var androidDeviceComboBox: ComboBox<DeviceWrapper>

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

        fun addComboBox(text: String, devices: List<Device>): ComboBox<DeviceWrapper> {
            val appleDeviceLabel = JBLabel(text)
            panel.add(appleDeviceLabel, g.nextLine().next())

            val noDeviceSelected = arrayOf(DeviceWrapper(null))
            val deviceOptions = noDeviceSelected + devices.map(::DeviceWrapper)
            val comboBox = ComboBox(DefaultComboBoxModel(deviceOptions))
            comboBox.addItemListener { event ->
                val item = event.item as DeviceWrapper
                item.device ?: return@addItemListener
                val deviceName = item.toString()
                when (event.stateChange) {
                    ItemEvent.DESELECTED -> {
                        executionTargetNames.remove(deviceName)
                    }
                    ItemEvent.SELECTED -> {
                        executionTargetNames.add(deviceName)
                    }
                }
            }
            panel.add(comboBox, g.next().coverLine())
            appleDeviceLabel.labelFor = comboBox
            return comboBox
        }

        val deviceService = MobileDeviceService.getInstance(project)

        val appleDevices = deviceService.getAppleDevices()
        appleDeviceComboBox = addComboBox(MobileBundle.message("device.apple.select"), appleDevices)

        val androidDevices = deviceService.getAndroidDevices()
        androidDeviceComboBox = addComboBox(MobileBundle.message("device.android.select"), androidDevices)

        return panel
    }

    private class DeviceWrapper(val device: Device?) {
        override fun toString(): String = device?.displayName ?: MobileBundle.message("device.not.selected")
    }

    override fun applyEditorTo(runConfiguration: MobileRunConfigurationBase) {
        runConfiguration.module = modulesComboBox.selectedModule
        runConfiguration.executionTargetNames = executionTargetNames.toMutableList()
    }

    override fun resetEditorFrom(runConfiguration: MobileRunConfigurationBase) {
        modulesComboBox.selectedModule = runConfiguration.module
        executionTargetNames = runConfiguration.executionTargetNames.toSortedSet()

        fun selectItem(comboBox: ComboBox<DeviceWrapper>) {
            for (i in 0 until comboBox.itemCount) {
                if (comboBox.getItemAt(i).toString() in executionTargetNames) {
                    comboBox.selectedIndex = i
                    break
                }
            }
        }
        selectItem(appleDeviceComboBox)
        selectItem(androidDeviceComboBox)
    }
}