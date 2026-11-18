/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.cli.common.GroupedKtSources
import org.jetbrains.kotlin.cli.common.SessionWithSources
import org.jetbrains.kotlin.cli.common.prepareJsSessions
import org.jetbrains.kotlin.cli.common.prepareWasmSessions
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.renderer.FirDeclarationRenderer
import org.jetbrains.kotlin.fir.renderer.FirDeclarationRendererWithSpecificAttributes
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.ir.backend.js.loadWebKlibs
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.PLATFORM_DEPENDANT_METADATA
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.RENDER_SPECIFIC_FIR_DECLARATION_ATTRIBUTES
import org.jetbrains.kotlin.test.directives.assertEqualsToDump
import org.jetbrains.kotlin.test.directives.getClassifiedDumpFile
import org.jetbrains.kotlin.test.directives.getDefaultDumpFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.frontend.fir.DeprecatedTestFacade
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

class JvmLoadedMetadataDumpHandler(testServices: TestServices) : AbstractLoadedMetadataDumpHandler<BinaryArtifacts.Jvm>(
    testServices,
    ArtifactKinds.Jvm
) {
    override val targetPlatform: TargetPlatform
        get() = JvmPlatforms.defaultJvmPlatform

    override val dependencyKind: DependencyKind
        get() = DependencyKind.Binary

    override fun prepareSessions(
        module: TestModule,
        configuration: CompilerConfiguration,
        environment: VfsBasedProjectEnvironment,
        moduleName: Name,
        libraryList: DependencyListForCliModule,
    ): List<SessionWithSources> {
        return prepareJvmSessionsWithoutFiles(configuration, environment, moduleName, libraryList)
    }

    private fun prepareJvmSessionsWithoutFiles(
        configuration: CompilerConfiguration,
        environment: VfsBasedProjectEnvironment,
        moduleName: Name,
        libraryList: DependencyListForCliModule
    ): List<SessionWithSources> {
        return JvmFrontendPipelinePhase.prepareJvmSessions(
            sources = GroupedKtSources.EMPTY,
            rootModuleName = moduleName,
            configuration = configuration,
            projectEnvironment = environment,
            librariesClasspath = JvmClasspath.ProjectLibraries(),
            libraryList = libraryList,
            incrementalCompilationContext = null,
        )
    }
}

class KlibJsLoadedMetadataDumpHandler(testServices: TestServices) : AbstractLoadedMetadataDumpHandler<BinaryArtifacts.KLib>(
    testServices,
    ArtifactKinds.KLib
) {
    override val targetPlatform: TargetPlatform
        get() = JsPlatforms.defaultJsPlatform
    override val dependencyKind: DependencyKind
        get() = DependencyKind.Binary

    override fun prepareSessions(
        module: TestModule,
        configuration: CompilerConfiguration,
        environment: VfsBasedProjectEnvironment,
        moduleName: Name,
        libraryList: DependencyListForCliModule,
    ): List<SessionWithSources> {
        val klibs = loadWebKlibs(
            configuration = configuration,
            platformChecker = KlibPlatformChecker.JS,
        )

        return prepareJsSessions(
            sources = GroupedKtSources.EMPTY,
            configuration,
            moduleName,
            klibs.all,
            libraryList,
            extensionRegistrars = emptyList(),
            incrementalDataProvider = null
        )
    }
}

class KlibWasmJsLoadedMetadataDumpHandler(testServices: TestServices) : AbstractLoadedMetadataDumpHandler<BinaryArtifacts.KLib>(
    testServices,
    ArtifactKinds.KLib
) {
    override val targetPlatform: TargetPlatform
        get() = WasmPlatforms.wasmJs
    override val dependencyKind: DependencyKind
        get() = DependencyKind.Binary

    override fun prepareSessions(
        module: TestModule,
        configuration: CompilerConfiguration,
        environment: VfsBasedProjectEnvironment,
        moduleName: Name,
        libraryList: DependencyListForCliModule,
    ): List<SessionWithSources> {
        val klibs = loadWebKlibs(
            configuration = configuration,
            platformChecker = KlibPlatformChecker.Wasm(WasmTarget.JS.alias),
        )

        return prepareWasmSessions(
            sources = GroupedKtSources.EMPTY,
            configuration,
            moduleName,
            klibs.all,
            libraryList,
            extensionRegistrars = emptyList(),
            incrementalDataProvider = null
        )
    }
}

