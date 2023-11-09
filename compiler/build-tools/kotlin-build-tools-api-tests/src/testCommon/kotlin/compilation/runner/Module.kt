/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.runner

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories

class Module(
    val moduleName: String,
    val moduleDirectory: Path,
    val additionalCompilationArguments: List<String> = emptyList(),
) {
    val sourcesDirectory: Path
        get() = moduleDirectory.resolve("src")

    val buildDirectory: Path
        get() = moduleDirectory.resolve("build")

    val outputDirectory: Path
        get() = buildDirectory.resolve("output")

    val icDir: Path
        get() = buildDirectory.resolve("ic")

    val snapshotFile: Path
        get() = icDir.resolve("$moduleName.snapshot")

    val icCachesDir: Path
        get() = icDir.resolve("caches")

    override fun toString() = moduleName
}

fun prepareModule(
    moduleName: String,
    projectDirectory: Path,
    additionalCompilationArguments: List<String> = emptyList(),
): Module {
    val moduleDirectory = projectDirectory.resolve(moduleName)
    val module = Module(moduleName, moduleDirectory, additionalCompilationArguments)
    module.sourcesDirectory.createDirectories()
    Paths.get("src/testCommon/resources/modules/$moduleName").copyToRecursively(module.sourcesDirectory, followLinks = false)
    return module
}