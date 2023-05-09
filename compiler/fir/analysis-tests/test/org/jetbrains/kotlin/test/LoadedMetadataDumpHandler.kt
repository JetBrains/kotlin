/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.cli.common.prepareJvmSessions
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.test.backend.handlers.JvmBinaryArtifactHandler
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.io.File

class LoadedMetadataDumpHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        val emptyModule = TestModule(
            name = "empty", JvmPlatforms.defaultJvmPlatform, TargetBackend.JVM_IR, FrontendKinds.FIR,
            BackendKinds.IrBackend, ArtifactKinds.Jvm, files = emptyList(),
            allDependencies = listOf(DependencyDescription(module.name, DependencyKind.Binary, DependencyRelation.RegularDependency)),
            RegisteredDirectives.Empty, LanguageVersionSettingsImpl.DEFAULT
        )
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(emptyModule)
        val environment = VfsBasedProjectEnvironment(
            testServices.compilerConfigurationProvider.getProject(emptyModule),
            VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
            testServices.compilerConfigurationProvider.getPackagePartProviderFactory(emptyModule)
        )
        val binaryModuleData = BinaryModuleData.initialize(
            Name.identifier(emptyModule.name),
            JvmPlatforms.defaultJvmPlatform,
            JvmPlatformAnalyzerServices
        )
        val session = prepareJvmSessions(
            files = emptyList<KtFile>(),
            configuration, environment, Name.identifier(emptyModule.name),
            extensionRegistrars = emptyList(),
            environment.getSearchScopeForProjectLibraries(),
            DependencyListForCliModule.build(binaryModuleData),
            isCommonSource = { false },
            fileBelongsToModule = { _, _ -> false },
            createProviderAndScopeForIncrementalCompilation = { null }
        ).single().session

        val packageFqName = FqName("test")
        dumper.builderForModule(module)
            .append(collectPackageContent(session, packageFqName, extractNames(module, packageFqName)))
    }

    @Suppress("warnings")
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val commonExtension = ".fir.txt"
        val (specificExtension, otherSpecificExtension) = when (testServices.defaultsProvider.defaultFrontend) {
            FrontendKinds.ClassicFrontend -> ".fir.k1.txt" to ".fir.k2.txt"
            FrontendKinds.FIR -> ".fir.k2.txt" to ".fir.k1.txt"
            else -> shouldNotBeCalled()
        }
        val commonFirDump = testDataFile.withExtension(commonExtension)
        val specificFirDump = testDataFile.withExtension(specificExtension)

        val expectedFile = when {
            commonFirDump.exists() -> commonFirDump
            else -> specificFirDump
        }

        val actualText = dumper.generateResultingDump()
        assertions.assertEqualsToFile(expectedFile, actualText, message = { "Content is not equal" })

        if (commonFirDump.exists() && specificFirDump.exists()) {
            assertions.fail {
                """
                    Common dump ${commonFirDump.name} and specific ${specificFirDump.name} exist at the same time
                    Please remove ${specificFirDump.name}
                """.trimIndent()
            }
        }
        if (!commonFirDump.exists()) {
            val otherFirDump = testDataFile.withExtension(otherSpecificExtension)
            checkK1AndK2DumpsIdentity(specificFirDump, otherFirDump, commonFirDump)
        }
    }

    private fun checkK1AndK2DumpsIdentity(file1: File, file2: File, commonFile: File) {
        if (!file1.exists() || !file2.exists()) return
        val dump1 = file1.readText().trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd()
        val dump2 = file2.readText().trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd()
        if (dump1 == dump2) {
            commonFile.writeText(dump1)
            file1.delete()
            file2.delete()
            assertions.fail {
                """
                    Files ${file1.name} and ${file2.name} are identical
                    Generating ${commonFile.name} and deleting ${file1.name} and ${file2.name}
                """.trimIndent()
            }
        }
    }

    private fun collectPackageContent(session: FirSession, packageFqName: FqName, declarationNames: Collection<Name>): String {
        val provider = session.symbolProvider

        val builder = StringBuilder()
        val firRenderer = FirRenderer(builder)

        for (name in declarationNames) {
            for (symbol in provider.getTopLevelCallableSymbols(packageFqName, name)) {
                firRenderer.renderElementAsString(symbol.fir)
                builder.appendLine()
            }
        }

        for (name in declarationNames) {
            val classLikeSymbol = provider.getClassLikeSymbolByClassId(ClassId.topLevel(packageFqName.child(name))) ?: continue
            firRenderer.renderElementAsString(classLikeSymbol.fir)
            builder.appendLine()
        }

        return builder.toString().trimEnd()
    }

    private fun extractNames(module: TestModule, packageFqName: FqName): Collection<Name> {
        testServices.dependencyProvider.getArtifactSafe(module, FrontendKinds.ClassicFrontend)
            ?.let { return extractNames(it, packageFqName) }
        testServices.dependencyProvider.getArtifactSafe(module, FrontendKinds.FIR)
            ?.let { return extractNames(it, packageFqName) }
        error("Frontend artifact for module $module not found")
    }

    private fun extractNames(artifact: ClassicFrontendOutputArtifact, packageFqName: FqName): Collection<Name> {
        return DescriptorUtils.getAllDescriptors(artifact.analysisResult.moduleDescriptor.getPackage(packageFqName).memberScope)
            .mapTo(sortedSetOf()) { it.name }
    }

    private fun extractNames(artifact: FirOutputArtifact, packageFqName: FqName): Collection<Name> {
        return sortedSetOf<Name>().apply {
            for (part in artifact.partsForDependsOnModules) {
                val files = part.session.firProvider.getFirFilesByPackage(packageFqName)
                files.flatMapTo(this) { file ->
                    file.declarations.mapNotNull { (it as? FirMemberDeclaration)?.nameOrSpecialName }
                }
            }
        }
    }
}
