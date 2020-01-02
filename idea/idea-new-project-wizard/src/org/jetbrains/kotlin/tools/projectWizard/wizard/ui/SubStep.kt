package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import org.jetbrains.kotlin.tools.projectWizard.core.ValuesReadingContext
import java.awt.BorderLayout
import javax.swing.JComponent

abstract class SubStep(
    val valuesReadingContext: ValuesReadingContext
) : DynamicComponent(valuesReadingContext) {
    protected abstract fun buildContent(): JComponent

    final override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            add(buildContent(), BorderLayout.CENTER)
        }
    }
}

