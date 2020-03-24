package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.ActionButtonLook
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.UIUtilities
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.isSpecificError
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.DropDownSettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.IdeaBasedComponentValidator
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.SwingConstants
import javax.swing.SwingUtilities


class BuildSystemTypeSettingComponent(
    context: Context
) : SettingComponent<BuildSystemType, DropDownSettingType<BuildSystemType>>(
    BuildSystemPlugin::type.reference,
    context
), Disposable {
    override val forceLabelCenteringOffset: Int? = 2
    private val buttons = setting.type.values.map(::BuildSystemChooseButton)
    override val component = panel {
        row {
            buttons.forEach { button -> button(growX) }
        }
    }

    override val validationIndicator: ValidationIndicator = IdeaBasedComponentValidator(this, component)

    override fun onInit() {
        super.onInit()
        updateButtonsValidationState()
        updateButtons()
    }

    override fun navigateTo(error: ValidationResult.ValidationError) {
        if (validationIndicator.validationState.isSpecificError(error)) {
            component.requestFocus()
        }
    }

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        if (reference == BuildSystemPlugin::type.reference) {
            updateButtons()
        }
        if (reference == KotlinPlugin::projectKind.reference) {
            updateButtonsValidationState()
        }
    }

    private fun updateButtonsValidationState() {
        buttons.forEach(BuildSystemChooseButton::updateValidationState)
    }

    private fun validateBuildSystem(buildSystem: BuildSystemType) = read {
        setting.validator.validate(this, buildSystem)
    }

    private fun updateButtons() {
        buttons.forEach(BuildSystemChooseButton::updateSelectedBuildSystem)
    }

    override fun dispose() {}

    private inner class BuildSystemChooseButton(private val buildSystemType: BuildSystemType) : JComponent() {
        private val look = ActionButtonLook.SYSTEM_LOOK

        init {
            font = UIUtil.getLabelFont()
            preferredSize = Dimension(preferredSize.width, BUTTON_HEIGHT)
            minimumSize = Dimension(minimumSize.width, BUTTON_HEIGHT)
            cursor = Cursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) = modify {
                    if (isValidBuildSystem) {
                        BuildSystemPlugin::type.reference.setValue(buildSystemType)
                    }
                }

                override fun mouseEntered(e: MouseEvent?) {
                    hovered = true
                    repaint()
                }

                override fun mouseExited(e: MouseEvent?) {
                    hovered = false
                    repaint()
                }
            })
        }

        fun updateSelectedBuildSystem() {
            val currentBuildSystem = read { BuildSystemPlugin::type.settingValue }
            selected = currentBuildSystem == buildSystemType
            repaint()
        }

        fun updateValidationState() {
            val validationResult = validateBuildSystem(buildSystemType)
            isValidBuildSystem = validationResult.isOk
            cursor = Cursor(if (isValidBuildSystem) Cursor.HAND_CURSOR else Cursor.DEFAULT_CURSOR)
            toolTipText = validationResult.safeAs<ValidationResult.ValidationError>()
                ?.messages
                ?.firstOrNull()
                ?.takeUnless { isSelectedAndInvalid }
            repaint()
        }


        // mostly copied from com.intellij.openapi.actionSystem.impl.ActionButtonWithText.paintComponent
        override fun paintComponent(g: Graphics) {
            UISettings.setupAntialiasing(g)

            val fm = getFontMetrics(font)
            val viewRect = Rectangle(size)
            JBInsets.removeFrom(viewRect, insets)

            val iconRect = Rectangle()
            val textRect = Rectangle()
            val text = SwingUtilities.layoutCompoundLabel(
                this, fm, buildSystemType.text, null,
                SwingConstants.CENTER, SwingConstants.CENTER,
                SwingConstants.CENTER, SwingConstants.TRAILING,
                viewRect, iconRect, textRect, 0
            )

            look.paintLookBackground(g, viewRect, bgColor())

            g.color = if (isValidBuildSystem) foreground else UIUtil.getInactiveTextColor()
            UIUtilities.drawStringUnderlineCharAt(
                this, g, text, -1,
                textRect.x, textRect.y + fm.ascent
            )
        }

        private fun bgColor() = when {
            selected -> JBUI.CurrentTheme.ActionButton.pressedBackground()
            hovered -> JBUI.CurrentTheme.ActionButton.hoverBackground()
            else -> background
        }

        private var selected: Boolean = false
        private var hovered: Boolean = false
        private var isValidBuildSystem: Boolean = true

        private val isSelectedAndInvalid
            get() = selected && !isValidBuildSystem

    }

    companion object {
        private const val BUTTON_HEIGHT = 25
    }
}