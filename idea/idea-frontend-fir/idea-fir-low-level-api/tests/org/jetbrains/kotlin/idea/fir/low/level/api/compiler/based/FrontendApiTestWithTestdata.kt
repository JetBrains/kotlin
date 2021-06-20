/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based

import com.intellij.mock.MockProject
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.java.FirJavaElementFinder
import org.jetbrains.kotlin.fir.session.FirModuleInfoBasedModuleData
import org.jetbrains.kotlin.idea.fir.low.level.api.*
import org.jetbrains.kotlin.idea.fir.low.level.api.api.*
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSession
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.DeclarationProviderTestImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.KotlinOutOfBlockModificationTrackerFactoryTestImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.KtPackageProviderTestImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.SealedClassInheritorsProviderTestImpl
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.test.TestConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.*
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.nameWithoutExtension

abstract class FrontendApiTestWithTestdata : AbstractKotlinCompilerTest() {
    final override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }

        configureTest()
        defaultConfiguration(this)
        unregisterAllFacades()
        useFrontendFacades(::LowLevelFirFrontendFacade)
    }

    protected lateinit var testDataPath: Path

    protected fun testDataFileSibling(extension: String): Path {
        val extensionWithDot = "." + extension.removePrefix(".")
        return testDataPath.resolveSibling(testDataPath.nameWithoutExtension + extensionWithDot)
    }

    open fun TestConfigurationBuilder.configureTest() {}


    inner class LowLevelFirFrontendFacade(
        testServices: TestServices
    ) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
        override val additionalServices: List<ServiceRegistrationData>
            get() = listOf(service(::FirModuleInfoProvider))

        override val additionalDirectives: List<DirectivesContainer>
            get() = listOf(FirDiagnosticsDirectives)

        override fun analyze(module: TestModule): FirOutputArtifact {
            val project = testServices.compilerConfigurationProvider.getProject(module)
            val ktFiles = testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project)
            PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)

            val moduleInfo = TestModuleInfo(module)
            testServices.firModuleInfoProvider.registerModuleData(module, FirModuleInfoBasedModuleData(moduleInfo))
            val configurator = FirModuleResolveStateConfiguratorForSingleModuleTestImpl(testServices, module, ktFiles, moduleInfo)

            with(project as MockProject) {
                registerTestServices(configurator, ktFiles)
            }

            val resolveState = createResolveStateForNoCaching(moduleInfo, project) { configureSession() }
            return getArtifact(module, testServices, ktFiles, resolveState)
                ?: FirOutputArtifactImpl(
                    resolveState.rootModuleSession,
                    emptyMap(),
                    FirAnalyzerFacade(resolveState.rootModuleSession, configurator.getLanguageVersionSettings(moduleInfo))
                )
        }
    }


    protected open fun FirIdeSession.configureSession() {}

    protected abstract fun getArtifact(
        module: TestModule,
        testServices: TestServices,
        ktFiles: Map<TestFile, KtFile>,
        resolveState: FirModuleResolveState
    ): FirOutputArtifact?


    override fun runTest(filePath: String) {
        testDataPath = Paths.get(filePath)

        val configuration = testConfiguration(filePath, configuration)
        if (ignoreTest(filePath, configuration)) {
            return
        }
        super.runTest(filePath)
    }

    private fun ignoreTest(filePath: String, configuration: TestConfiguration): Boolean {
        val modules = configuration.moduleStructureExtractor.splitTestDataByModules(filePath, configuration.directives)

        if (modules.modules.size > 1) {
            return true // multimodule tests are not supported
        }

        val singleModule = modules.modules.single()
        if (singleModule.files.none { it.isKtFile }) {
            return true // nothing to highlight
        }

        return false
    }
}

class TestModuleInfo(
    val testModule: TestModule,
) : ModuleInfo, ModuleSourceInfoBase {
    override val name: Name get() = Name.identifier(testModule.name)

    override fun dependencies(): List<ModuleInfo> {
        return emptyList()
    }

    override val platform: TargetPlatform
        get() = testModule.targetPlatform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = testModule.targetPlatform.getAnalyzerServices()
}


