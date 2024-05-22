/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.AnalysisApiSimpleServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.ApplicationServiceRegistration
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.FirStandaloneServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.LLFirStandaloneLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.services.LLStandaloneFirElementByPsiElementChooser
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLFirElementByPsiElementChooser
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirLibrarySymbolProviderFactory
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.impl.KtSourceModuleImpl
import org.jetbrains.kotlin.analysis.project.structure.impl.buildKtModuleProviderByCompilerConfiguration
import org.jetbrains.kotlin.analysis.project.structure.impl.getPsiFilesFromPaths
import org.jetbrains.kotlin.analysis.project.structure.impl.getSourceFilePaths
import org.jetbrains.kotlin.analysis.providers.*
import org.jetbrains.kotlin.analysis.providers.impl.*
import org.jetbrains.kotlin.analysis.providers.lifetime.KtAlwaysAccessibleLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.providers.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public class StandaloneAnalysisAPISessionBuilder(
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
            KotlinCoreApplicationEnvironmentMode.fromUnitTestModeFlag(unitTestMode),
            classLoader = classLoader
        )

    private val serviceRegistrars = listOf(FirStandaloneServiceRegistrar, StandaloneSessionServiceRegistrar)

    init {
        val application = kotlinCoreProjectEnvironment.environment.application
        ApplicationServiceRegistration.registerWithCustomRegistration(application, serviceRegistrars) {
            // TODO (KT-68186): Passing the class loader explicitly is a workaround for KT-68186.
            if (this is FirStandaloneServiceRegistrar) {
                registerApplicationServicesWithCustomClassLoader(application, classLoader)
            } else {
                registerApplicationServices(application, data = Unit)
            }
        }
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

    private fun registerProjectServices(
        sourceKtFiles: List<KtFile>,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    ) {
        val project = kotlinCoreProjectEnvironment.project
        project.apply {
            serviceRegistrars.forEach { it.registerProjectServices(project) }
            serviceRegistrars.forEach { it.registerProjectExtensionPoints(project) }
            serviceRegistrars.forEach { it.registerProjectModelServices(project, kotlinCoreProjectEnvironment.parentDisposable) }

            registerService(KotlinModificationTrackerFactory::class.java, KotlinStaticModificationTrackerFactory::class.java)
            registerService(KotlinGlobalModificationService::class.java, KotlinStaticGlobalModificationService::class.java)

            registerService(KotlinAnnotationsResolverFactory::class.java, KotlinStaticAnnotationsResolverFactory(this, sourceKtFiles))
            registerService(KotlinResolutionScopeProvider::class.java, KotlinByModulesResolutionScopeProvider::class.java)
            val declarationProviderFactory = KotlinStaticDeclarationProviderFactory(
                this,
                sourceKtFiles,
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
            registerService(KotlinPackageProviderMerger::class.java, KotlinStaticPackageProviderMerger(this))

            registerService(
                PackagePartProviderFactory::class.java,
                KotlinStaticPackagePartProviderFactory(packagePartProvider)
            )
        }
    }

    private fun registerPsiDeclarationFromBinaryModuleProvider() {
        kotlinCoreProjectEnvironment.project.apply {
            registerService(
                KotlinPsiDeclarationProviderFactory::class.java,
                KotlinStaticPsiDeclarationProviderFactory::class.java
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
        val sourceKtFiles = projectStructureProvider.allSourceFiles.filterIsInstance<KtFile>()
        val libraryRoots = StandaloneProjectFactory.getAllBinaryRoots(
            projectStructureProvider.allKtModules,
            kotlinCoreProjectEnvironment,
        )

        val createPackagePartProvider =
            StandaloneProjectFactory.createPackagePartsProvider(
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

/**
 * Registers services which are not covered by [FirStandaloneServiceRegistrar]. In general, this concerns services which need to be
 * registered for production Standalone and Standalone test usages, but *not* for IDE mode Analysis API tests, which rely on
 * [FirStandaloneServiceRegistrar] as a basis.
 *
 * When using this service registrar in tests, make sure that `AnalysisApiIdeModeTestServiceRegistrar` isn't configured at the same time.
 */
internal object StandaloneSessionServiceRegistrar : AnalysisApiSimpleServiceRegistrar() {
    override fun registerApplicationServices(application: MockApplication) {
        application.apply {
            // TODO (KT-68386): Re-enable once KT-68386 is fixed.
            //registerService(KotlinAnalysisPermissionOptions::class.java, KotlinStandaloneAnalysisPermissionOptions::class.java)
        }
    }

    override fun registerProjectServices(project: MockProject) {
        project.apply {
            registerService(KtLifetimeTokenProvider::class.java, KtAlwaysAccessibleLifetimeTokenProvider::class.java)

            registerService(LLFirLibrarySymbolProviderFactory::class.java, LLFirStandaloneLibrarySymbolProviderFactory::class.java)
            registerService(LLFirElementByPsiElementChooser::class.java, LLStandaloneFirElementByPsiElementChooser::class.java)
        }
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun buildStandaloneAnalysisAPISession(
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
        projectDisposable,
        unitTestMode,
        classLoader
    ).apply(init).build(
        withPsiDeclarationFromBinaryModuleProvider,
    )
}
