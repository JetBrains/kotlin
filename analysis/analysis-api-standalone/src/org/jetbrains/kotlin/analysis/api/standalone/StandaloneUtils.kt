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
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.impl.base.references.HLApiReferenceProviderService
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.decompiled.light.classes.ClsJavaStubByVirtualFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.FirSealedClassInheritorsProcessorFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProviderImpl
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.impl.buildKtModuleProviderByCompilerConfiguration
import org.jetbrains.kotlin.analysis.project.structure.impl.getPsiFilesFromPaths
import org.jetbrains.kotlin.analysis.project.structure.impl.getSourceFilePaths
import org.jetbrains.kotlin.analysis.providers.*
import org.jetbrains.kotlin.analysis.providers.impl.*
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProviderImpl
import org.jetbrains.kotlin.idea.references.KotlinFirReferenceContributor
import org.jetbrains.kotlin.idea.references.KotlinReferenceProviderContributor
import org.jetbrains.kotlin.light.classes.symbol.SymbolKotlinAsJavaSupport
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
@Deprecated(
    "Use StandaloneAnalysisAPISessionBuilder.",
    ReplaceWith("buildStandaloneAnalysisAPISession { }")
)
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
 *   * [SymbolKotlinAsJavaSupport]
 *   * [ClsJavaStubByVirtualFileCache]
 *   * [KotlinModificationTrackerFactory]
 *   * [KotlinAnnotationsResolverFactory]
 *   * [LLfirResolveSessionService]
 *   * [FirSealedClassInheritorsProcessorFactory]
 *   * [KtModuleScopeProvider]
 *   * [ProjectStructureProvider]
 *   * [KotlinDeclarationProviderFactory]
 *   * [KotlinPackageProviderFactory]
 *   * [PackagePartProviderFactory]
 *   * [KotlinReferenceProvidersService]
 *   * [KotlinReferenceProviderContributor]
 *
 *  Note that [ProjectStructureProvider] is built by using
 *    * given [ktFiles] as Kotlin sources
 *    * other Java sources in [compilerConfig] (set via [addJavaSourceRoots])
 *    * JVM class paths in [compilerConfig] (set via [addJvmClasspathRoots]) as library.
 */
@Deprecated(
    "Use StandaloneAnalysisAPISessionBuilder.",
    ReplaceWith("buildStandaloneAnalysisAPISession { }")
)
public fun configureProjectEnvironment(
    project: MockProject,
    compilerConfig: CompilerConfiguration,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
) {
    val ktFiles = getPsiFilesFromPaths<KtFile>(project, getSourceFilePaths(compilerConfig))
    configureProjectEnvironment(project, compilerConfig, ktFiles, packagePartProvider)
}

internal fun configureProjectEnvironment(
    project: MockProject,
    compilerConfig: CompilerConfiguration,
    ktFiles: List<KtFile>,
    packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
) {
    reRegisterJavaElementFinder(project)

    project.registerService(
        KotlinModificationTrackerFactory::class.java,
        KotlinStaticModificationTrackerFactory()
    )

    // FIR LC
    project.registerService(
        ClsJavaStubByVirtualFileCache::class.java,
        ClsJavaStubByVirtualFileCache()
    )

    project.registerService(
        KotlinAnnotationsResolverFactory::class.java,
        KotlinStaticAnnotationsResolverFactory(ktFiles)
    )

    project.registerService(
        KotlinReferenceProvidersService::class.java,
        HLApiReferenceProviderService::class.java
    )

    project.registerService(
        KotlinReferenceProviderContributor::class.java,
        KotlinFirReferenceContributor::class.java
    )

    project.registerService(
        FirSealedClassInheritorsProcessorFactory::class.java,
        object : FirSealedClassInheritorsProcessorFactory() {
            override fun createSealedClassInheritorsProvider(): SealedClassInheritorsProvider {
                return SealedClassInheritorsProviderImpl
            }
        }
    )
    project.registerService(
        KtModuleScopeProvider::class.java,
        KtModuleScopeProviderImpl()
    )

    project.registerService(
        ProjectStructureProvider::class.java,
        buildKtModuleProviderByCompilerConfiguration(
            compilerConfig,
            project,
            ktFiles,
        )
    )
    project.registerService(
        KotlinDeclarationProviderFactory::class.java,
        KotlinStaticDeclarationProviderFactory(project, ktFiles)
    )
    project.registerService(
        KotlinDeclarationProviderMerger::class.java,
        KotlinStaticDeclarationProviderMerger(project)
    )
    project.registerService(
        KotlinPackageProviderFactory::class.java,
        KotlinStaticPackageProviderFactory(project, ktFiles)
    )
    project.registerService(
        PackagePartProviderFactory::class.java,
        KotlinStaticPackagePartProviderFactory(packagePartProvider)
    )
}

@OptIn(KtAnalysisApiInternals::class)
private fun reRegisterJavaElementFinder(project: Project) {
    PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)
    with(project as MockProject) {
        registerService(
            KtAnalysisSessionProvider::class.java,
            KtFirAnalysisSessionProvider(this)
        )
        picoContainer.unregisterComponent(KotlinAsJavaSupport::class.qualifiedName)
        registerService(
            KotlinAsJavaSupport::class.java,
            SymbolKotlinAsJavaSupport(project)
        )
    }
    @Suppress("DEPRECATION")
    PsiElementFinder.EP.getPoint(project).registerExtension(JavaElementFinder(project))
}
