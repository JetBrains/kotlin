/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.currentKotlinStdlibLocation
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

class Project(
    val kotlinToolchain: KotlinToolchains,
    val defaultStrategyConfig: ExecutionPolicy,
    val projectDirectory: Path,
) : AutoCloseable {
    private val invalidModuleNameCharactersRegex = """[\\/\r\n\t]""".toRegex()
    private val kotlinBuild = kotlinToolchain.createBuildSession()

    fun module(
        moduleName: String,
        dependencies: List<Dependency> = emptyList(),
        snapshotConfig: SnapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
        stdlibClasspath: List<Path>? = null,
        moduleCompilationConfigAction: (JvmCompilationOperation.Builder) -> Unit = {},
    ): Module {
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
        module.sourcesDirectory.createDirectories()
        val templatePath = Paths.get(System.getProperty("kotlin.test.templates.classpath") + "/modules/$moduleName")
        assert(templatePath.isDirectory()) {
            "Template for $moduleName not found. Expected template directory path is $templatePath"
        }
        templatePath.copyToRecursively(module.sourcesDirectory, followLinks = false)
        return module
    }

    override fun close() {
        kotlinBuild.close()
    }
}

fun BaseCompilationTest.project(kotlinToolchain: KotlinToolchains, strategyConfig: ExecutionPolicy, action: Project.() -> Unit) {
    Project(kotlinToolchain, strategyConfig, workingDirectory).use { project ->
        project.action()
    }
}

fun BaseCompilationTest.project(executionStrategy: CompilerExecutionStrategyConfiguration, action: Project.() -> Unit) {
    project(executionStrategy.first, executionStrategy.second, action)
}