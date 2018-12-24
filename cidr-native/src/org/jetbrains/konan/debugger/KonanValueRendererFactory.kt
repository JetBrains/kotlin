/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.debugger

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.xdebugger.XDebuggerManager
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriver
import com.jetbrains.cidr.execution.debugger.evaluation.ValueRendererFactory
import com.jetbrains.cidr.execution.debugger.evaluation.renderers.ValueRenderer
import org.jetbrains.konan.util.getKotlinNativePath
import org.jetbrains.konan.util.getKotlinNativeVersion
import java.nio.file.Path
import java.nio.file.Paths

class KonanValueRendererFactory : ValueRendererFactory {
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

private fun initLLDBDriver(project: Project, driver: LLDBDriver) {
    getKotlinNativePath(project)?.let { path ->
        // Apply custom formatting for Kotlin/Native structs:
        val lldbPrettyPrinters = Paths.get(path, "tools", "konan_lldb.py")

        // Patch pretty printers to make them compatible with Python 3.x. KT-29625
        val lldbPrettyPrintersPatched = if (getKotlinNativeVersion(path)?.isAtLeast(1, 1, 1) == true)
            lldbPrettyPrinters
        else
            patchPythonPrettyPrinters(lldbPrettyPrinters)

        driver.executeConsoleCommand("command script import \"$lldbPrettyPrintersPatched\"")

        // Re-draw debugger views that may be drawn by concurrent threads while formatting hasn't been applied:
        XDebuggerManager.getInstance(project).currentSession?.rebuildViews()
    }

    driver.executeConsoleCommand("settings set target.process.thread.step-avoid-regexp ^::Kotlin_")
}

private val LLDB_PRETTY_PRINTERS_PATCHED_LINE_REGEX = Regex("^\\s*print\\s.*")

private fun patchPythonPrettyPrinters(lldbPrettyPrinters: Path): Path {
    val lldbPrettyPrintersPatched = createTempDir().toPath().resolve("konan_lldb.py")

    lldbPrettyPrintersPatched.toFile().bufferedWriter().use { writer ->
        lldbPrettyPrinters.toFile().forEachLine { input ->
            val output = when {
                input.length <= 5 -> input
                input.startsWith("#") -> input
                input.matches(LLDB_PRETTY_PRINTERS_PATCHED_LINE_REGEX) -> input.replaceFirst("print", "print(") + ")"
                else -> input
            }

            writer.write(output)
            writer.write(System.lineSeparator())
        }
    }

    return lldbPrettyPrintersPatched
}