class FirModuleResolveStateConfiguratorForSingleModuleTestImpl(
    private val testServices: TestServices,
    private val testModule: TestModule,
    private val ktFiles: Map<TestFile, KtFile>,
    private val moduleInfo: ModuleInfo,
) : FirModuleResolveStateConfigurator() {
    val moduleInfoProvider = testServices.firModuleInfoProvider
    val compilerConfigurationProvider = testServices.compilerConfigurationProvider

    val project = compilerConfigurationProvider.getProject(testModule)

    val languageVersionSettings = testModule.languageVersionSettings
    val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(testModule)

    val configuration = compilerConfigurationProvider.getCompilerConfiguration(testModule)

    val librariesScope = ProjectScope.getLibrariesScope(project)
    val sourcesScope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles.values)

    private val sealedClassInheritorsProvider = SealedClassInheritorsProviderTestImpl()

    override fun createPackagePartsProvider(scope: GlobalSearchScope): PackagePartProvider {
        return packagePartProviderFactory.invoke(scope)
    }

    override fun createModuleDataProvider(moduleInfo: ModuleSourceInfoBase): ModuleDataProvider {
        return DependencyListForCliModule.build(
            moduleInfo.name,
            moduleInfo.platform,
            moduleInfo.analyzerServices
        ) {
            dependencies(configuration.jvmModularRoots.map { it.toPath() })
            dependencies(configuration.jvmClasspathRoots.map { it.toPath() })

            friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())

            sourceDependencies(moduleInfoProvider.getRegularDependentSourceModules(testModule))
            sourceFriendsDependencies(moduleInfoProvider.getDependentFriendSourceModules(testModule))
            sourceDependsOnDependencies(moduleInfoProvider.getDependentDependsOnSourceModules(testModule))
        }.moduleDataProvider
    }

    override fun getLanguageVersionSettings(moduleInfo: ModuleSourceInfoBase): LanguageVersionSettings {
        return languageVersionSettings
    }

    override fun getModuleSourceScope(moduleInfo: ModuleSourceInfoBase): GlobalSearchScope {
        return sourcesScope
    }

    override fun createScopeForModuleLibraries(moduleInfo: ModuleSourceInfoBase): GlobalSearchScope {
        return librariesScope
    }

    override fun createSealedInheritorsProvider(): SealedClassInheritorsProvider {
        return sealedClassInheritorsProvider
    }

    override fun getModuleInfoFor(element: KtElement): ModuleInfo {
        return moduleInfo
    }

    override fun configureSourceSession(session: FirSession) {
        @Suppress("IncorrectParentDisposable")
        PsiElementFinder.EP.getPoint(project).registerExtension(FirJavaElementFinder(session, project), project)
    }
}

fun MockProject.registerTestServices(
    configurator: FirModuleResolveStateConfiguratorForSingleModuleTestImpl,
    ktFiles: Map<TestFile, KtFile>
) {
    registerService(
        FirModuleResolveStateConfigurator::class.java,
        configurator
    )
    registerService(FirIdeResolveStateService::class.java)
    registerService(
        KotlinOutOfBlockModificationTrackerFactory::class.java,
        KotlinOutOfBlockModificationTrackerFactoryTestImpl::class.java
    )
    registerService(KtDeclarationProviderFactory::class.java, object : KtDeclarationProviderFactory() {
        override fun createDeclarationProvider(searchScope: GlobalSearchScope): DeclarationProvider {
            return DeclarationProviderTestImpl(searchScope, ktFiles.values)
        }
    })
    registerService(KtPackageProviderFactory::class.java, object : KtPackageProviderFactory() {
        override fun createPackageProvider(searchScope: GlobalSearchScope): KtPackageProvider {
            return KtPackageProviderTestImpl(searchScope, ktFiles.values)
        }
    })
}
