/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.native.test.debugger

import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import java.nio.file.Path
import java.nio.file.Paths

object DistProperties {
    private val dist: Path = Paths.get(requireProp("kotlin.native.home"))
    private val konancDriver = if (HostManager.host.family == Family.MINGW) "konanc.bat" else "konanc"
    val konanc: Path = dist.resolve("bin/$konancDriver")
    val lldb: Path = Paths.get("lldb")
    val devToolsSecurity: Path = Paths.get("DevToolsSecurity")
    val lldbPrettyPrinters: Path = dist.resolve("tools/konan_lldb.py")

    private fun requireProp(name: String): String
            = System.getProperty(name) ?: error("Property `$name` is not defined")
}