package org.jetbrains.kotlin.tools.projectWizard.wizard.ui


import org.jetbrains.kotlin.tools.projectWizard.core.Context
import java.awt.BorderLayout
import javax.swing.JComponent

abstract class SubStep(
    context: Context
) : DynamicComponent(context) {
    protected abstract fun buildContent(): JComponent

    final override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        customPanel {
            add(buildContent(), BorderLayout.CENTER)
        }
    }
}

