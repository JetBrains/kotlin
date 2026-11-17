/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.currentKotlinNativeStdlibKlibLocation
import java.nio.file.Path

class NativeProject(
    kotlinToolchain: KotlinToolchains,
    defaultStrategyConfig: ExecutionPolicy,
    projectDirectory: Path,
) : AbstractProject<NativeKlibCompilationOperation, NativeKlibCompilationOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>(
    kotlinToolchain,
    defaultStrategyConfig,
    projectDirectory,
) {
    override fun module(
        moduleName: String,
        dependencies: List<Dependency>,
        snapshotConfig: SnapshotConfig,
        stdlibClasspath: List<Path>?,
        compileJavaSources: Boolean,
        moduleCompilationConfigAction: (NativeKlibCompilationOperation.Builder) -> Unit,
    ): NativeModule {
        require(!compileJavaSources) { "Compiling Java sources is not supported for Kotlin/Native modules" }
        val moduleDirectory = projectDirectory.resolve(moduleName)
        val sanitizedModuleName = moduleName.replace(invalidModuleNameCharactersRegex, "_")
        val module = NativeModule(
            kotlinToolchain = kotlinToolchain,
            buildSession = kotlinBuild,
            project = this,
            moduleName = sanitizedModuleName,
            moduleDirectory = moduleDirectory,
            dependencies = dependencies,
            defaultStrategyConfig = defaultStrategyConfig,
            moduleCompilationConfigAction = moduleCompilationConfigAction,
            stdlibLocation = stdlibClasspath ?: listOf(
                currentKotlinNativeStdlibKlibLocation // compile against the provided stdlib
            )
        )
        initModule(module, moduleName)
        return module
    }
}

fun BaseCompilationTest.nativeProject(
    kotlinToolchain: KotlinToolchains,
    strategyConfig: ExecutionPolicy,
    action: NativeProject.() -> Unit,
) {
    NativeProject(kotlinToolchain, strategyConfig, workingDirectory).use { project ->
        project.action()
    }
}
