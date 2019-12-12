package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.EmptyIcon
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.label
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JPanel

class ValidationIndicator(
    defaultText: String? = null,
    private val showText: Boolean
) : JPanel(BorderLayout()) {
    private val textLabel = defaultText?.let { label("$it: ") }
    private val errorLabel = label(" ").apply {
        foreground = JBColor.red
    }

    init {
        border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        textLabel?.let { add(it, BorderLayout.WEST) }
        add(errorLabel, BorderLayout.CENTER)
    }

    private fun setIcon(icon: Icon?) {
        if (textLabel != null) textLabel.icon = icon
        else errorLabel.icon = icon ?: EmptyIcon.ICON_16
    }


    var validationState: ValidationResult = ValidationResult.OK
        set(value) {
            field = value
            when (value) {
                ValidationResult.OK -> {
                    setIcon(null)
                    errorLabel.text = " "
                    toolTipText = null
                }
                is ValidationResult.ValidationError -> {
                    setIcon(AllIcons.General.Error)
                    val errors = value.messages.firstOrNull().orEmpty()
                    toolTipText = errors
                    if (showText) errorLabel.text = errors
                }
            }
        }
}