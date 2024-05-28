/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.tests.BaseTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

class Project(
    val defaultStrategyConfig: CompilerExecutionStrategyConfiguration,
    val projectDirectory: Path,
) {
    val projectId = ProjectId.ProjectUUID(UUID.randomUUID())
    private val invalidModuleNameCharactersRegex = """[\\/\r\n\t]""".toRegex()

    fun module(
        moduleName: String,
        dependencies: List<Module> = emptyList(),
        additionalCompilationArguments: List<String> = emptyList(),
    ): Module {
        val moduleDirectory = projectDirectory.resolve(moduleName)
        val sanitizedModuleName = moduleName.replace(invalidModuleNameCharactersRegex, "_")
        val module = JvmModule(this, sanitizedModuleName, moduleDirectory, dependencies, defaultStrategyConfig, additionalCompilationArguments)
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

fun BaseCompilationTest.project(strategyConfig: CompilerExecutionStrategyConfiguration, action: Project.() -> Unit) {
    Project(strategyConfig, workingDirectory).apply {
        action()
        endCompilationRound()
    }
}