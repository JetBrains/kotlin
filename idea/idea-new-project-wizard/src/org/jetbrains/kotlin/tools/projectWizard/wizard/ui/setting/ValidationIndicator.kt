package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.label
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel

interface ValidationIndicator {
    fun updateValidationState(newState: ValidationResult)
}

class IdeaBasedComponentValidator(
    parentDisposable: Disposable,
    private val jComponent: JComponent
) : ValidationIndicator {
    private val validator = ComponentValidator(parentDisposable).installOn(jComponent)

    override fun updateValidationState(newState: ValidationResult) {
        validator.updateInfo(
            newState.safeAs<ValidationResult.ValidationError>()
                ?.messages
                ?.firstOrNull()
                ?.let { ValidationInfo(it, jComponent) }
        )
    }
}

class AlwaysShownValidationIndicator(
    private val showText: Boolean,
    private val onClickAction: ((ValidationResult.ValidationError) -> Unit)? = null
) : JPanel(BorderLayout()), ValidationIndicator {
    private val errorLabel = label(" ")

    private var currentError: ValidationResult.ValidationError? = null

    init {
        errorLabel.icon = AllIcons.General.Error
        background = JBUI.CurrentTheme.Validator.errorBackgroundColor()
        border =
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBUI.CurrentTheme.Validator.errorBorderColor()),
                JBUI.Borders.empty(4, 8)
            )
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

    override fun updateValidationState(newState: ValidationResult) {
        isVisible = !newState.isOk
        when (newState) {
            ValidationResult.OK -> {
                currentError = null
            }
            is ValidationResult.ValidationError -> {
                val errors = newState.messages.firstOrNull().orEmpty()
                if (showText) errorLabel.text = errors
                currentError = newState
            }
        }
    }
}