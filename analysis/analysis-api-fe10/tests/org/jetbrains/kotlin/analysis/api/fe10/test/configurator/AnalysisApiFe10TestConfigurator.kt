/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fe10.test.configurator

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiBaseTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiDecompiledCodeTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleProjectStructure
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtSourceModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.TestModuleStructureFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
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
            useAdditionalService<KtModuleFactory> { KtSourceModuleFactory() }
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
    ): KtModuleProjectStructure {
        return TestModuleStructureFactory.createProjectStructureByTestStructure(moduleStructure, testServices, project)
    }

    override fun prepareFilesInModule(files: List<PsiFile>, module: TestModule, testServices: TestServices) {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val compilerConfiguration = compilerConfigurationProvider.getCompilerConfiguration(module)
        val project = compilerConfigurationProvider.getProject(module)
        val packageProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)
        JvmResolveUtil.analyze(project, files.filterIsInstance<KtFile>(), compilerConfiguration, packageProviderFactory)
    }

    override fun preprocessTestDataPath(path: Path): Path {
        val newPath = path.resolveSibling(path.nameWithoutExtension + "." + testPrefix + "." + path.extension)
        if (newPath.toFile().exists()) return newPath
        return path
    }
}
