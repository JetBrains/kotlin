/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils.libraries.binary

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.ClassFileViewProviderFactory
import com.intellij.psi.FileTypeFileViewProviders
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.fir.FirFrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.test.framework.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinClassFileDecompiler
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.FileAttributeService
import org.jetbrains.kotlin.analysis.decompiler.stub.files.DummyFileAttributeService
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.registerTestServices
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.LibraryEnvironmentConfigurator
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

object LibraryFrontendApiTestConfiguratorService : FrontendApiTestConfiguratorService {
    override val allowDependedAnalysisSession: Boolean get() = false

    override fun TestConfigurationBuilder.configureTest(disposable: Disposable) {
        useConfigurators(::LibraryEnvironmentConfigurator)
        usePreAnalysisHandlers(::LibraryModuleRegistrarPreAnalysisHandler)
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

    override fun registerApplicationServices(application: MockApplication) {
        FirFrontendApiTestConfiguratorService.registerApplicationServices(application)
        if (application.getServiceIfCreated(ClsKotlinBinaryClassCache::class.java) != null) return
        application.registerService(ClsKotlinBinaryClassCache::class.java)
        application.registerService(FileAttributeService::class.java, DummyFileAttributeService)

        FileTypeFileViewProviders.INSTANCE.addExplicitExtension(
            JavaClassFileType.INSTANCE,
            ClassFileViewProviderFactory()
        )

        @Suppress("DEPRECATION")
        ClassFileDecompilers.getInstance().EP_NAME.point.apply {
            registerExtension(KotlinClassFileDecompiler(), LoadingOrder.FIRST)
            registerExtension(KotlinBuiltInDecompiler(), LoadingOrder.FIRST)
        }
    }

    override fun doOutOfBlockModification(file: KtFile) {
        FirFrontendApiTestConfiguratorService.doOutOfBlockModification(file)
    }
}
