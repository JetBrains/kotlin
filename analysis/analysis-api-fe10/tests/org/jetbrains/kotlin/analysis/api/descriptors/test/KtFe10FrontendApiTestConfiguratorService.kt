/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.test

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.descriptors.CliFe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisHandlerExtension
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.descriptors.references.base.KtFe10KotlinReferenceProviderContributor
import org.jetbrains.kotlin.analysis.test.framework.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.base.references.HLApiReferenceProviderService
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

object KtFe10FrontendApiTestConfiguratorService : FrontendApiTestConfiguratorService {
    override val testPrefix: String
        get() = "descriptors"

    override val allowDependedAnalysisSession: Boolean
        get() = false

    override fun TestConfigurationBuilder.configureTest(disposable: Disposable) {
        usePreAnalysisHandlers(::KtFe10ModuleRegistrarPreAnalysisHandler.bind(disposable))
    }

    @OptIn(InvalidWayOfUsingAnalysisSession::class)
    override fun registerProjectServices(
        project: MockProject,
        compilerConfig: CompilerConfiguration,
        files: List<KtFile>,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        projectStructureProvider: ProjectStructureProvider,
        jarFileSystem: CoreJarFileSystem,
    ) {
        project.registerService(KtAnalysisSessionProvider::class.java, KtFe10AnalysisSessionProvider())
        project.registerService(Fe10AnalysisFacade::class.java, CliFe10AnalysisFacade(project))
        AnalysisHandlerExtension.registerExtension(project, KtFe10AnalysisHandlerExtension())
    }

    override fun registerApplicationServices(application: MockApplication) {
        if (application.getServiceIfCreated(KotlinReferenceProvidersService::class.java) == null) {
            application.registerService(KotlinReferenceProvidersService::class.java, HLApiReferenceProviderService::class.java)
            application.registerService(KotlinReferenceProviderContributor::class.java, KtFe10KotlinReferenceProviderContributor::class.java)
        }
    }

    override fun prepareTestFiles(files: List<KtFile>, module: TestModule, testServices: TestServices) {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val project = compilerConfigurationProvider.getProject(module)
        val compilerConfiguration = compilerConfigurationProvider.getCompilerConfiguration(module)
        val packageProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)
        JvmResolveUtil.analyze(project, files, compilerConfiguration, packageProviderFactory)
    }

    override fun doOutOfBlockModification(file: KtFile) {
        // TODO not supported yet
    }

    override fun preprocessTestDataPath(path: Path): Path {
        val newPath = path.resolveSibling(path.nameWithoutExtension + "." + testPrefix + "." + path.extension)
        if (newPath.toFile().exists()) return newPath
        return path
    }
}
