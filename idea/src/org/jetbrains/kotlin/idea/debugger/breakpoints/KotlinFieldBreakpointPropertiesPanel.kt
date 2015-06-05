/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger.breakpoints

import com.intellij.debugger.DebuggerBundle
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.DialogUtil
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel
import com.intellij.xdebugger.impl.breakpoints.XBreakpointBase
import org.jetbrains.kotlin.idea.JetBundle
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.Box
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.properties.Delegates

public class KotlinFieldBreakpointPropertiesPanel: XBreakpointCustomPropertiesPanel<XLineBreakpoint<KotlinPropertyBreakpointProperties>>() {
    private var myWatchInitializationCheckBox: JCheckBox by Delegates.notNull()
    private var myWatchAccessCheckBox: JCheckBox by Delegates.notNull()
    private var myWatchModificationCheckBox: JCheckBox by Delegates.notNull()

    override fun getComponent(): JComponent {
        myWatchInitializationCheckBox = JCheckBox(JetBundle.message("debugger.field.watchpoints.properties.panel.field.initialization.label"))
        myWatchAccessCheckBox = JCheckBox(JetBundle.message("debugger.field.watchpoints.properties.panel.field.access.label"))
        myWatchModificationCheckBox = JCheckBox(JetBundle.message("debugger.field.watchpoints.properties.panel.field.modification.label"))

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
        mainPanel.setBorder(IdeBorderFactory.createTitledBorder(DebuggerBundle.message("label.group.watch.events"), true))
        return mainPanel
    }

    override fun loadFrom(breakpoint: XLineBreakpoint<KotlinPropertyBreakpointProperties>) {
        myWatchInitializationCheckBox.setSelected(breakpoint.getProperties().WATCH_INITIALIZATION)
        myWatchAccessCheckBox.setSelected(breakpoint.getProperties().WATCH_ACCESS)
        myWatchModificationCheckBox.setSelected(breakpoint.getProperties().WATCH_MODIFICATION)
    }

    override fun saveTo(breakpoint: XLineBreakpoint<KotlinPropertyBreakpointProperties>) {
        var changed = breakpoint.getProperties().WATCH_ACCESS != myWatchAccessCheckBox.isSelected()
        breakpoint.getProperties().WATCH_ACCESS = myWatchAccessCheckBox.isSelected()

        changed = breakpoint.getProperties().WATCH_MODIFICATION != myWatchModificationCheckBox.isSelected() || changed
        breakpoint.getProperties().WATCH_MODIFICATION = myWatchModificationCheckBox.isSelected()

        changed = breakpoint.getProperties().WATCH_INITIALIZATION != myWatchInitializationCheckBox.isSelected() || changed
        breakpoint.getProperties().WATCH_INITIALIZATION = myWatchInitializationCheckBox.isSelected()

        if (changed) {
            (breakpoint as XBreakpointBase<*, *, *>).fireBreakpointChanged()
        }
    }
}