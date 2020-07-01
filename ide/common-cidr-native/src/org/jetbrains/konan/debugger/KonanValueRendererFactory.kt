package org.jetbrains.konan.debugger

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.xdebugger.XDebuggerManager
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriver
import com.jetbrains.cidr.execution.debugger.evaluation.ValueRendererFactory
import com.jetbrains.cidr.execution.debugger.evaluation.renderers.ValueRenderer
import org.jetbrains.konan.util.getKotlinNativeHome
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
    return Paths.get(kotlinNativeHome, "tools", "konan_lldb.py")
}

// N.B. If you need to use custom LLDB bindings uncomment the code below and adjust it as necessary:

//private fun getPrettyPrintersLocation(kotlinNativeHome: String): Path {
//    // use custom (patched) pretty printers for certain versions of Kotlin/Native
//    val prettyPrintersFromPlugin = getKotlinNativeVersion(kotlinNativeHome)?.run {
//        if (major == 1 && minor == 3 && maintenance >= 70 && maintenance < 80)
//            PP_1_3_7X
//        else
//            null
//    } ?: return Paths.get(kotlinNativeHome, "tools", "konan_lldb.py")
//
//    val outOfPluginPrettyPrinters = createTempDir().resolve("konan_lldb.py")
//    outOfPluginPrettyPrinters.outputStream().use { outputStream ->
//        KonanValueRendererFactory::class.java.getResourceAsStream("/lldb/${prettyPrintersFromPlugin.filename}").copyTo(outputStream)
//    }
//
//    return outOfPluginPrettyPrinters.toPath()
//}
//
//private enum class PrettyPrintersFromPlugin(val filename: String) {
//    PP_1_3_7X("konan_lldb.py-1.3.7x")
//}
