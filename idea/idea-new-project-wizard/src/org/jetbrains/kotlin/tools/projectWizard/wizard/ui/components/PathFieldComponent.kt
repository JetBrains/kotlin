package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import org.jetbrains.kotlin.tools.projectWizard.core.entity.StringValidator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingValidator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settingValidator
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.Component
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.textField
import java.awt.BorderLayout
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.Icon
import javax.swing.JComponent

class PathFieldComponent(
    valuesReadingContext: ValuesReadingContext,
    labelText: String? = null,
    initialValue: Path? = null,
    validator: SettingValidator<Path>? = null,
    onValueUpdate: (Path) -> Unit = {}
) : UIComponent<Path>(
    valuesReadingContext,
    labelText,
    validator,
    onValueUpdate
) {
    override val uiComponent: TextFieldWithBrowseButton = TextFieldWithBrowseButton(
        textField(initialValue?.toString().orEmpty()) { path -> fireValueUpdated(Paths.get(path)) }
    ).apply {
        addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                null
            )
        )
    }

    override fun updateUiValue(newValue: Path)= safeUpdateUi {
        uiComponent.text = newValue.toString()
    }

    override fun getUiValue(): Path = Paths.get(uiComponent.text)
}