/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JsModule
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JsProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.SnapshotConfig
import org.jetbrains.kotlin.buildtools.tests.compilation.model.assumeJsIsSupported

class JsScenarioDsl(
    private val project: JsProject,
    private val strategyConfig: ExecutionPolicy,
    override val kotlinToolchains: KotlinToolchains,
) : Scenario<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder> {
    @Synchronized
    override fun module(
        moduleName: String,
        dependencies: List<ScenarioModule>,
        snapshotConfig: SnapshotConfig,
        compilationConfigAction: (JsKlibCompilationOperation.Builder) -> Unit,
        icOptionsConfigAction: (JsHistoryBasedIncrementalCompilationConfiguration.Builder) -> Unit,
    ): ScenarioModule {
        val transformedDependencies = dependencies.map { (it as BaseScenarioModule<*, *>).module }
        val module =
            project.module(moduleName, transformedDependencies, snapshotConfig, moduleCompilationConfigAction = compilationConfigAction)
        return GlobalCompiledProjectsCache.getProjectFromCache(
            module,
            strategyConfig,
            snapshotConfig,
            icOptionsConfigAction,
            false,
            dependencies,
        )
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(
                module,
                strategyConfig,
                snapshotConfig,
                icOptionsConfigAction,
                false,
                dependencies,
            )
    }

    @Synchronized
    override fun trackedModule(
        moduleName: String,
        dependencies: List<ScenarioModule>,
        snapshotConfig: SnapshotConfig,
        compilationConfigAction: (JsKlibCompilationOperation.Builder) -> Unit,
        icOptionsConfigAction: (JsHistoryBasedIncrementalCompilationConfiguration.Builder) -> Unit,
    ): ScenarioModule {
        val transformedDependencies = dependencies.map { (it as BaseScenarioModule<*, *>).module }
        val module =
            project.module(moduleName, transformedDependencies, snapshotConfig, moduleCompilationConfigAction = compilationConfigAction)
        return GlobalCompiledProjectsCache.getProjectFromCache(
            module,
            strategyConfig,
            snapshotConfig,
            icOptionsConfigAction,
            true,
            dependencies,
        )
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(
                module,
                strategyConfig,
                snapshotConfig,
                icOptionsConfigAction,
                true,
                dependencies,
            )
    }

    override fun modules(vararg moduleSpecs: ModuleSpec<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>): List<ScenarioModule> {
        return modules(tracked = false, *moduleSpecs)
    }

    private fun modules(
        tracked: Boolean,
        vararg moduleSpecs: ModuleSpec<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>,
    ): List<ScenarioModule> {
        val specsToModules: MutableList<Pair<ModuleSpec<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>, JsModule>> =
            moduleSpecs.fold(mutableListOf()) { acc, spec ->
                val transformedDependencies = spec.dependencies.map { dependency -> acc.first { it.first.moduleName == dependency }.second }
                acc.add(
                    spec to project.module(
                        spec.moduleName,
                        transformedDependencies,
                        spec.snapshotConfig,
                        moduleCompilationConfigAction = spec.compilationConfigAction
                    )
                )
                acc
            }
        val modules = specsToModules.map { it.second }
        modules.forEach { module ->
            module.otherModules.addAll(modules.minus(module))
        }
        return specsToModules.fold(mutableListOf<ScenarioModule>()) { acc, pair ->
            val spec = pair.first
            val module = pair.second
            acc.add(
                GlobalCompiledProjectsCache.getProjectFromCache(
                    module,
                    strategyConfig,
                    spec.snapshotConfig,
                    spec.icOptionsConfigAction,
                    tracked,
                    acc.filter { (it as BaseScenarioModule<*, *>).module.moduleName in spec.dependencies },
                )
                    ?: GlobalCompiledProjectsCache.putProjectIntoCache(
                        module,
                        strategyConfig,
                        spec.snapshotConfig,
                        spec.icOptionsConfigAction,
                        tracked,
                        acc.filter { (it as BaseScenarioModule<*, *>).module.moduleName in spec.dependencies },
                    )
            )
            acc
        }
    }

    override fun trackedModules(vararg moduleSpecs: ModuleSpec<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>): List<ScenarioModule> {
        return modules(tracked = true, *moduleSpecs)
    }
}

fun BaseCompilationTest.jsScenario(
    kotlinToolchains: KotlinToolchains,
    strategyConfig: ExecutionPolicy,
    action: Scenario<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    kotlinToolchains.assumeJsIsSupported()
    action(JsScenarioDsl(JsProject(kotlinToolchains, strategyConfig, workingDirectory), strategyConfig, kotlinToolchains))
}

fun BaseCompilationTest.jsScenario(
    executionStrategy: CompilerExecutionStrategyConfiguration,
    action: Scenario<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    jsScenario(executionStrategy.first, executionStrategy.second, action)
}
