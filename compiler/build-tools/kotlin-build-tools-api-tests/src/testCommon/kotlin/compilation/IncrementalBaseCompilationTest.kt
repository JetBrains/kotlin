/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters
import org.jetbrains.kotlin.buildtools.api.tests.buildToolsVersion
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import java.io.File
import kotlin.io.path.*

abstract class IncrementalBaseCompilationTest : BaseCompilationTest() {
    fun Module.compileIncrementallyImpl(
        runner: BuildRunner,
        sourcesChanges: SourcesChanges,
        dependencies: Set<Module> = emptySet(),
        forceNonIncrementalCompilation: Boolean = false,
        assertions: (CompilationResult, Map<LogLevel, Collection<String>>, Set<String>) -> Unit = { _, _, _ -> },
    ) {
        val snapshots = dependencies.map {
            runner.generateClasspathSnapshot(it).toFile()
        }
        val shrunkClasspathSnapshotFile = icDir.resolve("shrunk-classpath-snapshot.bin")
        val params = ClasspathSnapshotBasedIncrementalCompilationApproachParameters(
            snapshots,
            shrunkClasspathSnapshotFile.toFile()
        )
        val options = runner.compilationConfig.makeClasspathSnapshotBasedIncrementalCompilationConfiguration()
        options.setBuildDir(buildDirectory.toFile())
        options.setRootProjectDir(workingDirectory.toFile())
        if (buildToolsVersion < KotlinToolingVersion(2, 0, 0, "Beta2")) {
            // work around incorrect default value
            options.useOutputDirs(setOf(icCachesDir.toFile(), outputDirectory.toFile()))
        }
        if (forceNonIncrementalCompilation) {
            options.forceNonIncrementalMode()
        }
        runner.compilationConfig.useIncrementalCompilation(
            icCachesDir.toFile(),
            sourcesChanges,
            params,
            options,
        )
        compileImpl(runner, dependencies) { result, logs ->
            assertions(result, logs, logs.extractCompiledSources())
        }
        assertTrue(shrunkClasspathSnapshotFile.exists())
    }

    private fun Map<LogLevel, Collection<String>>.extractCompiledSources() =
        getValue(LogLevel.DEBUG).filter { it.startsWith("compile iteration") }
            .flatMap { it.replace("compile iteration: ", "").trim().split(", ") }
            .toSet()
}