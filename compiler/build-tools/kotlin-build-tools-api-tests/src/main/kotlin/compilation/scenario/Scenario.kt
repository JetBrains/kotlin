/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.jvm.IncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.Module

interface Scenario {
    /**
     * Creates a module for a scenario.
     *
     * Modules with the same combination of [Module.scenarioDslCacheKey], [compilationOptionsModifier], and [incrementalCompilationOptionsModifier] are compiled initially only once per tests run.
     *
     * In the case you are using custom values for [compilationOptionsModifier] or [incrementalCompilationOptionsModifier], consider sharing the same lambda between tests for better cacheability results.
     *
     * @param moduleName The name of the module.
     * @param dependencies (optional) The list of scenario modules that this module depends on. Defaults to an empty list.
     * @param additionalCompilationArguments (optional) The list of additional compilation arguments for this module. Defaults to an empty list.
     * @param compilationOptionsModifier (optional) A function that can be used to modify the compilation configuration for this module.
     * @param incrementalCompilationOptionsModifier (optional) A function that can be used to modify the incremental compilation configuration for this module.
     * @return The created scenario module in the compiled state.
     */
    fun module(
        moduleName: String,
        dependencies: List<ScenarioModule> = emptyList(),
        additionalCompilationArguments: List<String> = emptyList(),
        compilationOptionsModifier: ((JvmCompilationConfiguration) -> Unit)? = null,
        incrementalCompilationOptionsModifier: ((IncrementalJvmCompilationConfiguration<*>) -> Unit)? = null,
    ): ScenarioModule
}