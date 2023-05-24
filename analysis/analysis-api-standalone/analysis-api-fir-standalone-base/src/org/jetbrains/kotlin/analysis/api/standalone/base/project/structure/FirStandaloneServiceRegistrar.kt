/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.project.structure

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.impl.PsiElementFinderImpl
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.fir.references.ReadWriteAccessCheckerFirImpl
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveSessionService
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.JvmFirDeserializedSymbolProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirBuiltinsSessionFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionInvalidationService
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.JvmStubBasedDeserializedSymbolProviderFactory
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.idea.references.KotlinFirReferenceContributor
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor
import org.jetbrains.kotlin.idea.references.ReadWriteAccessChecker
import org.jetbrains.kotlin.light.classes.symbol.SymbolKotlinAsJavaSupport

@OptIn(LLFirInternals::class, KtAnalysisApiInternals::class)
object FirStandaloneServiceRegistrar : AnalysisApiStandaloneServiceRegistrar {
    override fun registerApplicationServices(application: MockApplication) {
        application.apply {

        }
    }

    override fun registerProjectExtensionPoints(project: MockProject) {
        IrGenerationExtension.registerExtensionPoint(project)
        FirExtensionRegistrarAdapter.registerExtensionPoint(project)
        LLFirSessionConfigurator.registerExtensionPoint(project)
    }

    override fun registerProjectServices(project: MockProject) {
        project.apply {
            registerService(KtAnalysisSessionProvider::class.java, KtFirAnalysisSessionProvider(this))
            registerService(LLFirResolveSessionService::class.java)
            registerService(LLFirSessionCache::class.java)
            registerService(KotlinAsJavaSupport::class.java, SymbolKotlinAsJavaSupport::class.java)
            registerService(LLFirGlobalResolveComponents::class.java)
            registerService(LLFirBuiltinsSessionFactory::class.java)
            registerService(JvmFirDeserializedSymbolProviderFactory::class.java, JvmStubBasedDeserializedSymbolProviderFactory::class.java)
            registerService(KotlinReferenceProviderContributor::class.java, KotlinFirReferenceContributor::class.java)
            registerService(ReadWriteAccessChecker::class.java, ReadWriteAccessCheckerFirImpl::class.java)

            registerService(LLFirSessionInvalidationService::class.java)
            LLFirSessionInvalidationService.getInstance(project).subscribeToModificationEvents()
        }
    }

    @Suppress("TestOnlyProblems")
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable) {
        with(PsiElementFinder.EP.getPoint(project)) {
            registerExtension(JavaElementFinder(project), disposable)
            registerExtension(PsiElementFinderImpl(project), disposable)
        }
    }
}