/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.impl.PsiElementFinderImpl
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveStateService
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.FirSealedClassInheritorsProcessorFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.PackagePartProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.LLFirSealedClassInheritorsProcessorFactoryForTests
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.PackagePartProviderTestImpl
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.idea.references.KotlinFirReferenceContributor
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor
import org.jetbrains.kotlin.light.classes.symbol.KotlinAsJavaFirSupport
import org.jetbrains.kotlin.light.classes.symbol.caches.SymbolLightClassFacadeCache
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiFirTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
        IrGenerationExtension.registerExtensionPoint(project)
        FirExtensionRegistrarAdapter.registerExtensionPoint(project)
    }

    @OptIn(InvalidWayOfUsingAnalysisSession::class, TestInfrastructureInternals::class)
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerService(KtAnalysisSessionProvider::class.java, KtFirAnalysisSessionProvider(this))
            registerService(FirSealedClassInheritorsProcessorFactory::class.java, LLFirSealedClassInheritorsProcessorFactoryForTests())
            registerService(LLFirResolveStateService::class.java)
            registerService(PackagePartProviderFactory::class.java, PackagePartProviderTestImpl(testServices))

            registerService(SymbolLightClassFacadeCache::class.java)
            registerService(KotlinAsJavaSupport::class.java, KotlinAsJavaFirSupport(project))
        }

        with(PsiElementFinder.EP.getPoint(project)) {
            registerExtension(JavaElementFinder(project), testServices.testConfiguration.rootDisposable)
            registerExtension(PsiElementFinderImpl(project), testServices.testConfiguration.rootDisposable)
        }
    }

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        application.apply {
            registerService(KotlinReferenceProviderContributor::class.java, KotlinFirReferenceContributor::class.java)
        }
    }
}