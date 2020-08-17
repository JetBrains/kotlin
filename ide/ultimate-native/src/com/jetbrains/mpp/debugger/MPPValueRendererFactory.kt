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
import com.jetbrains.mpp.MPPWorkspace
import java.nio.file.Paths

private fun initLLDBDriver(project: Project, driver: LLDBDriver) {
    val workspace = MPPWorkspace.getInstance(project)
    workspace.konanHome?.let { konanHome ->
        // Apply custom formatting for Kotlin/Native structs:
        val lldbPrettyPrinters = Paths.get(konanHome, "tools", "konan_lldb.py")
        driver.executeConsoleCommand("command script import \"$lldbPrettyPrinters\"")

        // Re-draw debugger views that may be drawn by concurrent threads while formatting hasn't been applied:
        XDebuggerManager.getInstance(project).currentSession?.rebuildViews()
    }

    driver.executeConsoleCommand("settings set target.process.thread.step-avoid-regexp ^::Kotlin_")
}

class MPPValueRendererFactory : ValueRendererFactory {
    override fun createRenderer(context: ValueRendererFactory.FactoryContext): ValueRenderer? {
        val process = context.physicalValue.process
        if (process.getUserData(prettyPrinters) == true) return null
        process.putUserData(prettyPrinters, true)

        process.postCommand { driver ->
            if (driver !is LLDBDriver) return@postCommand
            initLLDBDriver(process.project, driver)
        }
        return null
    }

    companion object {
        private val prettyPrinters = Key.create<Boolean>("KotlinPrettyPrinters")
    }
}