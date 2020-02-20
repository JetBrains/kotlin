package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import org.jetbrains.kotlin.tools.projectWizard.core.ReadingContext
import java.awt.BorderLayout
import javax.swing.JComponent

abstract class SubStep(
    readingContext: ReadingContext
) : DynamicComponent(readingContext) {
    protected abstract fun buildContent(): JComponent

    final override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            add(buildContent(), BorderLayout.CENTER)
        }
    }
}

