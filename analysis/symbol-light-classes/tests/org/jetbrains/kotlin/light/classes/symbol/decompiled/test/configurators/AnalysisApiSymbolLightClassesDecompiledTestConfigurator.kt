/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirLibraryBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneApplicationEnvironmentConfiguration
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestProjectConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiSymbolLightClassesDecompiledTestConfigurator : AnalysisApiTestConfigurator() {
    override val analyseInDependentSession: Boolean get() = false
    override val frontendKind: FrontendKind get() = FrontendKind.Fir

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        AnalysisApiFirLibraryBinaryTestConfigurator.configureTest(builder, disposable)
    }

    override val applicationEnvironmentConfiguration: StandaloneApplicationEnvironmentConfiguration
        get() = AnalysisApiFirLibraryBinaryTestConfigurator.applicationEnvironmentConfiguration

    override val projectConfigurators: List<AnalysisApiTestProjectConfigurator>
        get() = AnalysisApiFirLibraryBinaryTestConfigurator.projectConfigurators +
                AnalysisApiSymbolLightClassesDecompiledTestProjectConfigurator

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): KtModuleProjectStructure {
        return AnalysisApiFirLibraryBinaryTestConfigurator.createModules(moduleStructure, testServices, project)
    }
}
