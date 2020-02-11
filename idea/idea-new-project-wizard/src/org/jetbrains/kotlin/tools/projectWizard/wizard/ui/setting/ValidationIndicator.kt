package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.util.ui.EmptyIcon
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.asHtml
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.label
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JPanel

class ValidationIndicator(
    defaultText: String? = null,
    private val showText: Boolean,
    private val onClickAction: ((ValidationResult.ValidationError) -> Unit)? = null
) : JPanel(BorderLayout()) {
    private val textLabel = defaultText?.let { label("$it: ") }
    private val errorLabel = label(" ").apply {
        foreground = JBColor.red
    }

    private var currentError: ValidationResult.ValidationError? = null


    init {
        border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        textLabel?.let { add(it, BorderLayout.WEST) }
        add(errorLabel, BorderLayout.CENTER)

        if (onClickAction != null) {
            cursor = Cursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseListener {
                override fun mouseReleased(p0: MouseEvent?) {}
                override fun mouseEntered(p0: MouseEvent?) {}
                override fun mouseExited(p0: MouseEvent?) {}
                override fun mousePressed(p0: MouseEvent?) {}

                override fun mouseClicked(p0: MouseEvent?) {
                    currentError?.let(onClickAction)
                }
            })
        }
    }

    private fun setIcon(icon: Icon?) {
        if (textLabel != null) textLabel.icon = icon
        else errorLabel.icon = icon ?: EmptyIcon.ICON_16
    }

    private fun String.linkifyErrorText() =
        if (onClickAction == null) this
        else "<u>$this</u>".asHtml()

    var validationState: ValidationResult = ValidationResult.OK
        set(value) {
            field = value
            when (value) {
                ValidationResult.OK -> {
                    setIcon(null)
                    errorLabel.text = " "
                    toolTipText = null
                    currentError = null
                }
                is ValidationResult.ValidationError -> {
                    setIcon(AllIcons.General.Error)
                    val errors = value.messages.firstOrNull().orEmpty()
                    toolTipText = errors
                    if (showText) errorLabel.text = errors.linkifyErrorText()
                    currentError = value
                }
            }
        }
}