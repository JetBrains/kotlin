/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.currentKotlinStdlibLocation
import java.nio.file.Path

class JvmProject(
    kotlinToolchain: KotlinToolchains,
    defaultStrategyConfig: ExecutionPolicy,
    projectDirectory: Path,
) : AbstractProject<JvmCompilationOperation, JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>(
    kotlinToolchain,
    defaultStrategyConfig,
    projectDirectory,
) {
    override fun module(
        moduleName: String,
        dependencies: List<Dependency>,
        snapshotConfig: SnapshotConfig,
        stdlibClasspath: List<Path>?,
        moduleCompilationConfigAction: (JvmCompilationOperation.Builder) -> Unit,
    ): JvmModule {
        val moduleDirectory = projectDirectory.resolve(moduleName)
        val sanitizedModuleName = moduleName.replace(invalidModuleNameCharactersRegex, "_")
        val module = JvmModule(
            kotlinToolchain = kotlinToolchain,
            buildSession = kotlinBuild,
            project = this,
            moduleName = sanitizedModuleName,
            moduleDirectory = moduleDirectory,
            dependencies = dependencies,
            defaultStrategyConfig = defaultStrategyConfig,
            snapshotConfig = snapshotConfig,
            moduleCompilationConfigAction = moduleCompilationConfigAction,
            stdlibLocation = stdlibClasspath ?: listOf(
                currentKotlinStdlibLocation // compile against the provided stdlib
            )
        )
        initModule(module, moduleName)
        return module
    }
}

fun BaseCompilationTest.jvmProject(kotlinToolchain: KotlinToolchains, strategyConfig: ExecutionPolicy, action: JvmProject.() -> Unit) {
    JvmProject(kotlinToolchain, strategyConfig, workingDirectory).use { project ->
        project.action()
    }
}

fun BaseCompilationTest.jvmProject(executionStrategy: CompilerExecutionStrategyConfiguration, action: JvmProject.() -> Unit) {
    jvmProject(executionStrategy.first, executionStrategy.second, action)
}
