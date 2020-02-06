/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.breakpoints

import com.intellij.debugger.DebuggerBundle
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.DialogUtil
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase
import org.jetbrains.kotlin.idea.debugger.KotlinDebuggerCoreBundle
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.properties.Delegates

class KotlinFieldBreakpointPropertiesPanel : XBreakpointCustomPropertiesPanel<XLineBreakpoint<KotlinPropertyBreakpointProperties>>() {
    private var myWatchInitializationCheckBox: JCheckBox by Delegates.notNull()
    private var myWatchAccessCheckBox: JCheckBox by Delegates.notNull()
    private var myWatchModificationCheckBox: JCheckBox by Delegates.notNull()

    override fun getComponent(): JComponent {
        myWatchInitializationCheckBox =
            JCheckBox(KotlinDebuggerCoreBundle.message("field.watchpoint.properties.initialization"))
        myWatchAccessCheckBox = JCheckBox(KotlinDebuggerCoreBundle.message("field.watchpoint.properties.access"))
        myWatchModificationCheckBox =
            JCheckBox(KotlinDebuggerCoreBundle.message("field.watchpoint.properties.modification"))

        DialogUtil.registerMnemonic(myWatchInitializationCheckBox)
        DialogUtil.registerMnemonic(myWatchAccessCheckBox)
        DialogUtil.registerMnemonic(myWatchModificationCheckBox)

        fun Box.addNewPanelForCheckBox(checkBox: JCheckBox) {
            val panel = JPanel(BorderLayout())
            panel.add(checkBox, BorderLayout.NORTH)
            this.add(panel)
        }

        val watchBox = Box.createVerticalBox()
        watchBox.addNewPanelForCheckBox(myWatchInitializationCheckBox)
        watchBox.addNewPanelForCheckBox(myWatchAccessCheckBox)
        watchBox.addNewPanelForCheckBox(myWatchModificationCheckBox)

        val mainPanel = JPanel(BorderLayout())
        val innerPanel = JPanel(BorderLayout())
        innerPanel.add(watchBox, BorderLayout.CENTER)
        innerPanel.add(Box.createHorizontalStrut(3), BorderLayout.WEST)
        innerPanel.add(Box.createHorizontalStrut(3), BorderLayout.EAST)
        mainPanel.add(innerPanel, BorderLayout.NORTH)
        mainPanel.border = IdeBorderFactory.createTitledBorder(DebuggerBundle.message("label.group.watch.events"), true)
        return mainPanel
    }

    override fun loadFrom(breakpoint: XLineBreakpoint<KotlinPropertyBreakpointProperties>) {
        myWatchInitializationCheckBox.isSelected = breakpoint.properties.watchInitialization
        myWatchAccessCheckBox.isSelected = breakpoint.properties.watchAccess
        myWatchModificationCheckBox.isSelected = breakpoint.properties.watchModification
    }

    override fun saveTo(breakpoint: XLineBreakpoint<KotlinPropertyBreakpointProperties>) {
        var changed = breakpoint.properties.watchAccess != myWatchAccessCheckBox.isSelected
        breakpoint.properties.watchAccess = myWatchAccessCheckBox.isSelected

        changed = breakpoint.properties.watchModification != myWatchModificationCheckBox.isSelected || changed
        breakpoint.properties.watchModification = myWatchModificationCheckBox.isSelected

        changed = breakpoint.properties.watchInitialization != myWatchInitializationCheckBox.isSelected || changed
        breakpoint.properties.watchInitialization = myWatchInitializationCheckBox.isSelected

        if (changed) {
            (breakpoint as XBreakpointBase<*, *, *>).fireBreakpointChanged()
        }
    }
}