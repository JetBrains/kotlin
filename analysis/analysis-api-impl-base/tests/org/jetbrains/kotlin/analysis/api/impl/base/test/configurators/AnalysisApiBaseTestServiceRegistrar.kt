/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.impl.base.references.HLApiReferenceProviderService
import org.jetbrains.kotlin.analysis.api.lifetime.KtDefaultLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.lifetime.KtReadActionConfinementDefaultLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.FileAttributeService
import org.jetbrains.kotlin.analysis.decompiler.stub.files.DummyFileAttributeService
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProviderImpl
import org.jetbrains.kotlin.analysis.providers.*
import org.jetbrains.kotlin.analysis.providers.impl.*
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiBaseTestServiceRegistrar: AnalysisApiTestServiceRegistrar()  {
    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
        @Suppress("UnstableApiUsage")
        project.extensionArea.apply {
            registerExtensionPoint(
                KtResolveExtensionProvider.EP_NAME.name,
                KtResolveExtensionProvider::class.java.name,
                ExtensionPoint.Kind.INTERFACE,
                false
            )
        }
    }

    @OptIn(KtAnalysisApiInternals::class)
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(KotlinModificationTrackerFactory::class.java, KotlinStaticModificationTrackerFactory::class.java)
            registerService(KtDefaultLifetimeTokenProvider::class.java, KtReadActionConfinementDefaultLifetimeTokenProvider::class.java)
        }
    }

    override fun registerProjectModelServices(project: MockProject, testServices: TestServices) {
        val moduleStructure = testServices.ktModuleProvider.getModuleStructure()
        val allKtFiles = moduleStructure.mainModules.flatMap { it.files.filterIsInstance<KtFile>() }
        val roots = StandaloneProjectFactory.getVirtualFilesForLibraryRoots(
            moduleStructure.binaryModules.flatMap { binary -> binary.getBinaryRoots() },
            testServices.environmentManager.getProjectEnvironment()
        ).distinct()
        project.apply {
            registerService(KtModuleScopeProvider::class.java, KtModuleScopeProviderImpl())
            registerService(KotlinAnnotationsResolverFactory::class.java, KotlinStaticAnnotationsResolverFactory(allKtFiles))

            registerService(KotlinDeclarationProviderFactory::class.java, KotlinStaticDeclarationProviderFactory(
                    project,
                    allKtFiles,
                    additionalRoots = roots
                ))
            registerService(KotlinPackageProviderFactory::class.java, KotlinStaticPackageProviderFactory(project, allKtFiles))
            registerService(KotlinReferenceProvidersService::class.java, HLApiReferenceProviderService::class.java)
            registerService(KotlinResolutionScopeProvider::class.java, KotlinByModulesResolutionScopeProvider::class.java)
        }
    }

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        application.registerService(KotlinFakeClsStubsCache::class.java, KotlinFakeClsStubsCache())
    }
}
