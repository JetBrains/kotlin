package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.PopupHandler
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.UiConstants.GAP_BORDER_SIZE
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.ModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.SimpleTargetConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.TargetConfigurator
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

internal inline fun label(text: String, bold: Boolean = false, init: JBLabel.() -> Unit = {}) = JBLabel().apply {
    font = UIUtil.getButtonFont().deriveFont(if (bold) Font.BOLD else Font.PLAIN)
    this.text = text
    init()
}

inline fun panel(layout: LayoutManager = BorderLayout(), init: JPanel.() -> Unit = {}) = JPanel(layout).apply(init)

fun textField(defaultValue: String?, onUpdated: (value: String) -> Unit) = JBTextField(defaultValue).apply {
    document.addDocumentListener(object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) = onUpdated(this@apply.text)
        override fun removeUpdate(e: DocumentEvent?) = onUpdated(this@apply.text)
        override fun changedUpdate(e: DocumentEvent?) = onUpdated(this@apply.text)
    })
}

internal fun hyperlinkLabel(
    text: String,
    onClick: () -> Unit
) = DescriptionPanel().apply {
    cursor = Cursor(Cursor.HAND_CURSOR)
    addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) = onClick()
    })
    updateText("<a href='javascript:void(0)'>$text</a>".asHtml())
}

internal fun String.asHtml() = "<html><body>$this</body></html>"


val DisplayableSettingItem.htmlText
    get() = (text + greyText?.let { " <i>($greyText)</i>" }.orEmpty()).asHtml()

fun splitterFor(
    vararg components: JComponent,
    vertical: Boolean = false
) = components.reduce { left, right ->
    JBSplitter(vertical, 1f / components.size).apply {
        firstComponent = left
        secondComponent = right
        dividerWidth = 1
    }
}

val ModuleType.icon: Icon
    get() = when (this) {
        ModuleType.jvm -> KotlinIcons.SMALL_LOGO
        ModuleType.js -> KotlinIcons.JS
        ModuleType.native -> KotlinIcons.NATIVE
        ModuleType.common -> KotlinIcons.SMALL_LOGO
    }


val Module.icon: Icon
    get() = when (kind) {
        ModuleKind.target -> configurator.moduleType.icon
        ModuleKind.multiplatform -> AllIcons.Nodes.Module
        ModuleKind.singleplatform -> AllIcons.Nodes.Module
    }


val ModuleSubType.icon: Icon
    get() = moduleType.icon

val ModuleConfigurator.icon: Icon
    get() = when (this) {
        is SimpleTargetConfigurator -> moduleSubType.icon
        is TargetConfigurator -> moduleType.icon
        else -> AllIcons.Nodes.Module
    }

fun ToolbarDecorator.createPanelWithPopupHandler(popupTarget: JComponent) = createPanel().apply toolbarApply@{
    val actionGroup = DefaultActionGroup().apply {
        ToolbarDecorator.findAddButton(this@toolbarApply)?.let(this::add)
        ToolbarDecorator.findRemoveButton(this@toolbarApply)?.let(this::add)
    }
    PopupHandler.installPopupHandler(
        popupTarget,
        actionGroup,
        ActionPlaces.UNKNOWN,
        ActionManager.getInstance()
    )
}

fun <C : JComponent> C.bordered(
    needTopEmptyBorder: Boolean = true,
    needBottomEmptyBorder: Boolean = true,
    needInnerEmptyBorder: Boolean = true,
    needLineBorder: Boolean = true
) = apply {
    val lineBorder = if (needLineBorder) BorderFactory.createLineBorder(JBColor.border()) else null
    border = BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(
            if (needTopEmptyBorder) GAP_BORDER_SIZE else 0,
            0,
            if (needBottomEmptyBorder) GAP_BORDER_SIZE else 0,
            0
        ),
        when {
            needInnerEmptyBorder -> BorderFactory.createCompoundBorder(
                lineBorder,
                BorderFactory.createEmptyBorder(GAP_BORDER_SIZE, GAP_BORDER_SIZE, GAP_BORDER_SIZE, GAP_BORDER_SIZE)
            )
            else -> lineBorder
        }
    )
}

// Copied from com.intellij.ide.projectWizard.ProjectSettingsStep.addField
fun JPanel.addFieldWithLabel(label: String, field: JComponent) {
    val jLabel = JBLabel(label)
    jLabel.labelFor = field
    jLabel.verticalAlignment = SwingConstants.TOP
    add(
        jLabel,
        GridBagConstraints(
            0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
            GridBagConstraints.VERTICAL, JBUI.insets(5, 0, 5, 0), 4, 0
        )
    )
    add(
        field,
        GridBagConstraints(
            1, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, JBUI.insetsBottom(5), 0, 0
        )
    )
}

object UiConstants {
    const val GAP_BORDER_SIZE = 5
}