/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import java.nio.file.Path

abstract class AbstractModule(
    val project: Project,
    override val moduleName: String,
    val moduleDirectory: Path,
    val dependencies: List<Dependency>,
    override val additionalCompilationArguments: List<String> = emptyList(),
) : Module {
    override val sourcesDirectory: Path
        get() = moduleDirectory.resolve("src")

    override val buildDirectory: Path
        get() = moduleDirectory.resolve("build")

    override val outputDirectory: Path
        get() = buildDirectory.resolve("output")

    override val location: Path
        get() = outputDirectory

    override val icWorkingDir: Path
        get() = buildDirectory.resolve("ic")

    override val snapshotFile: Path
        get() = icWorkingDir.resolve("$moduleName.snapshot")

    override val icCachesDir: Path
        get() = icWorkingDir.resolve("caches")

    override fun toString() = moduleName
}