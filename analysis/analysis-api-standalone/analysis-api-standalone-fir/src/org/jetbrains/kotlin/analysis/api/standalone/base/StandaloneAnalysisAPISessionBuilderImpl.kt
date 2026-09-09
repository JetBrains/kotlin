/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.fir.utils.KaFirCacheCleaner
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.permissions.KotlinAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISessionBuilder
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.packages.KotlinStandalonePackageProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.packages.KotlinStandalonePackageProviderMerger
import org.jetbrains.kotlin.analysis.api.standalone.base.permissions.KotlinStandaloneAnalysisPermissionOptions
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.*
import org.jetbrains.kotlin.analysis.api.standalone.base.services.LLStandaloneFirElementByPsiElementChooser
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLFirElementByPsiElementChooser
import org.jetbrains.kotlin.analysis.project.structure.builder.KaModuleContainerBuilder
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.psi.KtFile

internal class StandaloneAnalysisAPISessionBuilderImpl(
    projectDisposable: Disposable,
    unitTestMode: Boolean,
) : StandaloneAnalysisAPISessionBuilder {
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
            registerApplicationServices(application, kotlinCoreProjectEnvironment.environment.parentDisposable, data = Unit)
        }
    }

    override val application: Application = kotlinCoreProjectEnvironment.environment.application

    override val project: Project = kotlinCoreProjectEnvironment.project

    private lateinit var projectStructureProvider: KotlinStaticProjectStructureProvider

    override fun buildKtModuleProvider(init: KaModuleContainerBuilder.() -> Unit) {
        val [moduleContainer, platform] = buildModuleContainer(kotlinCoreProjectEnvironment.environment, project, init)
        projectStructureProvider = KotlinStandaloneProjectStructureProvider(platform, project, moduleContainer)
    }

    override fun <T : Any> registerApplicationService(serviceInterface: Class<T>, serviceImplementation: T) {
        kotlinCoreProjectEnvironment.environment.application.apply {
            registerService(serviceInterface, serviceImplementation)
        }
    }

    override fun <T : Any> registerApplicationService(serviceImplementation: Class<T>) {
        kotlinCoreProjectEnvironment.environment.application.apply {
            registerService(serviceImplementation)
        }
    }

    override fun <T : Any> registerProjectExtensionPoint(extensionPointName: ExtensionPointName<T>, extensionClass: Class<T>) {
        CoreApplicationEnvironment.registerExtensionPoint(
            project.extensionArea,
            extensionPointName,
            extensionClass,
        )
    }

    private fun registerProjectServices(
        sourceKtFiles: List<KtFile>,
        libraryRoots: List<VirtualFile>,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    ) {
        val project = kotlinCoreProjectEnvironment.project
        project.apply {
            serviceRegistrars.registerProjectExtensionPoints(project, data = Unit)
            serviceRegistrars.registerProjectServices(project, data = Unit)
            serviceRegistrars.registerProjectModelServices(project, kotlinCoreProjectEnvironment.parentDisposable, data = Unit)

            registerService(KotlinModificationTrackerFactory::class.java, KotlinStandaloneModificationTrackerFactory::class.java)

            registerService(KotlinAnnotationsResolverFactory::class.java, KotlinStandaloneAnnotationsResolverFactory(this, sourceKtFiles))
            val declarationProviderFactory = KotlinStandaloneDeclarationProviderFactory(
                this,
                kotlinCoreProjectEnvironment.environment,
                sourceKtFiles,
            )
            registerService(
                KotlinDeclarationProviderFactory::class.java,
                declarationProviderFactory
            )
            registerService(KotlinDeclarationProviderMerger::class.java, KotlinStandaloneDeclarationProviderMerger(this))
            registerService(
                KotlinPackageProviderFactory::class.java,
                KotlinStandalonePackageProviderFactory(
                    project,
                    sourceKtFiles + declarationProviderFactory.getAdditionalCreatedKtFiles(),
                    libraryRoots
                )
            )
            registerService(KotlinPackageProviderMerger::class.java, KotlinStandalonePackageProviderMerger(this))

            registerService(
                KotlinPackagePartProviderFactory::class.java,
                KotlinStaticPackagePartProviderFactory(packagePartProvider)
            )

            if (!isCacheCleanerEnabled) {
                project.picoContainer.unregisterComponent(KaFirCacheCleaner::class.java)
            }
        }
    }

    override fun <T : Any> registerProjectService(serviceInterface: Class<T>, serviceImplementation: T) {
        kotlinCoreProjectEnvironment.project.apply {
            registerService(serviceInterface, serviceImplementation)
        }
    }

    override fun <T : Any> registerProjectService(serviceImplementation: Class<T>) {
        kotlinCoreProjectEnvironment.project.apply {
            registerService(serviceImplementation)
        }
    }

    private var isCacheCleanerEnabled = false

    override fun enableCacheCleaner() {
        isCacheCleanerEnabled = true
    }

    override fun build(): StandaloneAnalysisAPISession {
        StandaloneProjectFactory.registerServicesForProjectEnvironment(
            kotlinCoreProjectEnvironment,
            projectStructureProvider,
        )

        val sourceKtFiles = projectStructureProvider.allSourceFiles.filterIsInstance<KtFile>()
        val libraryRoots = StandaloneProjectFactory.getAllBinaryRoots(
            projectStructureProvider.allModules,
            kotlinCoreProjectEnvironment.environment,
        )

        val createPackagePartProvider = StandaloneProjectFactory.createPackagePartsProvider(libraryRoots)
        registerProjectServices(sourceKtFiles, libraryRoots.map { it.file }, createPackagePartProvider)

        return StandaloneAnalysisAPISessionImpl(kotlinCoreProjectEnvironment) {
            projectStructureProvider.allModules.mapNotNull { ktModule ->
                if (ktModule !is KaSourceModule) return@mapNotNull null
                ktModule to ktModule.psiRoots.filterIsInstance<PsiFile>()
            }.toMap()
        }
    }
}

internal class KotlinStaticPackagePartProviderFactory(
    private val packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
) : KotlinPackagePartProviderFactory {
    private val cache = ContainerUtil.createConcurrentSoftMap<GlobalSearchScope, PackagePartProvider>()

    override fun createPackagePartProvider(scope: GlobalSearchScope): PackagePartProvider {
        return cache.getOrPut(scope) {
            packagePartProvider(scope)
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
@KaImplementationDetail
object StandaloneSessionServiceRegistrar : AnalysisApiSimpleServiceRegistrar() {
    override fun registerApplicationServices(application: MockApplication, disposable: Disposable) {
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
