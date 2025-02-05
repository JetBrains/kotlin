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
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalModificationService
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.permissions.KotlinAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinByModulesResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.base.KotlinStandalonePlatformSettings
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneFirCompilerPluginsProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneGlobalModificationService
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.packages.KotlinStandalonePackageProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.packages.KotlinStandalonePackageProviderMerger
import org.jetbrains.kotlin.analysis.api.standalone.base.permissions.KotlinStandaloneAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiSimpleServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.ApplicationServiceRegistration
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.FirStandaloneServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KotlinStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.registerProjectExtensionPoints
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.registerProjectModelServices
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.registerProjectServices
import org.jetbrains.kotlin.analysis.api.standalone.base.services.LLStandaloneFirElementByPsiElementChooser
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLFirElementByPsiElementChooser
import org.jetbrains.kotlin.analysis.project.structure.builder.KtModuleProviderBuilder
import org.jetbrains.kotlin.analysis.project.structure.builder.buildProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.impl.buildKtModuleProviderByCompilerConfiguration
import org.jetbrains.kotlin.analysis.project.structure.impl.getPsiFilesFromPaths
import org.jetbrains.kotlin.analysis.project.structure.impl.getSourceFilePaths
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
        )

    private val serviceRegistrars = listOf(FirStandaloneServiceRegistrar, StandaloneSessionServiceRegistrar)

    init {
        val application = kotlinCoreProjectEnvironment.environment.application
        ApplicationServiceRegistration.registerWithCustomRegistration(application, serviceRegistrars) {
            registerApplicationServices(application, data = Unit)
        }
    }

    public val application: Application = kotlinCoreProjectEnvironment.environment.application

    public val project: Project = kotlinCoreProjectEnvironment.project

    private lateinit var projectStructureProvider: KotlinStaticProjectStructureProvider

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
            serviceRegistrars.registerProjectExtensionPoints(project, data = Unit)
            serviceRegistrars.registerProjectServices(project, data = Unit)
            serviceRegistrars.registerProjectModelServices(project, kotlinCoreProjectEnvironment.parentDisposable, data = Unit)

            registerService(KotlinModificationTrackerFactory::class.java, KotlinStandaloneModificationTrackerFactory::class.java)
            registerService(KotlinGlobalModificationService::class.java, KotlinStandaloneGlobalModificationService::class.java)

            registerService(KotlinAnnotationsResolverFactory::class.java, KotlinStandaloneAnnotationsResolverFactory(this, sourceKtFiles))
            registerService(KotlinResolutionScopeProvider::class.java, KotlinByModulesResolutionScopeProvider::class.java)
            val declarationProviderFactory = KotlinStandaloneDeclarationProviderFactory(
                this,
                sourceKtFiles,
            )
            registerService(
                KotlinDeclarationProviderFactory::class.java,
                declarationProviderFactory
            )
            registerService(KotlinDeclarationProviderMerger::class.java, KotlinStandaloneDeclarationProviderMerger(this))
            registerService(
                KotlinPackageProviderFactory::class.java,
                KotlinStandalonePackageProviderFactory(project, sourceKtFiles + declarationProviderFactory.getAdditionalCreatedKtFiles())
            )
            registerService(KotlinPackageProviderMerger::class.java, KotlinStandalonePackageProviderMerger(this))

            registerService(
                KotlinPackagePartProviderFactory::class.java,
                KotlinStaticPackagePartProviderFactory(packagePartProvider)
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

    /**
     * Registers *optional* services for compiler plugin support.
     *
     * @param compilerConfiguration The [CompilerConfiguration] containing information about the registered compiler plugins.
     */
    public fun registerCompilerPluginServices(compilerConfiguration: CompilerConfiguration) {
        registerProjectService(
            KotlinCompilerPluginsProvider::class.java,
            KotlinStandaloneFirCompilerPluginsProvider(compilerConfiguration),
        )
    }

    @OptIn(KaExperimentalApi::class)
    public fun build(): StandaloneAnalysisAPISession {
        StandaloneProjectFactory.registerServicesForProjectEnvironment(
            kotlinCoreProjectEnvironment,
            projectStructureProvider,
        )

        val sourceKtFiles = projectStructureProvider.allSourceFiles.filterIsInstance<KtFile>()
        val libraryRoots = StandaloneProjectFactory.getAllBinaryRoots(
            projectStructureProvider.allModules,
            kotlinCoreProjectEnvironment,
        )

        val createPackagePartProvider = StandaloneProjectFactory.createPackagePartsProvider(libraryRoots)
        registerProjectServices(sourceKtFiles, createPackagePartProvider)

        return StandaloneAnalysisAPISession(kotlinCoreProjectEnvironment) {
            projectStructureProvider.allModules.mapNotNull { ktModule ->
                if (ktModule !is KaSourceModule) return@mapNotNull null
                ktModule to ktModule.psiRoots.filterIsInstance<PsiFile>()
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
            registerService(KotlinAnalysisPermissionOptions::class.java, KotlinStandaloneAnalysisPermissionOptions::class.java)
        }
    }

    override fun registerProjectServices(project: MockProject) {
        project.apply {
            registerService(KotlinLifetimeTokenFactory::class.java, KotlinAlwaysAccessibleLifetimeTokenFactory::class.java)
            registerService(KotlinPlatformSettings::class.java, KotlinStandalonePlatformSettings::class.java)

            registerService(LLFirElementByPsiElementChooser::class.java, LLStandaloneFirElementByPsiElementChooser::class.java)
        }
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun buildStandaloneAnalysisAPISession(
    projectDisposable: Disposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project"),
    unitTestMode: Boolean = false,
    init: StandaloneAnalysisAPISessionBuilder.() -> Unit,
): StandaloneAnalysisAPISession {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return StandaloneAnalysisAPISessionBuilder(
        projectDisposable,
        unitTestMode,
    ).apply(init).build()
}

/**
 * Disposes global resources which would persist after unloading Analysis API and IJ platform classes.
 *
 * **Important:** Once this function has been called, Analysis API *and* IntelliJ platform classes should not be used anymore. The classes
 * should either be unloaded or the whole program should be shut down.
 *
 * You don't need to use this endpoint right before your program shuts down. The purpose of this function is rather to dispose global
 * resources which would persist after unloading Analysis API and IJ platform classes. For example, an IJ platform class may be registered
 * with a JDK class. If Analysis API & IJ platform classes are unloaded, this global registration may keep alive the old class loader.
 *
 * Note: Everything in Standalone is experimental, but this endpoint is likely to change in the *near future*. Please consult with the
 * Analysis API team if you want to use this. (We'll want to know about your use case.)
 */
@Suppress("UnstableApiUsage")
@KaExperimentalApi
public fun disposeGlobalStandaloneApplicationServices() {
    AppExecutorUtil.shutdownApplicationScheduledExecutorService()
}
