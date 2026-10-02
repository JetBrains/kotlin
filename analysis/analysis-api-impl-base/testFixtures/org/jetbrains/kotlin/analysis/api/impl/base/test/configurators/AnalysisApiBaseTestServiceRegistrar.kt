/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderMerger
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneAnnotationsResolverFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderMerger
import org.jetbrains.kotlin.analysis.api.standalone.base.modification.KotlinStandaloneModificationTrackerFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.packages.KotlinStandalonePackageProviderFactory
import org.jetbrains.kotlin.analysis.api.standalone.base.packages.KotlinStandalonePackageProviderMerger
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiBinaryLibraryIndexingMode
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.libraryIndexingConfiguration
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.CliScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.NO_RUNTIME
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

object AnalysisApiBaseTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerPlatformSettings(testServices)

            registerService(KotlinModificationTrackerFactory::class.java, KotlinStandaloneModificationTrackerFactory::class.java)

            registerService(ScriptDefinitionProvider::class.java, CliScriptDefinitionProvider())
        }
    }

    private fun MockProject.registerPlatformSettings(testServices: TestServices) {
        val deserializedDeclarationsOrigin = when (testServices.libraryIndexingConfiguration.binaryLibraryIndexingMode) {
            AnalysisApiBinaryLibraryIndexingMode.INDEX_STUBS -> KotlinDeserializedDeclarationsOrigin.STUBS
            AnalysisApiBinaryLibraryIndexingMode.NO_INDEXING -> KotlinDeserializedDeclarationsOrigin.BINARIES
        }

        val settings = object : KotlinPlatformSettings {
            override val deserializedDeclarationsOrigin: KotlinDeserializedDeclarationsOrigin = deserializedDeclarationsOrigin
        }

        registerService(KotlinPlatformSettings::class.java, settings)
    }

    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {
        val moduleStructure = testServices.ktTestModuleStructure
        val testKtFiles = moduleStructure.mainModules.flatMap { it.ktFiles }

        // We explicitly exclude decompiled libraries. Their decompiled PSI files are indexed by the declaration provider, so it shouldn't
        // additionally build and index stubs for the library.
        val mainBinaryModules = moduleStructure.mainModules
            .filter { it.moduleKind == TestModuleKind.LibraryBinary }
            .mapNotNull {
                // Builtins have `TestModuleKind.LibraryBinary` but `KaBuiltinsModule`
                // See KT-69367, builtins should probably be handled another way
                it.ktModule as? KaLibraryModule
            }

        val sharedBinaryDependencies = moduleStructure.binaryModules.toMutableSet()
        for (mainModule in moduleStructure.mainModules) {
            val ktModule = mainModule.ktModule
            if (ktModule !is KaLibraryModule) continue
            sharedBinaryDependencies -= ktModule
        }

        val mainBinaryRoots = StandaloneProjectFactory.getVirtualFilesForLibraryRoots(
            mainBinaryModules.flatMap { it.binaryRoots },
            testServices.environmentManager.getApplicationEnvironment(),
        ).distinct()

        val mainBinaryVirtualFiles = mainBinaryModules.flatMap { it.binaryVirtualFiles }.distinct()

        val sharedBinaryRoots = StandaloneProjectFactory.getVirtualFilesForLibraryRoots(
            sharedBinaryDependencies.flatMap { binary -> binary.binaryRoots },
            testServices.environmentManager.getApplicationEnvironment()
        ).distinct()

        val sharedBinaryVirtualFiles = sharedBinaryDependencies.flatMap { it.binaryVirtualFiles }.distinct()

        project.apply {
            registerService(KotlinAnnotationsResolverFactory::class.java, KotlinStandaloneAnnotationsResolverFactory(project, testKtFiles))

            val ktFilesForBinaries: List<KtFile>
            val shouldBuildStubsForBinaryLibraries =
                testServices.libraryIndexingConfiguration.binaryLibraryIndexingMode == AnalysisApiBinaryLibraryIndexingMode.INDEX_STUBS

            val declarationProviderFactory = KotlinStandaloneDeclarationProviderFactory(
                project,
                testServices.environmentManager.getApplicationEnvironment(),
                testKtFiles,
                binaryRoots = mainBinaryRoots + mainBinaryVirtualFiles,
                sharedBinaryRoots = sharedBinaryRoots + sharedBinaryVirtualFiles,
                skipBuiltins = testServices.moduleStructure.allDirectives.contains(NO_RUNTIME),
                shouldBuildStubsForBinaryLibraries = shouldBuildStubsForBinaryLibraries,
                shouldComputeBinaryLibraryPackageSets = true,
                postponeIndexing = true,
            )

            ktFilesForBinaries = declarationProviderFactory.getAdditionalCreatedKtFiles()
            registerService(
                KotlinDeclarationProviderFactory::class.java, declarationProviderFactory
            )
            registerService(KotlinDeclarationProviderMerger::class.java, KotlinStandaloneDeclarationProviderMerger(project))
            registerService(
                KotlinPackageProviderFactory::class.java,
                KotlinStandalonePackageProviderFactory(project, testKtFiles + ktFilesForBinaries, sharedBinaryRoots),
            )
            registerService(KotlinPackageProviderMerger::class.java, KotlinStandalonePackageProviderMerger(project))
        }
    }

    override fun registerApplicationServices(application: MockApplication, disposable: Disposable, testServices: TestServices) {}
}
