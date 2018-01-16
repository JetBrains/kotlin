package org.jetbrains.kotlin.compiletest

import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import java.nio.file.Path
import java.nio.file.Paths


object DistProperties {
    private val dist: Path = Paths.get(requireProp("konan.home"))
    private val konancDriver = if (HostManager.host.family == Family.WINDOWS) "konanc.bat" else "konanc"
    val konanc: Path = dist.resolve("bin/$konancDriver")
    val lldb: Path = Paths.get("lldb")
    val lldbPrettyPrinters: Path = dist.resolve("tools/konan_lldb.py")

    private fun requireProp(name: String): String
            = System.getProperty(name) ?: error("Property `$name` is not defined")
}