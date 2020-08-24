/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.debugger

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.xdebugger.XDebuggerManager
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriver
import com.jetbrains.cidr.execution.debugger.evaluation.ValueRendererFactory
import com.jetbrains.cidr.execution.debugger.evaluation.renderers.ValueRenderer
import com.jetbrains.mpp.workspace.WorkspaceBase
import java.nio.file.Path
import java.nio.file.Paths


abstract class KonanValueRendererFactory : ValueRendererFactory {
    protected abstract fun getWorkspace(project: Project): WorkspaceBase

    protected open fun getPrintersPath(project: Project): Path? {
        val konanHome = getWorkspace(project).konanHome ?: return null
        return Paths.get(konanHome, "tools", "konan_lldb.py")
    }

    private fun initPrinters(driver: LLDBDriver, project: Project, printersPath: Path) {
        // Apply custom formatting for Kotlin/Native structs:
        driver.executeConsoleCommand("command script import \"$printersPath\"")

        // Re-draw debugger views that may be drawn by concurrent threads while formatting hasn't been applied:
        XDebuggerManager.getInstance(project).currentSession?.rebuildViews()

        driver.executeConsoleCommand("settings set target.process.thread.step-avoid-regexp ^::Kotlin_")

    }

    override fun createRenderer(context: ValueRendererFactory.FactoryContext): ValueRenderer? {
        val process = context.physicalValue.process

        if (process.getUserData(PRETTY_PRINTERS) == true) {
            return null
        }

        process.putUserData(PRETTY_PRINTERS, true)

        process.postCommand { driver ->
            if (driver !is LLDBDriver) return@postCommand
            val printersPath = getPrintersPath(process.project) ?: return@postCommand
            initPrinters(driver, process.project, printersPath)
        }

        return null
    }

    companion object {
        private val PRETTY_PRINTERS = Key.create<Boolean>("KotlinPrettyPrinters")
    }
}