abstract class AbstractLoadedMetadataDumpHandler<A : ResultingArtifact.Binary<A>>(
    testServices: TestServices,
    override val artifactKind: ArtifactKind<A>
) : BinaryArtifactHandler<A>(
    testServices,
    artifactKind,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false
) {
    companion object {
        private const val DUMP_SUFFIX = ".fir.txt"
    }

    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper()

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: A) {
        val languageSettingsBuilder = testServices.defaultsProvider.newLanguageSettingsBuilder()
        languageSettingsBuilder.configureUsingDirectives(
            testServices.defaultDirectives,
            testServices.environmentConfigurators
        )

        val emptyModule = TestModule(
            name = "dump-${module.name}", files = emptyList(),
            allDependencies = listOf(DependencyDescription(module, dependencyKind, DependencyRelation.RegularDependency)),
            RegisteredDirectives.Empty, languageSettingsBuilder.build()
        )
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(emptyModule)
        val environment = VfsBasedProjectEnvironment(
            testServices.compilerConfigurationProvider.getProject(emptyModule),
            VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL),
            testServices.compilerConfigurationProvider.getPackagePartProviderFactory(emptyModule)
        )
        val moduleName = Name.identifier(emptyModule.name)
        @OptIn(DeprecatedTestFacade::class)
        val libraryList = FirFrontendFacade.initializeLibraryList(
            emptyModule, moduleName, targetPlatform, configuration, testServices
        )

        val session = prepareSessions(
            emptyModule,
            configuration,
            environment,
            moduleName,
            libraryList
        ).single().session

        val packageFqName = FqName("test")
        val printedAttributes = runIf(RENDER_SPECIFIC_FIR_DECLARATION_ATTRIBUTES in module.directives) {
            module.directives[RENDER_SPECIFIC_FIR_DECLARATION_ATTRIBUTES].ifNotEmpty { toSet() }
        }
        dumper.builderForModule(module)
            .append(collectPackageContent(session, packageFqName, extractNames(module, packageFqName), printedAttributes))
    }

    protected abstract val targetPlatform: TargetPlatform
    protected abstract val dependencyKind: DependencyKind

    protected abstract fun prepareSessions(
        module: TestModule,
        configuration: CompilerConfiguration,
        environment: VfsBasedProjectEnvironment,
        moduleName: Name,
        libraryList: DependencyListForCliModule,
    ): List<SessionWithSources>

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val actualText = if (dumper.isEmpty()) null else dumper.generateResultingDump()

        // Common logic: checks that actual dump is equal
        // to existing `.platform.fir.txt` / `.fir.txt` file.
        // If it is equal to `.fir.txt` one, also reports that
        // `.platform.fir.txt` is not needed.
        assertEqualsToDump(DUMP_SUFFIX, actualText)

        // Logic specific to this handler:
        val commonFile = testServices.moduleStructure.getDefaultDumpFile(DUMP_SUFFIX)
        val platformFile = testServices.moduleStructure.getClassifiedDumpFile(DUMP_SUFFIX)

        val oppositePlatformFile = when {
            platformFile.name.endsWith(".jvm$DUMP_SUFFIX") -> platformFile.withExtension(".klib$DUMP_SUFFIX")
            platformFile.name.endsWith(".klib$DUMP_SUFFIX") -> platformFile.withExtension(".jvm$DUMP_SUFFIX")
            else -> error("Dump classifier must be either '.jvm' or '.klib'.")
        }

        checkDumpsIdentity(
            testServices.moduleStructure.getDefaultDumpFile(DUMP_SUFFIX),
            platformFile, oppositePlatformFile, commonFile,
            postProcessTestData = { it.replace("// $PLATFORM_DEPENDANT_METADATA\n", "") }
        )

        if (PLATFORM_DEPENDANT_METADATA in testServices.moduleStructure.allDirectives) {
            assertions.assertFalse(commonFile.exists()) {
                "Base dump $commonFile exists in presence of $PLATFORM_DEPENDANT_METADATA directive. Please remove file or directive"
            }
        } else {
            assertions.assertFalse(platformFile.exists()) {
                "Please remove platform-specific dump $platformFile or add $PLATFORM_DEPENDANT_METADATA directive"
            }
        }
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

    private fun collectPackageContent(session: FirSession, packageFqName: FqName, declarationNames: Collection<Name>, printedAttributes: Set<String>?): String {
        val provider = session.symbolProvider

        val builder = StringBuilder()
        val firRenderer = FirRenderer(
            builder,
            declarationRenderer = if (printedAttributes != null) {
                FirDeclarationRendererWithSpecificAttributes(printedAttributes)
            } else {
                FirDeclarationRenderer()
            }
        )

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
        testServices.artifactsProvider.getArtifactSafe(module, FrontendKinds.FIR)
            ?.let { return extractNames(it, packageFqName) }
        error("Frontend artifact for module $module not found")
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
