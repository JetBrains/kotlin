/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.idea.debugger.coroutines.view

import com.intellij.debugger.actions.DebuggerActions
import com.intellij.debugger.impl.DebuggerStateManager
import com.intellij.debugger.ui.impl.ThreadsPanel
import com.intellij.debugger.ui.impl.watch.DebuggerTree
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.debugger.coroutines.CoroutineDebuggerActions

/**
 * Added into ui in [CoroutineProjectConnectionListener.registerCoroutinesPanel]
 */
class CoroutinesPanel(project: Project, stateManager: DebuggerStateManager) : ThreadsPanel(project, stateManager) {

    override fun createTreeView(): DebuggerTree {
        return CoroutinesDebuggerTree(project)
    }

    override fun createPopupMenu(): ActionPopupMenu {
        val group = ActionManager.getInstance().getAction(DebuggerActions.THREADS_PANEL_POPUP) as DefaultActionGroup
        return ActionManager.getInstance().createActionPopupMenu(
            CoroutineDebuggerActions.COROUTINE_PANEL_POPUP, group)
    }

    override fun getData(dataId: String): Any? {
        return if (helpDataId(dataId)) HELP_ID else super.getData(dataId)
    }

    private fun helpDataId(dataId: String): Boolean = PlatformDataKeys.HELP_ID.`is`(dataId)

    companion object {
        val HELP_ID = "debugging.debugCoroutines"
    }

}