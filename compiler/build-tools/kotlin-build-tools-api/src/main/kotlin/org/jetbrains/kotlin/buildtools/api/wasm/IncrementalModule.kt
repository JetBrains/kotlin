/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.wasm

import java.io.File
import java.io.Serializable
import java.nio.file.Path

/**
 * Information about a module that is being compiled incrementally.
 *
 * @property name the name of the module
 * @property output the output directory or file (e.g. klib) of the module
 * @property buildDir the build directory of the module
 * @property buildHistoryDir the directory where the build history file is stored (if it is different than [buildDir], otherwise `null`)
 *
 * @since 2.4.20
 */
public class IncrementalModule(
    public val name: String,
    output: Path,
    buildDir: Path,
    buildHistoryDir: Path? = null,
) : Serializable {

    public val output: Path
        get() = _output.toPath()

    public val buildDir: Path
        get() = _buildDir.toPath()

    public val buildHistoryDir: Path?
        get() = _buildHistoryDir?.toPath()


    public val _output: File = output.toFile()
    public val _buildDir: File = buildDir.toFile()
    public val _buildHistoryDir: File? = buildHistoryDir?.toFile()

}

