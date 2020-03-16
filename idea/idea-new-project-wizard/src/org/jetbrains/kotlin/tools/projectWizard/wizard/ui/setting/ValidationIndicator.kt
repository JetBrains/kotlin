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