/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.currentKotlinJsStdlibKlibLocation
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.nio.file.Path

class JsProject(
    kotlinToolchain: KotlinToolchains,
    defaultStrategyConfig: ExecutionPolicy,
    projectDirectory: Path,
) : AbstractProject<JsKlibCompilationOperation, JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>(
    kotlinToolchain,
    defaultStrategyConfig,
    projectDirectory,
) {
    private val registeredModules = mutableSetOf<JsModule>()

    override fun module(
        moduleName: String,
        dependencies: List<Dependency>,
        snapshotConfig: SnapshotConfig,
        stdlibClasspath: List<Path>?,
        moduleCompilationConfigAction: (JsKlibCompilationOperation.Builder) -> Unit,
    ): JsModule {
        val moduleDirectory = projectDirectory.resolve(moduleName)
        val sanitizedModuleName = moduleName.replace(invalidModuleNameCharactersRegex, "_")
        val module = JsModule(
            kotlinToolchain = kotlinToolchain,
            buildSession = kotlinBuild,
            project = this,
            moduleName = sanitizedModuleName,
            moduleDirectory = moduleDirectory,
            dependencies = dependencies,
            defaultStrategyConfig = defaultStrategyConfig,
            moduleCompilationConfigAction = moduleCompilationConfigAction,
            stdlibKlibLocation = stdlibClasspath ?: listOf(
                currentKotlinJsStdlibKlibLocation
            ),
            registeredModules = registeredModules,
        )
        registeredModules.add(module)
        initModule(module, moduleName)
        return module
    }
}

fun BaseCompilationTest.jsProject(kotlinToolchain: KotlinToolchains, strategyConfig: ExecutionPolicy, action: JsProject.() -> Unit) {
    kotlinToolchain.assumeJsIsSupported()
    JsProject(kotlinToolchain, strategyConfig, workingDirectory).use { project ->
        project.action()
    }
}

fun BaseCompilationTest.jsProject(executionStrategy: CompilerExecutionStrategyConfiguration, action: JsProject.() -> Unit) {
    jsProject(executionStrategy.first, executionStrategy.second, action)
}

fun KotlinToolchains.assumeJsIsSupported() {
    try {
        js
    } catch (e: Throwable) {
        if (e.message?.startsWith("Unsupported") == true) {
            assumeTrue(false) { "Kotlin/JS is not supported on this version" }
        } else throw e
    }
}
