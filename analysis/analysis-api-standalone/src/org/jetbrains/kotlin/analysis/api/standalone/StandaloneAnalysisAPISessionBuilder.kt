/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.FirStandaloneServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.LLFirStandaloneLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.FirSealedClassInheritorsProcessorFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModuleScopeProviderImpl
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.impl.KtModuleProviderImpl
import org.jetbrains.kotlin.analysis.project.structure.impl.KtSourceModuleImpl
import org.jetbrains.kotlin.analysis.project.structure.impl.buildKtModuleProviderByCompilerConfiguration
import org.jetbrains.kotlin.analysis.project.structure.impl.getPsiFilesFromPaths
import org.jetbrains.kotlin.analysis.project.structure.impl.getSourceFilePaths
import org.jetbrains.kotlin.analysis.providers.*
import org.jetbrains.kotlin.analysis.providers.impl.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProviderImpl
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public class StandaloneAnalysisAPISessionBuilder(
    applicationDisposable: Disposable,
    projectDisposable: Disposable,
    unitTestMode: Boolean,
    classLoader: ClassLoader = MockProject::class.java.classLoader
) {
    init {
        // We depend on swing (indirectly through PSI or something), so we want to declare headless mode,
        // to avoid accidentally starting the UI thread. But, don't set it if it was set externally.
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true")
        }
        setupIdeaStandaloneExecution()
    }

    private val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment =
        StandaloneProjectFactory.createProjectEnvironment(
            projectDisposable,
            applicationDisposable,
            unitTestMode,
            classLoader = classLoader
        )

    init {
        FirStandaloneServiceRegistrar.registerApplicationServices(kotlinCoreProjectEnvironment.environment.application)
    }

    public val application: Application = kotlinCoreProjectEnvironment.environment.application

    public val project: Project = kotlinCoreProjectEnvironment.project

    private lateinit var projectStructureProvider: KtStaticProjectStructureProvider

    public fun buildKtModuleProvider(init: KtModuleProviderBuilder.() -> Unit) {
        projectStructureProvider = buildProjectStructureProvider(kotlinCoreProjectEnvironment, init)
    }

    @Deprecated(
        "Compiler configuration is not a good fit for specifying multi-module project.",
        ReplaceWith("buildKtModuleProvider { }")
    )
    public fun buildKtModuleProviderByCompilerConfiguration(
        compilerConfiguration: CompilerConfiguration,
    ) {
        projectStructureProvider = buildKtModuleProviderByCompilerConfiguration(
            kotlinCoreProjectEnvironment,
            compilerConfiguration,
            getPsiFilesFromPaths(kotlinCoreProjectEnvironment, getSourceFilePaths(compilerConfiguration)),
        )
    }

    public fun <T : Any> registerApplicationService(serviceInterface: Class<T>, serviceImplementation: T) {
        kotlinCoreProjectEnvironment.environment.application.apply {
            registerService(serviceInterface, serviceImplementation)
        }
    }

    public fun <T : Any> registerApplicationService(serviceImplementation: Class<T>) {
        kotlinCoreProjectEnvironment.environment.application.apply {
            registerService(serviceImplementation)
        }
    }

    public fun <T : Any> registerProjectExtensionPoint(extensionDescriptor: ProjectExtensionDescriptor<T>) {
        extensionDescriptor.registerExtensionPoint(project)
    }

    @OptIn(KtAnalysisApiInternals::class)
    private fun registerProjectServices(
        sourceKtFiles: List<KtFile>,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    ) {
        val project = kotlinCoreProjectEnvironment.project
        project.apply {
            registerService(KotlinMessageBusProvider::class.java, KotlinProjectMessageBusProvider::class.java)

            FirStandaloneServiceRegistrar.registerProjectServices(project)
            FirStandaloneServiceRegistrar.registerProjectExtensionPoints(project)
            FirStandaloneServiceRegistrar.registerProjectModelServices(project, kotlinCoreProjectEnvironment.parentDisposable)

            registerService(KotlinModificationTrackerFactory::class.java, KotlinStaticModificationTrackerFactory::class.java)
            registerService(KotlinGlobalModificationService::class.java, KotlinStaticGlobalModificationService::class.java)

            registerService(KtModuleScopeProvider::class.java, KtModuleScopeProviderImpl())
            registerService(KotlinAnnotationsResolverFactory::class.java, KotlinStaticAnnotationsResolverFactory(sourceKtFiles))
            registerService(KotlinResolutionScopeProvider::class.java, KotlinByModulesResolutionScopeProvider::class.java)
            val declarationProviderFactory = KotlinStaticDeclarationProviderFactory(
                this,
                sourceKtFiles,
                kotlinCoreProjectEnvironment.environment.jarFileSystem as CoreJarFileSystem
            )
            registerService(
                KotlinDeclarationProviderFactory::class.java,
                declarationProviderFactory
            )
            registerService(KotlinDeclarationProviderMerger::class.java, KotlinStaticDeclarationProviderMerger(this))
            registerService(
                KotlinPackageProviderFactory::class.java,
                KotlinStaticPackageProviderFactory(project, sourceKtFiles + declarationProviderFactory.getAdditionalCreatedKtFiles())
            )

            registerService(
                FirSealedClassInheritorsProcessorFactory::class.java,
                object : FirSealedClassInheritorsProcessorFactory() {
                    override fun createSealedClassInheritorsProvider(): SealedClassInheritorsProvider {
                        return SealedClassInheritorsProviderImpl
                    }
                }
            )

            registerService(
                PackagePartProviderFactory::class.java,
                KotlinStaticPackagePartProviderFactory(packagePartProvider)
            )

            registerService(LLFirLibrarySymbolProviderFactory::class.java, LLFirStandaloneLibrarySymbolProviderFactory::class.java)
        }
    }

    private fun registerPsiDeclarationFromBinaryModuleProvider() {
        val ktModuleProviderImpl = projectStructureProvider as KtModuleProviderImpl
        kotlinCoreProjectEnvironment.project.apply {
            registerService(
                KotlinPsiDeclarationProviderFactory::class.java,
                KotlinStaticPsiDeclarationProviderFactory(
                    this,
                    ktModuleProviderImpl.binaryModules,
                    kotlinCoreProjectEnvironment.environment.jarFileSystem as CoreJarFileSystem
                )
            )
        }
    }

    public fun <T : Any> registerProjectService(serviceInterface: Class<T>, serviceImplementation: T) {
        kotlinCoreProjectEnvironment.project.apply {
            registerService(serviceInterface, serviceImplementation)
        }
    }

    public fun <T : Any> registerProjectService(serviceImplementation: Class<T>) {
        kotlinCoreProjectEnvironment.project.apply {
            registerService(serviceImplementation)
        }
    }

    public fun build(
        withPsiDeclarationFromBinaryModuleProvider: Boolean = false,
    ): StandaloneAnalysisAPISession {
        StandaloneProjectFactory.registerServicesForProjectEnvironment(
            kotlinCoreProjectEnvironment,
            projectStructureProvider,
        )
        val project = kotlinCoreProjectEnvironment.project
        val sourceKtFiles = projectStructureProvider.allSourceFiles.filterIsInstance<KtFile>()
        val libraryRoots = StandaloneProjectFactory.getAllBinaryRoots(
            projectStructureProvider.allKtModules,
            kotlinCoreProjectEnvironment,
        )
        val createPackagePartProvider =
            StandaloneProjectFactory.createPackagePartsProvider(
                project,
                libraryRoots,
            )
        registerProjectServices(
            sourceKtFiles,
            createPackagePartProvider,
        )
        if (withPsiDeclarationFromBinaryModuleProvider) {
            registerPsiDeclarationFromBinaryModuleProvider()
        }

        return StandaloneAnalysisAPISession(
            kotlinCoreProjectEnvironment,
            createPackagePartProvider,
        ) {
            projectStructureProvider.allKtModules.mapNotNull { ktModule ->
                if (ktModule !is KtSourceModule) return@mapNotNull null
                check(ktModule is KtSourceModuleImpl)
                ktModule to ktModule.sourceRoots.filterIsInstance<PsiFile>()
            }.toMap()
        }
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun buildStandaloneAnalysisAPISession(
    applicationDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.application"),
    projectDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project"),
    unitTestMode: Boolean = false,
    withPsiDeclarationFromBinaryModuleProvider: Boolean = false,
    classLoader: ClassLoader = MockProject::class.java.classLoader,
    init: StandaloneAnalysisAPISessionBuilder.() -> Unit,
): StandaloneAnalysisAPISession {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return StandaloneAnalysisAPISessionBuilder(
        applicationDisposable,
        projectDisposable,
        unitTestMode,
        classLoader
    ).apply(init).build(
        withPsiDeclarationFromBinaryModuleProvider,
    )
}
