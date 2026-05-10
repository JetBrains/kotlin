/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.test.configurators

import org.jetbrains.kotlin.analysis.test.framework.services.TargetPlatformEnum
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil

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
    val moduleKind: TestModuleKind = TestModuleKind.SourceLike,
    val analysisSessionMode: AnalysisSessionMode = AnalysisSessionMode.Normal,
    val analysisApiMode: AnalysisApiMode = AnalysisApiMode.Ide,
    val targetPlatform: TargetPlatformEnum = TargetPlatformEnum.JVM,
)

fun AnalysisApiTestConfiguratorFactoryData.defaultPattern(): String = when (moduleKind) {
    TestModuleKind.SourceLike if targetPlatform == TargetPlatformEnum.JVM -> TestGeneratorUtil.KT_OR_KTS
    else -> TestGeneratorUtil.KT
}

enum class AnalysisSessionMode(val suffix: String) {
    Normal("Normal"),
    Dependent("Dependent"),
    ;
}

enum class AnalysisApiMode(val suffix: String) {
    Ide("Ide"),
    Standalone("Standalone"),
    ;
}
