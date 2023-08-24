/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.test.configurators

abstract class AnalysisApiTestConfiguratorFactory {
    abstract fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfigurator

    abstract fun supportMode(data: AnalysisApiTestConfiguratorFactoryData): Boolean

    protected fun requireSupported(data: AnalysisApiTestConfiguratorFactoryData) {
        if (!supportMode(data)) {
            unsupportedModeError(data)
        }
    }

    protected fun unsupportedModeError(data: AnalysisApiTestConfiguratorFactoryData): Nothing {
        error("${this::class} is does not support $data")
    }
}

data class AnalysisApiTestConfiguratorFactoryData(
    val frontend: FrontendKind,
    val moduleKind: TestModuleKind,
    val analysisSessionMode: AnalysisSessionMode,
    val analysisApiMode: AnalysisApiMode,
)

fun AnalysisApiTestConfiguratorFactoryData.defaultExtension(): String = when (this.moduleKind) {
    TestModuleKind.ScriptSource -> "kts"
    else -> "kt"
}

enum class AnalysisSessionMode(val suffix: String) {
    Normal("Normal"),

    Dependent("Dependent");
}

enum class AnalysisApiMode(val suffix: String) {
    Ide("Ide"),
    Standalone("Standalone");
}

enum class FrontendKind(val suffix: String) {
    Fir("Fir"),
    Fe10("Fe10"),
}

enum class TestModuleKind(val suffix: String) {
    Source("Source"),
    LibraryBinary("LibraryBinary"),
    LibrarySource("LibrarySource"),
    ScriptSource("ScriptSource"),
}
