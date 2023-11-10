/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.runner

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

class Module(
    val moduleName: String,
    val moduleDirectory: Path,
    val additionalCompilationArguments: List<String> = emptyList(),
) {
    val sourcesDirectory: Path
        get() = moduleDirectory.resolve("src")

    val buildDirectory: Path
        get() = moduleDirectory.resolve("build")

    /**
     * Directory for the main outputs of the compiler (mainly .class files)
     */
    val outputDirectory: Path
        get() = buildDirectory.resolve("output")

    /**
     * Directory for storing different service files of IC
     */
    val icDir: Path
        get() = buildDirectory.resolve("ic")

    val icCachesDir: Path
        get() = icDir.resolve("caches")

    /**
     * You can use this function to force a clean build of the module
     */
    fun clearState() {
        buildDirectory.deleteRecursively()
        icDir.deleteRecursively()
    }

    override fun toString() = moduleName
}

/**
 * Creates a [Module] and sets up temporary directory to contain sources copied from the resources folder
 *
 * If you're using the compilation DSL defined by [org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest.scenario],
 * prefer using [org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest.ScenarioDsl.module] instead of this method.
 */
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