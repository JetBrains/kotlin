/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.fir.utils.configureOptionalTestCompilerPlugin
import org.jetbrains.kotlin.analysis.api.impl.base.references.HLApiReferenceProviderService
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.FirLowLevelAnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.references.KotlinFirReferenceContributor
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

object FirAnalysisApiTestConfiguratorService : AnalysisApiTestConfiguratorService {
    override fun TestConfigurationBuilder.configureTest(disposable: Disposable) {
        with(FirLowLevelAnalysisApiTestConfiguratorService) { configureTest(disposable) }
        configureOptionalTestCompilerPlugin()
    }

    override fun processTestFiles(files: List<KtFile>): List<KtFile> {
        return FirLowLevelAnalysisApiTestConfiguratorService.processTestFiles(files)
    }

    override fun getOriginalFile(file: KtFile): KtFile {
        return FirLowLevelAnalysisApiTestConfiguratorService.getOriginalFile(file)
    }

    override fun registerProjectServices(
        project: MockProject,
        compilerConfig: CompilerConfiguration,
        files: List<KtFile>,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        projectStructureProvider: ProjectStructureProvider,
        jarFileSystem: CoreJarFileSystem,
    ) {
        FirLowLevelAnalysisApiTestConfiguratorService.registerProjectServices(
            project,
            compilerConfig,
            files,
            packagePartProvider,
            projectStructureProvider,
            jarFileSystem
        )
    }

    override fun registerApplicationServices(application: MockApplication) {
        FirLowLevelAnalysisApiTestConfiguratorService.registerApplicationServices(application)
        if (application.getServiceIfCreated(KotlinReferenceProvidersService::class.java) == null) {
            application.registerService(KotlinReferenceProvidersService::class.java, HLApiReferenceProviderService::class.java)
            application.registerService(KotlinReferenceProviderContributor::class.java, KotlinFirReferenceContributor::class.java)
        }
    }

    override fun doOutOfBlockModification(file: KtFile) {
        FirLowLevelAnalysisApiTestConfiguratorService.doOutOfBlockModification(file)
    }
}
