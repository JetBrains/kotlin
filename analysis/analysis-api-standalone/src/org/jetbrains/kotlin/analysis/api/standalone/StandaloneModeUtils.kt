/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.impl.base.references.HLApiReferenceProviderService
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.FirSealedClassInheritorsProcessorFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.PackagePartProviderFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProviderImpl
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.impl.ProjectStructureProviderByCompilerConfiguration
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticPackageProviderFactory
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProviderImpl
import org.jetbrains.kotlin.idea.references.KotlinFirReferenceContributor
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor
import org.jetbrains.kotlin.light.classes.symbol.IDEKotlinAsJavaFirSupport
import org.jetbrains.kotlin.light.classes.symbol.caches.SymbolLightClassFacadeCache
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService
import org.jetbrains.kotlin.psi.KtFile

/**
 * Configure Application environment for Analysis API standalone mode.
 *
 * In particular, this will register:
 *   * [KotlinReferenceProvidersService]
 *   * [KotlinReferenceProviderContributor]
 */
public fun configureApplicationEnvironment(app: MockApplication) {
    if (app.getServiceIfCreated(KotlinReferenceProvidersService::class.java) == null) {
        app.registerService(
            KotlinReferenceProvidersService::class.java,
            HLApiReferenceProviderService::class.java
        )
    }
    if (app.getServiceIfCreated(KotlinReferenceProviderContributor::class.java) == null) {
        app.registerService(
            KotlinReferenceProviderContributor::class.java,
            KotlinFirReferenceContributor::class.java
        )
    }
}

/**
 * Configure Project environment for Analysis API standalone mode.
 *
 * In particular, this will register:
 *   * [KtAnalysisSessionProvider]
 *   * [KotlinAsJavaSupport] (a FIR version)
 *   * [SymbolLightClassFacadeCache] for FIR light class support
 *   * [KotlinModificationTrackerFactory]
 *   * [LLFirResolveStateService]
 *   * [FirSealedClassInheritorsProcessorFactory]
 *   * [KtModuleScopeProvider]
 *   * [ProjectStructureProvider]
 *   * [KotlinDeclarationProviderFactory]
 *   * [KotlinPackageProviderFactory]
 *   * [PackagePartProviderFactory]
 *
 *  Note that [ProjectStructureProvider] is built by using
 *    * given [ktFiles] as Kotlin sources
 *    * other Java sources in [compilerConfig] (set via [addJavaSourceRoots])
 *    * JVM class paths in [compilerConfig] (set via [addJvmClasspathRoots]) as library.
 */
public fun configureProjectEnvironment(
    project: MockProject,
    compilerConfig: CompilerConfiguration,
    ktFiles: List<KtFile>,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
) {
    reRegisterJavaElementFinder(project)

    // FIR LC
    project.registerService(
        SymbolLightClassFacadeCache::class.java
    )

    project.picoContainer.registerComponentInstance(
        KotlinModificationTrackerFactory::class.qualifiedName,
        KotlinStaticModificationTrackerFactory()
    )
    RegisterComponentService.registerLLFirResolveStateService(project)
    project.picoContainer.registerComponentInstance(
        FirSealedClassInheritorsProcessorFactory::class.qualifiedName,
        object : FirSealedClassInheritorsProcessorFactory() {
            override fun createSealedClassInheritorsProvider(): SealedClassInheritorsProvider {
                return SealedClassInheritorsProviderImpl
            }
        }
    )
    project.picoContainer.registerComponentInstance(
        KtModuleScopeProvider::class.qualifiedName,
        KtModuleScopeProviderImpl()
    )

    project.picoContainer.registerComponentInstance(
        ProjectStructureProvider::class.qualifiedName,
        ProjectStructureProviderByCompilerConfiguration(compilerConfig, project, ktFiles)
    )
    project.picoContainer.registerComponentInstance(
        KotlinDeclarationProviderFactory::class.qualifiedName,
        KotlinStaticDeclarationProviderFactory(ktFiles)
    )
    project.picoContainer.registerComponentInstance(
        KotlinPackageProviderFactory::class.qualifiedName,
        KotlinStaticPackageProviderFactory(ktFiles)
    )
    project.picoContainer.registerComponentInstance(
        PackagePartProviderFactory::class.qualifiedName,
        object : PackagePartProviderFactory() {
            override fun createPackagePartProviderForLibrary(scope: GlobalSearchScope): PackagePartProvider {
                return packagePartProvider(scope)
            }
        }
    )
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
            IDEKotlinAsJavaFirSupport(project)
        )
    }
    @Suppress("DEPRECATION")
    PsiElementFinder.EP.getPoint(project).registerExtension(JavaElementFinder(project))
}
