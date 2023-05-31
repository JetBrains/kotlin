/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.cli.common.SessionWithSources
import org.jetbrains.kotlin.cli.common.prepareJsSessions
import org.jetbrains.kotlin.cli.common.prepareJvmSessions
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.PLATFORM_DEPENDANT_METADATA
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE_VERSION
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.getAllJsDependenciesPaths
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.io.File

class JvmLoadedMetadataDumpHandler(testServices: TestServices) : AbstractLoadedMetadataDumpHandler<BinaryArtifacts.Jvm>(
    testServices,
    ArtifactKinds.Jvm
) {
    override val targetPlatform: TargetPlatform
        get() = JvmPlatforms.defaultJvmPlatform
    override val platformAnalyzerServices: PlatformDependentAnalyzerServices
        get() = JvmPlatformAnalyzerServices
    override val dependencyKind: DependencyKind
        get() = DependencyKind.Binary

    override fun prepareSessions(
        module: TestModule,
        configuration: CompilerConfiguration,
        environment: VfsBasedProjectEnvironment,
        moduleName: Name,
        libraryList: DependencyListForCliModule,
    ): List<SessionWithSources<KtFile>> {
        return prepareJvmSessions(
            files = emptyList(),
            configuration, environment, moduleName,
            extensionRegistrars = emptyList(),
            environment.getSearchScopeForProjectLibraries(),
            libraryList,
            isCommonSource = { false },
            fileBelongsToModule = { _, _ -> false },
            createProviderAndScopeForIncrementalCompilation = { null }
        )
    }
}

class KlibLoadedMetadataDumpHandler(testServices: TestServices) : AbstractLoadedMetadataDumpHandler<BinaryArtifacts.KLib>(
    testServices,
    ArtifactKinds.KLib
) {
    override val targetPlatform: TargetPlatform
        get() = JsPlatforms.defaultJsPlatform
    override val platformAnalyzerServices: PlatformDependentAnalyzerServices
        get() = JsPlatformAnalyzerServices
    override val dependencyKind: DependencyKind
        get() = DependencyKind.KLib

    override fun prepareSessions(
        module: TestModule,
        configuration: CompilerConfiguration,
        environment: VfsBasedProjectEnvironment,
        moduleName: Name,
        libraryList: DependencyListForCliModule,
    ): List<SessionWithSources<KtFile>> {
        val libraries = getAllJsDependenciesPaths(module, testServices)
        val resolvedLibraries = CommonKLibResolver.resolve(libraries, DummyLogger).getFullResolvedList()
        return prepareJsSessions(
            files = emptyList(),
            configuration,
            moduleName,
            resolvedLibraries.map { it.library },
            libraryList,
            extensionRegistrars = emptyList(),
            isCommonSource = { false },
            fileBelongsToModule = { _, _ -> false },
            lookupTracker = null,
            icData = null
        )
    }
}

