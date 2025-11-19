/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import com.intellij.diagnostic.PluginException
import com.intellij.diagnostic.PluginProblemReporter
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers
import com.intellij.openapi.project.Project
import com.intellij.psi.ClassFileViewProviderFactory
import com.intellij.psi.FileTypeFileViewProviders
import com.intellij.psi.PsiClass
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.compiled.ClassFileDecompiler
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.impl.compiled.ClsDecompilerImpl
import com.intellij.psi.stubs.BinaryFileStubBuilders
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
import org.jetbrains.kotlin.analysis.decompiled.light.classes.ClsJavaStubByVirtualFileCache
import org.jetbrains.kotlin.analysis.decompiled.light.classes.DecompiledLightClassesFactory
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibMetaFileType
import org.jetbrains.kotlin.analysis.decompiler.konan.KotlinKlibMetadataDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinClassFileDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiBinaryLibraryIndexingMode
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.libraryIndexingConfiguration
import org.jetbrains.kotlin.analysis.test.framework.services.disposableProvider
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileClassProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.CliScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.serialization.deserialization.METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.NO_RUNTIME
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

object AnalysisApiBaseTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.apply {
            registerPlatformSettings(testServices)

            registerService(KotlinModificationTrackerFactory::class.java, KotlinStandaloneModificationTrackerFactory::class.java)

            //KotlinClassFileDecompiler is registered as application service so it's available for the tests run in parallel as well
            //when the decompiler is registered, for compiled class KtClsFile is created instead of ClsFileImpl
            //and KtFile doesn't return any classes in classOwner.getClasses if there is no KtFileClassProvider
            //but getClasses is used during java resolve, thus it's required to return some PsiClass for such cases
            registerService(KtFileClassProvider::class.java, KtClsFileClassProvider(project))
            registerService(ClsJavaStubByVirtualFileCache::class.java, ClsJavaStubByVirtualFileCache())
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

    class KtClsFileClassProvider(val project: Project) : KtFileClassProvider {
        override fun getFileClasses(file: KtFile): Array<PsiClass> {
            val virtualFile = file.virtualFile
            val classOrObject = file.declarations.filterIsInstance<KtClassOrObject>().singleOrNull()
            if (file is KtClsFile && virtualFile != null) {
                DecompiledLightClassesFactory.createClsJavaClassFromVirtualFile(file, virtualFile, classOrObject, project)?.let {
                    return arrayOf(it)
                }
            }
            return PsiClass.EMPTY_ARRAY
        }
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
                KotlinStandalonePackageProviderFactory(project, testKtFiles + ktFilesForBinaries)
            )
            registerService(KotlinPackageProviderMerger::class.java, KotlinStandalonePackageProviderMerger(project))
        }
    }

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        val applicationEnvironment = testServices.environmentManager.getApplicationEnvironment()
        val applicationDisposable = testServices.disposableProvider.getApplicationDisposable()

        applicationEnvironment.registerFileType(KotlinBuiltInFileType, BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION)
        applicationEnvironment.registerFileType(KotlinBuiltInFileType, METADATA_FILE_EXTENSION)
        applicationEnvironment.registerFileType(KlibMetaFileType, KLIB_METADATA_FILE_EXTENSION)

        // Everything related to decompiled files has to be registered here since we support stubs.
        // In particular, the library indexer requires them for correct work (which is applicable to almost all tests)
        for (fileType in listOf(JavaClassFileType.INSTANCE, KotlinBuiltInFileType, KlibMetaFileType)) {
            FileTypeFileViewProviders.INSTANCE.addExplicitExtension(
                fileType,
                ClassFileViewProviderFactory(),
                applicationDisposable,
            )

            BinaryFileStubBuilders.INSTANCE.addExplicitExtension(fileType, ClassFileStubBuilder(), applicationDisposable)
            BinaryFileTypeDecompilers.getInstance().addExplicitExtension(fileType, ClassFileDecompiler(), applicationDisposable)
        }

        ClassFileDecompilers.getInstance().EP_NAME.point.apply {
            registerExtension(KotlinClassFileDecompiler(), LoadingOrder.FIRST, applicationDisposable)
            registerExtension(KotlinBuiltInDecompiler(), LoadingOrder.FIRST, applicationDisposable)
            registerExtension(
                KotlinKlibMetadataDecompiler(),
                LoadingOrder.FIRST,
                applicationDisposable,
            )

            registerExtension(ClsDecompilerImpl(), LoadingOrder.FIRST, applicationDisposable)
        }

        // To properly handle exceptions from the stub builder
        @Suppress("UnstableApiUsage")
        application.registerService(
            PluginProblemReporter::class.java,
            PluginProblemReporter { errorMessage, cause, _ ->
                PluginException(errorMessage, cause, null)
            },
        )
    }
}
