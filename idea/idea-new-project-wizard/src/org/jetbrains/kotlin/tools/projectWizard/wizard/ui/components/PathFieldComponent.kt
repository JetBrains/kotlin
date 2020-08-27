package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.asPath
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingValidator
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.componentWithCommentAtBottom
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.withOnUpdatedListener
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComponent

class PathFieldComponent(
    context: Context,
    labelText: String? = null,
    description: String? = null,
    initialValue: Path? = null,
    validator: SettingValidator<Path>? = null,
    onValueUpdate: (Path) -> Unit = {}
) : UIComponent<Path>(
    context,
    labelText,
    validator,
    onValueUpdate
) {
    val textFieldWithBrowseButton = TextFieldWithBrowseButton().apply {
        textField.text = initialValue?.toString().orEmpty()
        textField.withOnUpdatedListener { path -> fireValueUpdated(path.trim().asPath()) }
        addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                null
            )
        )
    }

    override val alignTarget: JComponent? get() = textFieldWithBrowseButton

    override val uiComponent = componentWithCommentAtBottom(textFieldWithBrowseButton, description)

    override fun getValidatorTarget(): JComponent = textFieldWithBrowseButton.textField

    override fun updateUiValue(newValue: Path) = safeUpdateUi {
        textFieldWithBrowseButton.text = newValue.toString()
    }

    override fun getUiValue(): Path = Paths.get(textFieldWithBrowseButton.text.trim())

    override fun focusOn() {
        textFieldWithBrowseButton.textField.requestFocus()
    }
}