/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled.service

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.fir.utils.libraries.binary.LibraryAnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.decompiled.light.classes.ClsJavaStubByVirtualFileCache
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.CompiledLibraryProvider
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.ServiceRegistrationData

internal object SymbolLightClassesForLibraryAnalysisApiTestConfiguratorService : AnalysisApiTestConfiguratorService {
    override val analyseInDependentSession: Boolean get() = false
    override fun TestConfigurationBuilder.configureTest(disposable: Disposable) {
        with(LibraryAnalysisApiTestConfiguratorService) {
            configureTest(disposable)
        }
        useAdditionalServices(ServiceRegistrationData(CompiledLibraryProvider::class, ::CompiledLibraryProvider))
    }

    override fun registerProjectServices(
        project: MockProject,
        compilerConfig: CompilerConfiguration,
        files: List<KtFile>,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        projectStructureProvider: ProjectStructureProvider,
        jarFileSystem: CoreJarFileSystem,
    ) {
        LibraryAnalysisApiTestConfiguratorService.registerProjectServices(
            project,
            compilerConfig,
            files,
            packagePartProvider,
            projectStructureProvider,
            jarFileSystem,
        )
        project.registerService(ClsJavaStubByVirtualFileCache::class.java)
    }

    override fun registerApplicationServices(application: MockApplication) {
        LibraryAnalysisApiTestConfiguratorService.registerApplicationServices(application)
    }

    override fun doOutOfBlockModification(file: KtFile) {
        LibraryAnalysisApiTestConfiguratorService.doOutOfBlockModification(file)
    }
}
