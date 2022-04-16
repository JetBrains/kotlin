/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveStateService
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.FirSealedClassInheritorsProcessorFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.PackagePartProviderFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProviderImpl
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.providers.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticPackageProviderFactory
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProviderImpl
import org.jetbrains.kotlin.light.classes.symbol.KotlinAsJavaFirSupport
import org.jetbrains.kotlin.light.classes.symbol.caches.SymbolLightClassFacadeCache
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile

fun MockProject.registerTestServices(
    allKtFiles: List<KtFile>,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    projectStructureProvider: ProjectStructureProvider
) {
    registerService(
        PackagePartProviderFactory::class.java,
        PackagePartProviderTestImpl(packagePartProvider)
    )
    registerService(
        FirSealedClassInheritorsProcessorFactory::class.java,
        FirSealedClassInheritorsProcessorFactoryTestImpl()
    )
    registerService(KtModuleScopeProvider::class.java, KtModuleScopeProviderImpl())
    registerService(LLFirResolveStateService::class.java)
    registerService(
        KotlinModificationTrackerFactory::class.java,
        KotlinStaticModificationTrackerFactory::class.java
    )
    registerService(KotlinAnnotationsResolverFactory::class.java, KotlinStaticAnnotationsResolverFactory(allKtFiles))
    registerService(KotlinDeclarationProviderFactory::class.java, KotlinStaticDeclarationProviderFactory(allKtFiles))
    registerService(KotlinPackageProviderFactory::class.java, KotlinStaticPackageProviderFactory(allKtFiles))
    registerService(ProjectStructureProvider::class.java, projectStructureProvider)
    registerService(SymbolLightClassFacadeCache::class.java)
    reRegisterJavaElementFinder(this)
}

private class FirSealedClassInheritorsProcessorFactoryTestImpl : FirSealedClassInheritorsProcessorFactory() {
    override fun createSealedClassInheritorsProvider(): SealedClassInheritorsProvider {
        return SealedClassInheritorsProviderImpl
    }
}

private class PackagePartProviderTestImpl(
    private val packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
) : PackagePartProviderFactory() {
    override fun createPackagePartProviderForLibrary(scope: GlobalSearchScope): PackagePartProvider {
        return packagePartProvider(scope)
    }
}

@OptIn(InvalidWayOfUsingAnalysisSession::class)
private fun reRegisterJavaElementFinder(project: Project) {
    PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)
    with(project as MockProject) {
        picoContainer.registerComponentInstance(
            KtAnalysisSessionProvider::class.qualifiedName,
            KtFirAnalysisSessionProvider(this)
        )
        picoContainer.unregisterComponent(KotlinAsJavaSupport::class.qualifiedName)
        picoContainer.registerComponentInstance(
            KotlinAsJavaSupport::class.qualifiedName,
            KotlinAsJavaFirSupport(project)
        )
    }
    @Suppress("DEPRECATION")
    PsiElementFinder.EP.getPoint(project).registerExtension(JavaElementFinder(project))
}
