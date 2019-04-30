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
import org.jetbrains.konan.util.getKotlinNativeHome
import org.jetbrains.konan.util.getKotlinNativeVersion
import org.jetbrains.kotlin.konan.MetaVersion
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
    getKotlinNativeHome(project)?.let { kotlinNativeHome ->
        // Apply custom formatting for Kotlin/Native structs:
        val lldbPrettyPrinters = getPrettyPrintersLocation(kotlinNativeHome)

        driver.executeConsoleCommand("command script import \"$lldbPrettyPrinters\"")

        // Re-draw debugger views that may be drawn by concurrent threads while formatting hasn't been applied:
        XDebuggerManager.getInstance(project).currentSession?.rebuildViews()
    }

    driver.executeConsoleCommand("settings set target.process.thread.step-avoid-regexp ^::Kotlin_")
}

private fun getPrettyPrintersLocation(kotlinNativeHome: String): Path {
    // For versions of Kotlin/Native older than "1.2-release" use improved pretty printers bundled with the plugin
    val usePrettyPrintersFromPlugin = getKotlinNativeVersion(kotlinNativeHome)?.run {
        when (major) {
            0 -> true
            1 -> when (minor) {
                0, 1 -> true
                2 -> meta != MetaVersion.RELEASE
                else -> false
            }
            else -> false
        }
    } ?: false

    if (!usePrettyPrintersFromPlugin)
        return Paths.get(kotlinNativeHome, "tools", "konan_lldb.py")

    val outOfPluginPrettyPrinters = createTempDir().resolve("konan_lldb.py")
    outOfPluginPrettyPrinters.outputStream().use { outputStream ->
        KonanValueRendererFactory::class.java.getResourceAsStream("/lldb/konan_lldb.py").copyTo(outputStream)
    }

    return outOfPluginPrettyPrinters.toPath()
}
