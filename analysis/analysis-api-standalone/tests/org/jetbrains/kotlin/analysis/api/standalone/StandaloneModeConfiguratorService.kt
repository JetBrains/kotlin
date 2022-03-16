/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.fir.FirFrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.test.framework.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

object StandaloneModeConfiguratorService : FrontendApiTestConfiguratorService {
    override val allowDependedAnalysisSession: Boolean
        get() = false

    override fun TestConfigurationBuilder.configureTest(disposable: Disposable) {
        with(FirFrontendApiTestConfiguratorService) { configureTest(disposable) }
    }

    override fun registerProjectServices(
        project: MockProject,
        compilerConfig: CompilerConfiguration,
        files: List<KtFile>,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        projectStructureProvider: ProjectStructureProvider,
        jarFileSystem: CoreJarFileSystem,
    ) {
        configureProjectEnvironment(
            project,
            compilerConfig,
            files,
            packagePartProvider,
            jarFileSystem
        )
    }

    override fun registerApplicationServices(application: MockApplication) {
        configureApplicationEnvironment(application)
    }

    override fun doOutOfBlockModification(file: KtFile) {
        FirFrontendApiTestConfiguratorService.doOutOfBlockModification(file)
    }
}
