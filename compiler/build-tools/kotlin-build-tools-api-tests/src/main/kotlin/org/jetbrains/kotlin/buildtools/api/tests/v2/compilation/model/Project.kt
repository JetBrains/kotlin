/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.v2.compilation.model

import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.tests.BaseTest
import org.jetbrains.kotlin.buildtools.api.tests.v2.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

class Project(
    val defaultExecutionPolicy: ExecutionPolicy,
    val projectDirectory: Path,
) {
    val projectId = ProjectId.RandomProjectUUID()
    private val invalidModuleNameCharactersRegex = """[\\/\r\n\t]""".toRegex()

    fun module(
        moduleName: String,
        dependencies: List<Module> = emptyList(),
        snapshotConfig: SnapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
    ): Module {
        val moduleDirectory = projectDirectory.resolve(moduleName)
        val sanitizedModuleName = moduleName.replace(invalidModuleNameCharactersRegex, "_")
        val module = JvmModule(
            project = this,
            moduleName = sanitizedModuleName,
            moduleDirectory = moduleDirectory,
            dependencies = dependencies,
            defaultExecutionPolicy = defaultExecutionPolicy,
            snapshotConfig = snapshotConfig,
        )
        module.sourcesDirectory.createDirectories()
        val templatePath = Paths.get("src/main/resources/modules/$moduleName")
        assert(templatePath.isDirectory()) {
            "Template for $moduleName not found. Expected template directory path is $templatePath"
        }
        templatePath.copyToRecursively(module.sourcesDirectory, followLinks = false)
        return module
    }

    fun endCompilationRound() {
        BaseTest.compilationService.finishProjectCompilation(projectId)
    }
}

inline fun BaseCompilationTest.project(executionPolicy: ExecutionPolicy, action: Project.() -> Unit) {
    Project(executionPolicy, workingDirectory).apply {
        action()
        endCompilationRound()
    }
}
