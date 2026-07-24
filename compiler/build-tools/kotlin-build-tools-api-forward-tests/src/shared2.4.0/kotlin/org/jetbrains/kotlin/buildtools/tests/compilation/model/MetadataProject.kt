/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.forward.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.junit.jupiter.api.Assumptions
import java.nio.file.Path

class MetadataProject(
    kotlinToolchain: KotlinToolchains,
    defaultStrategyConfig: ExecutionPolicy,
    projectDirectory: Path,
) : AbstractProject<BaseCompilationOperation, BaseCompilationOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>(
    kotlinToolchain,
    defaultStrategyConfig,
    projectDirectory,
) {
    override fun module(
        moduleName: String,
        dependencies: List<Dependency>,
        snapshotConfig: SnapshotConfig,
        stdlibClasspath: List<Path>?,
        moduleCompilationConfigAction: (BaseCompilationOperation.Builder) -> Unit,
    ): MetadataModule {
        error("Not implemented")
    }
}

fun BaseCompilationTest.metadataProject(
    kotlinToolchain: KotlinToolchains,
    strategyConfig: ExecutionPolicy,
    action: MetadataProject.() -> Unit,
) {
    Assumptions.abort<Unit> { "Not supported in this API version" }
}

fun BaseCompilationTest.metadataProject(executionStrategy: CompilerExecutionStrategyConfiguration, action: MetadataProject.() -> Unit) {
    metadataProject(executionStrategy.first, executionStrategy.second, action)
}
