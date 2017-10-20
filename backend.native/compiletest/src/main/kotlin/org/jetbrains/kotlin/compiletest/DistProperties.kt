package org.jetbrains.kotlin.compiletest

import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.TargetManager
import java.nio.file.Path
import java.nio.file.Paths


object DistProperties {
    private val dist: Path = Paths.get(requireProp("konan.home"))
    private val konancDriver = if (TargetManager.host.family == Family.WINDOWS) "konanc.bat" else "konanc"
    val konanc: Path = dist.resolve("bin/$konancDriver")
    val lldb: Path = Paths.get("lldb")

    private fun requireProp(name: String): String
            = System.getProperty(name) ?: error("Property `$name` is not defined")
}