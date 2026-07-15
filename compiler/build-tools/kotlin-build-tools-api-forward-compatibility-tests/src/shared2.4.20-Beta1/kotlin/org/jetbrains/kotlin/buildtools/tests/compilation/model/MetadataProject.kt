/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.forward.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.util.currentKotlinMetadataStdlibKlibLocation
import java.nio.file.Path

class MetadataProject(
    kotlinToolchain: KotlinToolchains,
    defaultStrategyConfig: ExecutionPolicy,
    projectDirectory: Path,
) : AbstractProject<KotlinMetadataKlibCompilationOperation, KotlinMetadataKlibCompilationOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>(
    kotlinToolchain,
    defaultStrategyConfig,
    projectDirectory,
) {
    override fun module(
        moduleName: String,
        dependencies: List<Dependency>,
        snapshotConfig: SnapshotConfig,
        stdlibClasspath: List<Path>?,
        moduleCompilationConfigAction: (KotlinMetadataKlibCompilationOperation.Builder) -> Unit,
    ): MetadataModule {
        val moduleDirectory = projectDirectory.resolve(moduleName)
        val sanitizedModuleName = moduleName.replace(invalidModuleNameCharactersRegex, "_")
        val module = MetadataModule(
            kotlinToolchain = kotlinToolchain,
            buildSession = kotlinBuild,
            project = this,
            moduleName = sanitizedModuleName,
            moduleDirectory = moduleDirectory,
            dependencies = dependencies,
            defaultStrategyConfig = defaultStrategyConfig,
            moduleCompilationConfigAction = moduleCompilationConfigAction,
            stdlibLocation = stdlibClasspath ?: listOf(
                currentKotlinMetadataStdlibKlibLocation // compile against the provided stdlib
            )
        )
        initModule(module, moduleName)
        return module
    }
}

fun BaseCompilationTest.metadataProject(
    kotlinToolchain: KotlinToolchains,
    strategyConfig: ExecutionPolicy,
    action: MetadataProject.() -> Unit,
) {
    MetadataProject(kotlinToolchain, strategyConfig, workingDirectory).use { project ->
        project.action()
    }
}

fun BaseCompilationTest.metadataProject(executionStrategy: CompilerExecutionStrategyConfiguration, action: MetadataProject.() -> Unit) {
    metadataProject(executionStrategy.first, executionStrategy.second, action)
}
