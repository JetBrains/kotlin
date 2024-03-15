/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fe10.test.configurator

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiBaseTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiDecompiledCodeTestServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtSourceTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModuleProjectStructure
import org.jetbrains.kotlin.analysis.test.framework.project.structure.TestModuleStructureFactory
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiBinaryLibraryIndexingMode
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiJvmEnvironmentConfigurator
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiIndexingConfiguration
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
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
            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::AnalysisApiJvmEnvironmentConfigurator,
                ::JsEnvironmentConfigurator
            )
        }
    }

    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar> = listOf(
        AnalysisApiBaseTestServiceRegistrar,
        AnalysisApiDecompiledCodeTestServiceRegistrar,
        AnalysisApiFe10TestServiceRegistrar,
    )

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): KtTestModuleProjectStructure {
        return TestModuleStructureFactory.createProjectStructureByTestStructure(moduleStructure, testServices, project)
    }

    override fun prepareFilesInModule(ktTestModule: KtTestModule, testServices: TestServices) {
        val testModule = ktTestModule.testModule
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val compilerConfiguration = compilerConfigurationProvider.getCompilerConfiguration(testModule)
        val project = compilerConfigurationProvider.getProject(testModule)
        val packageProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(testModule)
        JvmResolveUtil.analyze(project, ktTestModule.files.filterIsInstance<KtFile>(), compilerConfiguration, packageProviderFactory)
    }

    override fun computeTestDataPath(path: Path): Path {
        val newPath = path.resolveSibling(path.nameWithoutExtension + "." + testPrefix + "." + path.extension)
        if (newPath.toFile().exists()) return newPath
        return path
    }
}
