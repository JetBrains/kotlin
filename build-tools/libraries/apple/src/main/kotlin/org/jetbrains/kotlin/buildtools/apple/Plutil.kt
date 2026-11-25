/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path
import java.nio.file.Paths

/** `/usr/bin/plutil`, which converts property lists, including Xcode's `project.pbxproj`. */
public class Plutil internal constructor(private val tools: AppleTools) {

    /** `plutil -convert json -o <output> <input>`, writing to standard output when [output] is `null`. */
    public fun convertToJsonInvocation(input: Path, output: Path? = null): AppleToolInvocation =
        tools.systemInvocation(PLUTIL, listOf("-convert", "json", "-o", output?.toString() ?: "-", input.toString()))

    /** Converts [input] to JSON and returns it. */
    public fun convertToJson(input: Path): String = tools.runner.runChecked(convertToJsonInvocation(input)).stdout

    /** Converts [input] to JSON and writes it to [output]. */
    public fun convertToJson(input: Path, output: Path) {
        tools.runner.runChecked(convertToJsonInvocation(input, output))
    }

    private companion object {
        val PLUTIL: Path = Paths.get("/usr/bin/plutil")
    }
}