abstract class AbstractLoadedMetadataDumpHandler<A : ResultingArtifact.Binary<A>>(
    testServices: TestServices,
    override val artifactKind: BinaryKind<A>
) : BinaryArtifactHandler<A>(
    testServices,
    artifactKind,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false
) {
    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper()

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: A) {
        if (testServices.loadedMetadataSuppressionDirective in module.directives) return
        val languageVersion = module.directives.singleOrZeroValue(LANGUAGE_VERSION)
        val languageVersionSettings = if (languageVersion != null) {
            LanguageVersionSettingsImpl(languageVersion, ApiVersion.createByLanguageVersion(languageVersion))
        } else {
            LanguageVersionSettingsImpl.DEFAULT
        }

        val emptyModule = TestModule(
            name = "empty", module.targetPlatform, module.targetBackend, FrontendKinds.FIR,
            BackendKinds.IrBackend, module.binaryKind, files = emptyList(),
            allDependencies = listOf(DependencyDescription(module.name, dependencyKind, DependencyRelation.RegularDependency)),
            RegisteredDirectives.Empty, languageVersionSettings
        )
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(emptyModule)
        val environment = VfsBasedProjectEnvironment(
            testServices.compilerConfigurationProvider.getProject(emptyModule),
            VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
            testServices.compilerConfigurationProvider.getPackagePartProviderFactory(emptyModule)
        )
        val moduleName = Name.identifier(emptyModule.name)
        val binaryModuleData = BinaryModuleData.initialize(
            moduleName,
            targetPlatform,
            platformAnalyzerServices
        )
        val libraryList = FirFrontendFacade.initializeLibraryList(
            emptyModule, binaryModuleData, targetPlatform, configuration, testServices
        )

        val session = prepareSessions(
            emptyModule,
            configuration,
            environment,
            moduleName,
            libraryList
        ).single().session

        val packageFqName = FqName("test")
        dumper.builderForModule(module)
            .append(collectPackageContent(session, packageFqName, extractNames(module, packageFqName)))
    }

    protected abstract val targetPlatform: TargetPlatform
    protected abstract val platformAnalyzerServices: PlatformDependentAnalyzerServices
    protected abstract val dependencyKind: DependencyKind

    protected abstract fun prepareSessions(
        module: TestModule,
        configuration: CompilerConfiguration,
        environment: VfsBasedProjectEnvironment,
        moduleName: Name,
        libraryList: DependencyListForCliModule,
    ): List<SessionWithSources<KtFile>>

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()

        val frontendKind = testServices.defaultsProvider.defaultFrontend

        val commonExtension = ".fir.txt"
        val (specificExtension, otherSpecificExtension) = when (frontendKind) {
            FrontendKinds.ClassicFrontend -> ".fir.k1.txt" to ".fir.k2.txt"
            FrontendKinds.FIR -> ".fir.k2.txt" to ".fir.k1.txt"
            else -> shouldNotBeCalled()
        }

        val targetPlatform = testServices.defaultsProvider.defaultPlatform
        if (PLATFORM_DEPENDANT_METADATA in testServices.moduleStructure.allDirectives) {
            val platformExtension = specificExtension.replace(".txt", "${targetPlatform.suffix}.txt")
            val otherPlatformExtension = specificExtension.replace(".txt", "${targetPlatform.oppositeSuffix}.txt")

            val expectedFile = testDataFile.withExtension(platformExtension)
            val actualText = dumper.generateResultingDump()
            assertions.assertEqualsToFile(expectedFile, actualText, message = { "Content is not equal" })

            val checks = listOf(commonExtension, specificExtension, otherSpecificExtension).map { extension ->
                {
                    val baseFile = testDataFile.withExtension(extension)
                    assertions.assertFalse(baseFile.exists()) {
                        "Base file $baseFile exists in presence of $PLATFORM_DEPENDANT_METADATA directive. Please remove file or directive"
                    }
                }
            }
            assertions.assertAll(checks)
            val secondFile = testDataFile.withExtension(otherPlatformExtension)
            val common = testDataFile.withExtension(specificExtension)
            checkDumpsIdentity(
                testDataFile, expectedFile, secondFile, common,
                postProcessTestData = { it.replace("// $PLATFORM_DEPENDANT_METADATA\n", "") }
            )
        } else {
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
                checkDumpsIdentity(testDataFile, specificFirDump, otherFirDump, commonFirDump)
            }
        }
    }

    private val TargetPlatform.suffix: String
        get() = when {
            isJvm() -> ".jvm"
            isJs() -> ".klib"
            else -> error("Unsupported platform: $this")
        }

    private val TargetPlatform.oppositeSuffix: String
        get() = when {
            isJvm() -> ".klib"
            isJs() -> ".jvm"
            else -> error("Unsupported platform: $this")
        }

    private fun checkDumpsIdentity(
        testDataFile: File,
        file1: File,
        file2: File,
        commonFile: File,
        postProcessTestData: ((String) -> String)? = null
    ) {
        if (!file1.exists() || !file2.exists()) return
        val dump1 = file1.readText().trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd()
        val dump2 = file2.readText().trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd()
        if (dump1 == dump2) {
            commonFile.writeText(dump1)
            file1.delete()
            file2.delete()
            if (postProcessTestData != null) {
                testDataFile.writeText(postProcessTestData(testDataFile.readText()))
            }
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
