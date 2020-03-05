package org.jetbrains.kotlin.tools.projectWizard.wizard.ui

import org.jetbrains.kotlin.tools.projectWizard.core.context.ReadingContext
import org.jetbrains.kotlin.tools.projectWizard.wizard.IdeContext
import java.awt.BorderLayout
import javax.swing.JComponent

abstract class SubStep(
    ideContext: IdeContext
) : DynamicComponent(ideContext) {
    protected abstract fun buildContent(): JComponent

    final override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) {
        panel {
            add(buildContent(), BorderLayout.CENTER)
        }
    }
}

