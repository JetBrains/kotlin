/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.runner

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.tests.CompilationServiceInitializationTest
import java.io.Closeable
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createParentDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.toPath

class BuildRunner(val project: Project) : Closeable {
    private val compilationService = CompilationService.loadImplementation(CompilationServiceInitializationTest::class.java.classLoader)
    private val strategyConfig = compilationService.makeCompilerExecutionStrategyConfiguration()
    val testLogger = TestKotlinLogger(compilationService.makeJvmCompilationConfiguration().logger)
    val compilationConfig = compilationService.makeJvmCompilationConfiguration()
        .useLogger(testLogger)

    var selectedExecutionStrategy: ExecutionStrategy = ExecutionStrategy.IN_PROCESS
        private set

    fun useInProcessStrategy(): BuildRunner {
        selectedExecutionStrategy = ExecutionStrategy.IN_PROCESS
        strategyConfig.useInProcessStrategy()
        return this
    }

    fun useDaemonStrategy(jvmArguments: List<String> = emptyList()): BuildRunner {
        selectedExecutionStrategy = ExecutionStrategy.DAEMON
        strategyConfig.useDaemonStrategy(jvmArguments)
        return this
    }

    fun generateClasspathSnapshot(module: Module): Path {
        val snapshot =
            compilationService.calculateClasspathSnapshot(module.outputDirectory.toFile(), ClassSnapshotGranularity.CLASS_MEMBER_LEVEL)
        module.snapshotFile.createParentDirectories()
        snapshot.saveSnapshot(module.snapshotFile.toFile())
        return module.snapshotFile
    }

    fun compileModule(module: Module, dependencies: Set<Module> = emptySet()): CompilationResult {
        val stdlibLocation = KotlinVersion::class.java.protectionDomain.codeSource.location.toURI().toPath() // compile against the provided stdlib
        val dependencyFiles = dependencies.map { it.outputDirectory }.plusElement(stdlibLocation)
        val defaultCompilationArguments = listOf(
            "-no-reflect",
            "-no-stdlib",
            "-d", module.outputDirectory.absolutePathString(),
            "-cp", dependencyFiles.joinToString(File.pathSeparator),
            "-module-name", module.moduleName,
        )
        testLogger.clear()
        println("Compiling $module")
        return compilationService.compileJvm(
            project.projectId,
            strategyConfig,
            compilationConfig,
            module.sourcesDirectory.listDirectoryEntries().map { it.toFile() },
            defaultCompilationArguments + module.additionalCompilationArguments,
        ).also {
            println("Compilation finished with result: $it")
        }
    }

    override fun close() {
        compilationService.finishProjectCompilation(project.projectId)
    }
}