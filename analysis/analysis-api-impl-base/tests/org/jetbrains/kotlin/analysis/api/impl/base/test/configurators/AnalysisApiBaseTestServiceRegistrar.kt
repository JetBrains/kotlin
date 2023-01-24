/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.impl.base.references.HLApiReferenceProviderService
import org.jetbrains.kotlin.analysis.api.lifetime.KtDefaultLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.lifetime.KtReadActionConfinementDefaultLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProviderImpl
import org.jetbrains.kotlin.analysis.providers.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticPackageProviderFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiBaseTestServiceRegistrar: AnalysisApiTestServiceRegistrar()  {
    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {}

    @OptIn(KtAnalysisApiInternals::class)
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(KotlinModificationTrackerFactory::class.java, KotlinStaticModificationTrackerFactory::class.java)
            registerService(KtDefaultLifetimeTokenProvider::class.java, KtReadActionConfinementDefaultLifetimeTokenProvider::class.java)
        }
    }

    override fun registerProjectModelServices(project: MockProject, testServices: TestServices) {
        val allKtFiles = testServices.ktModuleProvider.getModuleStructure().mainModules.flatMap { it.files.filterIsInstance<KtFile>() }

        project.apply {
            registerService(KtModuleScopeProvider::class.java, KtModuleScopeProviderImpl())
            registerService(KotlinAnnotationsResolverFactory::class.java, KotlinStaticAnnotationsResolverFactory(allKtFiles))
            registerService(KotlinDeclarationProviderFactory::class.java, KotlinStaticDeclarationProviderFactory(project, allKtFiles))
            registerService(KotlinPackageProviderFactory::class.java, KotlinStaticPackageProviderFactory(allKtFiles))
            registerService(KotlinReferenceProvidersService::class.java, HLApiReferenceProviderService::class.java)
        }
    }

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {}
}
