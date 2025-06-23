/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.v2.compilation.model.JvmModuleV2
import org.jetbrains.kotlin.buildtools.api.v2.KotlinToolchain
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.streams.asStream

class DefaultStrategyAgnosticCompilationTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        val compilationService = CompilationService.loadImplementation(BaseCompilationTest::class.java.classLoader)
        val kotlinToolchain = KotlinToolchain.loadImplementation(BaseCompilationTest::class.java.classLoader)
        return sequenceOf(
            named(
                "[v1] in-process", ProjectSpec(
                    KotlinToolingVersion(compilationService.getCompilerVersion()),
                    { project: Project, moduleDirectory: Path, sanitizedModuleName: String, dependencies: List<Module>, snapshotConfig: SnapshotConfig, overrides: Module.Overrides ->
                        JvmModule(
                            compilationService = compilationService,
                            project = project,
                            moduleName = sanitizedModuleName,
                            moduleDirectory = moduleDirectory,
                            dependencies = dependencies,
                            defaultStrategyConfig = compilationService.makeCompilerExecutionStrategyConfiguration().useInProcessStrategy(),
                            snapshotConfig = snapshotConfig,
                            overrides = overrides,
                        )
                    },
                    { projectUUID -> compilationService.finishProjectCompilation(projectUUID) })
            ), named(
                "[v1] within daemon", ProjectSpec(
                    KotlinToolingVersion(compilationService.getCompilerVersion()),
                    { project: Project, moduleDirectory: Path, sanitizedModuleName: String, dependencies: List<Module>, snapshotConfig: SnapshotConfig, overrides: Module.Overrides ->
                        JvmModule(
                            compilationService = compilationService,
                            project = project,
                            moduleName = sanitizedModuleName,
                            moduleDirectory = moduleDirectory,
                            dependencies = dependencies,
                            defaultStrategyConfig = compilationService.makeCompilerExecutionStrategyConfiguration()
                                .useDaemonStrategy(emptyList()),
                            snapshotConfig = snapshotConfig,
                            overrides = overrides,
                        )
                    },
                    { projectUUID -> compilationService.finishProjectCompilation(projectUUID) })
            ), named(
                "[v2] in-process", ProjectSpec(
                    KotlinToolingVersion(kotlinToolchain.getCompilerVersion()),
                    { project: Project, moduleDirectory: Path, sanitizedModuleName: String, dependencies: List<Module>, snapshotConfig: SnapshotConfig, overrides: Module.Overrides ->
                        JvmModuleV2(
                            toolchain = kotlinToolchain,
                            project = project,
                            moduleName = sanitizedModuleName,
                            moduleDirectory = moduleDirectory,
                            dependencies = dependencies,
                            defaultExecutionPolicy = kotlinToolchain.createInProcessExecutionPolicy(),
                            snapshotConfig = snapshotConfig,
                            overrides = overrides,
                        )
                    },
                    { projectUUID -> compilationService.finishProjectCompilation(projectUUID) })
            ), named(
                "[v2] within daemon",
                ProjectSpec(
                    KotlinToolingVersion(kotlinToolchain.getCompilerVersion()),
                    { project: Project, moduleDirectory: Path, sanitizedModuleName: String, dependencies: List<Module>, snapshotConfig: SnapshotConfig, overrides: Module.Overrides ->
                        JvmModuleV2(
                            toolchain = kotlinToolchain,
                            project = project,
                            moduleName = sanitizedModuleName,
                            moduleDirectory = moduleDirectory,
                            dependencies = dependencies,
                            defaultExecutionPolicy = kotlinToolchain.createDaemonExecutionPolicy(),
                            snapshotConfig = snapshotConfig,
                            overrides = overrides,
                        )
                    },
                    { projectUUID -> compilationService.finishProjectCompilation(projectUUID) }),
            )
        ).map { Arguments.of(it) }.asStream()
    }
}