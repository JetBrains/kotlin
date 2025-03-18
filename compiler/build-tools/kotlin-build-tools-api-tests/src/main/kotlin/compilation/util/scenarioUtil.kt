/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.util

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.SnapshotConfig
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.Scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.ScenarioModule


fun Scenario.moduleWithInlineSnapshotting(
    moduleName: String,
    dependencies: List<ScenarioModule>,
) = module(
    moduleName = moduleName,
    dependencies = dependencies,
    snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
)

fun Scenario.moduleWithFir(
    moduleName: String,
    additionalCompilerArguments: List<String> = emptyList()
) = module(
    moduleName = moduleName,
    additionalCompilationArguments = additionalCompilerArguments + listOf("-Xuse-fir-ic"),
    incrementalCompilationOptionsModifier = { incrementalOptions ->
        (incrementalOptions as ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration).useFirRunner(true)
    }
)
