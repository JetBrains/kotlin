package org.jetbrains.kotlin.compiletest

import java.nio.file.Path
import java.nio.file.Paths


object DistProperties {
    private val isWindows: Boolean = requireProp("os.name").startsWith("Windows")

    private val dist: Path = Paths.get(requireProp("konan.home"))
    private val konancDriver = if (isWindows) "konanc.bat" else "konanc"
    val konanc: Path = dist.resolve("bin/$konancDriver")
    val lldb: Path = Paths.get("lldb")

    private fun requireProp(name: String): String
            = System.getProperty(name) ?: error("Property `$name` is not defined")
}