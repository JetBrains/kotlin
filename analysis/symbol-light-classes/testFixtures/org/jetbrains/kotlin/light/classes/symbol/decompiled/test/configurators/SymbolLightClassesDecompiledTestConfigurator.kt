/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirLibraryBinaryDecompiledTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

abstract class SymbolLightClassesDecompiledTestConfigurator(
    override val defaultTargetPlatform: TargetPlatform,
    override val testPrefixes: List<String>,
) : AnalysisApiTestConfigurator() {
    override val analyseInDependentSession: Boolean get() = false
    override val analysisApiMode: AnalysisApiMode get() = AnalysisApiMode.Ide
    override val frontendKind: FrontendKind get() = FrontendKind.Fir

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        AnalysisApiFirLibraryBinaryDecompiledTestConfigurator.configureTest(builder, disposable)
    }

    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = AnalysisApiFirLibraryBinaryDecompiledTestConfigurator.serviceRegistrars +
                AnalysisApiSymbolLightClassesDecompiledTestServiceRegistrar

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): KtTestModuleStructure {
        return AnalysisApiFirLibraryBinaryDecompiledTestConfigurator.createModules(moduleStructure, testServices, project)
    }
}

object SymbolLightClassesDecompiledJvmTestConfigurator : SymbolLightClassesDecompiledTestConfigurator(
    JvmPlatforms.defaultJvmPlatform,
    listOf("lib"),
)

object SymbolLightClassesDecompiledJsTestConfigurator : SymbolLightClassesDecompiledTestConfigurator(
    JsPlatforms.defaultJsPlatform,
    listOf("kmp.lib"),
)
