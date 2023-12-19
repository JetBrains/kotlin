/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiBaseTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiLibraryBaseTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AnalysisApiFirTestServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.configureOptionalTestCompilerPlugin
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtLibraryBinaryModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.TestModuleStructureFactory
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiJvmEnvironmentConfigurator
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.DispatchingTestModuleCompiler
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleCompiler
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleDecompiler
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleDecompilerJar
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.preprocessors.ExternalAnnotationsSourcePreprocessor
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.ExternalAnnotationsEnvironmentConfigurator

object StandaloneModeLibraryBinaryTestConfigurator : StandaloneModeConfiguratorBase() {
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        with(builder) {
            configureOptionalTestCompilerPlugin()
            useConfigurators(::AnalysisApiJvmEnvironmentConfigurator)
            useConfigurators(::ExternalAnnotationsEnvironmentConfigurator)
            useSourcePreprocessor(::ExternalAnnotationsSourcePreprocessor)

            useAdditionalService<KtModuleFactory> { KtLibraryBinaryModuleFactory }
            useAdditionalService<TestModuleCompiler> { DispatchingTestModuleCompiler() }
            useAdditionalService<TestModuleDecompiler> { TestModuleDecompilerJar() }

            this.defaultsProviderBuilder.dependencyKind = DependencyKind.Binary
        }
    }

    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar>
        get() = listOf(
            AnalysisApiBaseTestServiceRegistrar,
            AnalysisApiFirTestServiceRegistrar,
            AnalysisApiLibraryBaseTestServiceRegistrar,
            StandaloneModeTestServiceRegistrar,
        )

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): KtModuleProjectStructure {
        return TestModuleStructureFactory.createProjectStructureByTestStructure(moduleStructure, testServices, project)
    }
}
