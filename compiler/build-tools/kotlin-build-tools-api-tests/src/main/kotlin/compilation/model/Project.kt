/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

class Project(
    val projectSpec: ProjectSpec,
    val projectDirectory: Path,
) {
    val projectId = ProjectId.RandomProjectUUID()
    private val invalidModuleNameCharactersRegex = """[\\/\r\n\t]""".toRegex()

    fun module(
        moduleName: String,
        dependencies: List<Module> = emptyList(),
        snapshotConfig: SnapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
        overrides: Module.Overrides = Module.Overrides(),
    ): Module {
        val moduleDirectory = projectDirectory.resolve(moduleName)
        val sanitizedModuleName = moduleName.replace(invalidModuleNameCharactersRegex, "_")
        val module = projectSpec.moduleFactory(this, moduleDirectory, sanitizedModuleName, dependencies, snapshotConfig, overrides)

        module.sourcesDirectory.createDirectories()
        val templatePath = Paths.get("src/main/resources/modules/$moduleName")
        assert(templatePath.isDirectory()) {
            "Template for $moduleName not found. Expected template directory path is $templatePath"
        }
        templatePath.copyToRecursively(module.sourcesDirectory, followLinks = false)
        return module
    }

}

inline fun BaseCompilationTest.project(projectSpec: ProjectSpec, action: Project.() -> Unit) {
    Project(projectSpec, workingDirectory).apply {
        action()
        projectSpec.endCompilationRound(this.projectId)
    }
}

class ProjectSpec(
    val compilerVersion: KotlinToolingVersion,
    val moduleFactory: (project: Project, moduleDirectory: Path, sanitizedModuleName: String, dependencies: List<Module>, snapshotConfig: SnapshotConfig, overrides: Module.Overrides) -> AbstractModule<*>,
    val endCompilationRound: (ProjectId.ProjectUUID) -> Unit,
)