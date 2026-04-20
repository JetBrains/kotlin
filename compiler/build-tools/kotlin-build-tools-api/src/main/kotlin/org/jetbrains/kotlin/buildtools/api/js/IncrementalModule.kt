/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js

import java.nio.file.Path

/**
 * Information about a module that is being compiled incrementally.
 *
 * @property name the name of the module
 * @property output the output directory or file (e.g. klib) of the module
 * @property buildDir the build directory of the module
 * @property buildHistoryDir the directory where the build history file is stored (if it is different than [buildDir], otherwise `null`)
 */
public class IncrementalModule(
    public val name: String,
    public val output: Path,
    public val buildDir: Path,
    public val buildHistoryDir: Path? = null,
) {
    override fun toString(): String {
        return "IncrementalModule(name='$name', output=$output, buildDir=$buildDir, buildHistoryDir=$buildHistoryDir)"
    }
}
