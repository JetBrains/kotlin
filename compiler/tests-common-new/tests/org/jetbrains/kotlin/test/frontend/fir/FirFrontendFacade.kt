/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.PsiBasedProjectFileSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.fir.analysis.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*

class FirFrontendFacade(
    testServices: TestServices,
    private val additionalSessionConfiguration: SessionConfiguration?
) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
    // Separate constructor is needed for creating callable references to it
    constructor(testServices: TestServices) : this(testServices, additionalSessionConfiguration = null)

    fun interface SessionConfiguration : (FirSessionFactory.FirSessionConfigurator) -> Unit

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirModuleInfoProvider))

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun analyze(module: TestModule): FirOutputArtifact {
        val moduleInfoProvider = testServices.firModuleInfoProvider
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        // TODO: add configurable parser

        val project = compilerConfigurationProvider.getProject(module)

        PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)

        val lightTreeEnabled = FirDiagnosticsDirectives.USE_LIGHT_TREE in module.directives
        val (ktFiles, originalFiles) = if (lightTreeEnabled) {
            emptyList<KtFile>() to module.files.filter { it.isKtFile }.map { testServices.sourceFileProvider.getRealFileForSourceFile(it) }
        } else {
            testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project).values to emptyList()
        }

        val languageVersionSettings = module.languageVersionSettings
        val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)

        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)

        val librariesScope = ProjectScope.getLibrariesScope(project)
        val sourcesScope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)

        val session = FirSessionFactory.createSessionWithDependencies(
            Name.identifier(module.name),
            module.targetPlatform,
            module.targetPlatform.getAnalyzerServices(),
            moduleInfoProvider.firSessionProvider,
            PsiBasedProjectEnvironment(
                project, VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
                { packagePartProviderFactory.invoke(it) }
            ),
            languageVersionSettings,
            PsiBasedProjectFileSearchScope(sourcesScope),
            PsiBasedProjectFileSearchScope(librariesScope),
            lookupTracker = null,
            providerAndScopeForIncrementalCompilation = null,
            extensionRegistrars = FirExtensionRegistrar.getInstances(project),
            needRegisterJavaElementFinder = true,
            dependenciesConfigurator = {
                dependencies(configuration.jvmModularRoots.map { it.toPath() })
                dependencies(configuration.jvmClasspathRoots.map { it.toPath() })

                friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())

                sourceDependencies(moduleInfoProvider.getRegularDependentSourceModules(module))
                sourceFriendsDependencies(moduleInfoProvider.getDependentFriendSourceModules(module))
                sourceDependsOnDependencies(moduleInfoProvider.getDependentDependsOnSourceModules(module))
            }
        ) {
            if (FirDiagnosticsDirectives.WITH_EXTENDED_CHECKERS in module.directives) {
                registerExtendedCommonCheckers()
            }
            additionalSessionConfiguration?.invoke(this)
        }

        moduleInfoProvider.registerModuleData(module, session.moduleData)

        val enablePluginPhases = FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES in module.directives
        val firAnalyzerFacade = FirAnalyzerFacade(
            session,
            languageVersionSettings,
            ktFiles,
            originalFiles,
            IrGenerationExtension.getInstances(project),
            lightTreeEnabled,
            enablePluginPhases
        )
        val firFiles = firAnalyzerFacade.runResolution()
        val filesMap = firFiles.mapNotNull { firFile ->
            val testFile = module.files.firstOrNull { it.name == firFile.name } ?: return@mapNotNull null
            testFile to firFile
        }.toMap()

        return FirOutputArtifactImpl(session, filesMap, firAnalyzerFacade)
    }
}

fun TargetPlatform.getAnalyzerServices(): PlatformDependentAnalyzerServices {
    return when {
        isJvm() -> JvmPlatformAnalyzerServices
        isJs() -> JsPlatformAnalyzerServices
        isNative() -> NativePlatformAnalyzerServices
        isCommon() -> CommonPlatformAnalyzerServices
        else -> error("Unknown target platform: $this")
    }
}
