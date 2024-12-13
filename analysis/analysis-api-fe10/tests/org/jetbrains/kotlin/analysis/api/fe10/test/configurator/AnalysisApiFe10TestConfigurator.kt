/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fe10.test.configurator

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiBaseTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiDecompiledCodeTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiIdeModeTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtSourceTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.TestModuleStructureFactory
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiBinaryLibraryIndexingMode
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiIndexingConfiguration
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.*
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

object AnalysisApiFe10TestConfigurator : AnalysisApiTestConfigurator() {
    override val analyseInDependentSession: Boolean get() = false

    override val frontendKind: FrontendKind get() = FrontendKind.Fe10

    override val testPrefix: String
        get() = "descriptors"

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        builder.apply {
            useAdditionalService<KtTestModuleFactory> { KtSourceTestModuleFactory }
            useAdditionalService { AnalysisApiIndexingConfiguration(AnalysisApiBinaryLibraryIndexingMode.NO_INDEXING) }
            configurePlatformEnvironmentConfigurators()
            configureLibraryCompilationSupport()
        }
    }

    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> = listOf(
        AnalysisApiBaseTestServiceRegistrar,
        AnalysisApiIdeModeTestServiceRegistrar,
        AnalysisApiDecompiledCodeTestServiceRegistrar,
        AnalysisApiFe10TestServiceRegistrar,
    )

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): KtTestModuleStructure {
        return TestModuleStructureFactory.createProjectStructureByTestStructure(moduleStructure, testServices, project)
    }

    override fun prepareFilesInModule(ktTestModule: KtTestModule, testServices: TestServices) {
        val testModule = ktTestModule.testModule
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val compilerConfiguration = compilerConfigurationProvider.getCompilerConfiguration(testModule)
        val project = compilerConfigurationProvider.getProject(testModule)
        val packageProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(testModule)
        JvmResolveUtil.analyze(project, ktTestModule.ktFiles, compilerConfiguration, packageProviderFactory)
    }

    override fun computeTestDataPath(path: Path): Path {
        val newPath = path.resolveSibling(path.nameWithoutExtension + "." + testPrefix + "." + path.extension)
        if (newPath.toFile().exists()) return newPath
        return path
    }
}
