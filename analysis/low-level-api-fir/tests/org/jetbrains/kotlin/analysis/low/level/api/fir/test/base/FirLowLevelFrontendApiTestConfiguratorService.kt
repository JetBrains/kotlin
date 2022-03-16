/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.ModuleRegistrarPreAnalysisHandler
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.registerTestServices
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.originalKtFile
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.test.framework.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

object FirLowLevelFrontendApiTestConfiguratorService : FrontendApiTestConfiguratorService {
    override fun TestConfigurationBuilder.configureTest(disposable: Disposable) {
        usePreAnalysisHandlers(::ModuleRegistrarPreAnalysisHandler.bind(disposable, false))
    }

    override fun processTestFiles(files: List<KtFile>): List<KtFile> {
        return files.map {
            val fakeFile = it.copy() as KtFile
            fakeFile.originalKtFile = it
            fakeFile
        }
    }

    override fun getOriginalFile(file: KtFile): KtFile {
        return file.originalKtFile!!
    }

    override fun registerProjectServices(
        project: MockProject,
        compilerConfig: CompilerConfiguration,
        files: List<KtFile>,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        projectStructureProvider: ProjectStructureProvider,
        jarFileSystem: CoreJarFileSystem,
    ) {
        project.registerTestServices(files, packagePartProvider, projectStructureProvider)
    }

    override fun registerApplicationServices(application: MockApplication) {}

    override fun doOutOfBlockModification(file: KtFile) {
        ServiceManager.getService(file.project, KotlinModificationTrackerFactory::class.java)
            .incrementModificationsCount()
    }
}
