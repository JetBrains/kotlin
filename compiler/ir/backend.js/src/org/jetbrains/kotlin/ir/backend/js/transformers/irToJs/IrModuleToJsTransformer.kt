/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.serialization.checkIsFunctionInterface
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.ic.JsExecutableProducer
import org.jetbrains.kotlin.ir.backend.js.jsexport.ExportModelToJsStatements
import org.jetbrains.kotlin.ir.backend.js.jsexport.ExportedDeclaration
import org.jetbrains.kotlin.ir.backend.js.jsexport.ExportedModule
import org.jetbrains.kotlin.ir.backend.js.lower.JsCodeOutliningLowering
import org.jetbrains.kotlin.ir.backend.js.lower.StaticMembersLowering
import org.jetbrains.kotlin.ir.backend.js.lower.extractedFromContainer
import org.jetbrains.kotlin.ir.backend.js.lower.isBuiltInClass
import org.jetbrains.kotlin.ir.backend.js.tsexport.joinTypeScriptFragments
import org.jetbrains.kotlin.ir.backend.js.tsexport.toTypeScriptFragment
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.ir.util.irError
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.js.artifacts.CachedTestFunctionsWithTheirPackage
import org.jetbrains.kotlin.js.artifacts.JsArtifactProducer
import org.jetbrains.kotlin.js.artifacts.PerFileGenerator
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.NoOpSourceLocationConsumer
import org.jetbrains.kotlin.js.backend.SourceLocationConsumer
import org.jetbrains.kotlin.js.backend.ast.JsCompositeBlock
import org.jetbrains.kotlin.js.backend.ast.JsLocation
import org.jetbrains.kotlin.js.backend.ast.JsSingleLineComment
import org.jetbrains.kotlin.js.common.makeValidES5Identifier
import org.jetbrains.kotlin.js.common.safeModuleName
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.js.sourceMap.SourceMapBuilderConsumer
import org.jetbrains.kotlin.js.util.NameTable
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import java.io.File
import java.util.*
import org.jetbrains.kotlin.ir.backend.js.jsexport.ExportModelGenerator as JsExportModelGenerator
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportModelGenerator as TsExportModelGenerator
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedDeclaration as TsExportedDeclaration

val IrModuleFragment.safeName: String
    get() = name.asString().safeModuleName

private fun IrDeclaration.findRootExtractedContainer(): IrDeclarationParent? {
    var current: IrDeclarationParent? = extractedFromContainer ?: return null
    while (current is IrDeclaration && current.extractedFromContainer != null) {
        current = current.extractedFromContainer
    }
    return current
}

// A class artifact must never reuse this postfix: its name would collide with the file-level exporter artifact
// (`<file>.export`), and duplicate artifact names are silently overwritten on write.
private val RESERVED_CLASS_ARTIFACT_POSTFIXES = setOf(EXPORTER_FILE_POSTFIX)

private fun IrClass.perClassArtifactPostfix(usedPostfixes: MutableSet<String>): String {
    // Lifted local classes and object expressions keep special names like `<no name provided>`, but carry
    // `localClassName`, which is already sanitized and unique within the file.
    val base = makeValidES5Identifier(localClassName ?: name.asStringStripSpecialMarkers())
    var postfix = if (".$base" in RESERVED_CLASS_ARTIFACT_POSTFIXES) "_$base" else base
    while (!usedPostfixes.add(postfix)) postfix += "_"
    return ".$postfix"
}

private typealias JsIrModules = JsArtifactProducer.ArtifactModules<JsIrModule>

class CompilerResult(
    val outputs: Map<TranslationMode, CompilationOutputs>,
    val rebuiltModules: Map<TranslationMode, List<String>> = emptyMap(),
) : Map<TranslationMode, CompilationOutputs> by outputs {
    constructor(outputs: Iterable<CompilationOutputs>) : this(outputs.associateBy { it.artifactConfiguration.translationMode })

    companion object {
        operator fun invoke(buildResults: Iterable<JsExecutableProducer.BuildResult>) : CompilerResult {
            val outputs = mutableMapOf<TranslationMode, CompilationOutputs>()
            val rebuiltModules = mutableMapOf<TranslationMode, List<String>>()
            for ((compilationOut, buildModules) in buildResults) {
                val translationMode = compilationOut.artifactConfiguration.translationMode
                outputs[translationMode] = compilationOut
                rebuiltModules[translationMode] = buildModules
            }
            return CompilerResult(outputs, rebuiltModules)
        }
    }
}

fun generateProxyIrModuleWith(
    safeName: String,
    externalName: String,
    mainFunctionTag: String?,
    suiteFunctionTag: String? = null,
    cachedTestFunctionsWithTheirPackage: CachedTestFunctionsWithTheirPackage = emptyMap(),
    importedWithEffectInModuleWithName: String? = null
): JsIrModule {
    val programFragment = JsIrProgramFragment(safeName, "<proxy-file>").apply {
        // INFO: we need it to "simulate"
        // that this program fragment has an effect in case if the [importedWithEffectInModuleWithName] was provided,
        // It should be revisited and reworked if the `per-module` granularity is dropped
        importedWithEffectInModuleWithName?.let {
            eagerInitializers.statements += JsCompositeBlock()
        }

        mainFunctionTag?.let {
            this.mainFunctionTag = it
            nameBindings[it] = ReservedJsNames.makeMainFunctionName()
        }
        cachedTestFunctionsWithTheirPackage.takeIf { it.isNotEmpty() }?.let {
            nameBindings += it.values.asSequence()
                .flatten()
                .map { tag -> tag to ReservedJsNames.makeTestFunctionName() }
                .plus(suiteFunctionTag!! to ReservedJsNames.makeSuiteFunctionName())

            JsTestFunctionTransformer.generateTestFunctionCall(
                it.asTestFunctionContainers(suiteFunctionTag, nameBindings)
            )?.run { declarations.statements += makeStmt() }
        }
    }

    return JsIrModule(
        safeName,
        externalName,
        listOf(programFragment),
        importedWithEffectInModuleWithName = importedWithEffectInModuleWithName
    )
}

enum class TranslationMode(
    val production: Boolean,
    val granularity: JsGenerationGranularity,
    val minimizedMemberNames: Boolean,
) {
    FULL_DEV(production = false, granularity = JsGenerationGranularity.WHOLE_PROGRAM, minimizedMemberNames = false),
    FULL_PROD(production = true, granularity = JsGenerationGranularity.WHOLE_PROGRAM, minimizedMemberNames = false),
    FULL_PROD_MINIMIZED_NAMES(production = true, granularity = JsGenerationGranularity.WHOLE_PROGRAM, minimizedMemberNames = true),
    PER_MODULE_DEV(production = false, granularity = JsGenerationGranularity.PER_MODULE, minimizedMemberNames = false),
    PER_MODULE_PROD(production = true, granularity = JsGenerationGranularity.PER_MODULE, minimizedMemberNames = false),
    PER_MODULE_PROD_MINIMIZED_NAMES(production = true, granularity = JsGenerationGranularity.PER_MODULE, minimizedMemberNames = true),
    PER_FILE_DEV(production = false, granularity = JsGenerationGranularity.PER_FILE, minimizedMemberNames = false),
    PER_FILE_PROD(production = true, granularity = JsGenerationGranularity.PER_FILE, minimizedMemberNames = false),
    PER_FILE_PROD_MINIMIZED_NAMES(production = true, granularity = JsGenerationGranularity.PER_FILE, minimizedMemberNames = true),

    PER_CLASS_DEV(production = false, granularity = JsGenerationGranularity.PER_CLASS, minimizedMemberNames = false),

    PER_CLASS_PROD(production = true, granularity = JsGenerationGranularity.PER_CLASS, minimizedMemberNames = false),

    PER_CLASS_PROD_MINIMIZED_NAMES(production = true, granularity = JsGenerationGranularity.PER_CLASS, minimizedMemberNames = true);

    companion object {
        fun fromFlags(
            production: Boolean,
            granularity: JsGenerationGranularity,
            minimizedMemberNames: Boolean
        ): TranslationMode {
            return when (granularity) {
                JsGenerationGranularity.PER_MODULE ->
                    if (production) {
                        if (minimizedMemberNames) PER_MODULE_PROD_MINIMIZED_NAMES
                        else PER_MODULE_PROD
                    } else PER_MODULE_DEV

                JsGenerationGranularity.PER_FILE ->
                    if (production) {
                        if (minimizedMemberNames) PER_FILE_PROD_MINIMIZED_NAMES
                        else PER_FILE_PROD
                    } else PER_FILE_DEV

                JsGenerationGranularity.PER_CLASS ->
                    if (production) {
                        if (minimizedMemberNames) PER_CLASS_PROD_MINIMIZED_NAMES
                        else PER_CLASS_PROD
                    } else PER_CLASS_DEV

                JsGenerationGranularity.WHOLE_PROGRAM ->
                    if (production) {
                        if (minimizedMemberNames) FULL_PROD_MINIMIZED_NAMES
                        else FULL_PROD
                    } else FULL_DEV
            }
        }
    }
}

class IrModuleToJsTransformer(
    private val backendContext: JsIrBackendContext,
    moduleToName: Map<IrModuleFragment, String> = emptyMap(),
) {
    private val shouldGeneratePolyfills = backendContext.configuration.getBoolean(JSConfigurationKeys.GENERATE_POLYFILLS)
    private val generateRegionComments = backendContext.configuration.getBoolean(JSConfigurationKeys.GENERATE_REGION_COMMENTS)
    private val shouldGenerateTypeScriptDefinitions = backendContext.configuration.getBoolean(JSConfigurationKeys.GENERATE_DTS)

    private val mainModuleName = backendContext.configuration[CommonConfigurationKeys.MODULE_NAME]!!
    private val moduleKind = backendContext.configuration[JSConfigurationKeys.MODULE_KIND]!!
    private val isEsModules = moduleKind == ModuleKind.ES
    private val sourceMapInfo = SourceMapsInfo.from(backendContext.configuration)

    private val moduleFragmentToNameMapper = ModuleFragmentToExternalName(moduleToName)

    private val jsExportModelGenerator by lazy {
        JsExportModelGenerator(backendContext, generateNamespacesForPackages = !isEsModules)
    }

    private val tsExportModelGenerator by lazy {
        runIf(shouldGenerateTypeScriptDefinitions) {
            TsExportModelGenerator(backendContext, isEsModules = isEsModules)
        }
    }

    private class IrFileExports(
        val file: IrFile,
        val externalDeclarations: List<IrDeclaration>,
        val declarationExports: Map<IrDeclaration, List<ExportedDeclaration>>,
        val tsDeclarationExports: Map<IrDeclaration, List<TsExportedDeclaration>>,
    )

    private class IrAndExportedDeclarations(val fragment: IrModuleFragment, val files: List<IrFileExports>)

    private fun associateIrAndExport(modules: Iterable<IrModuleFragment>): List<IrAndExportedDeclarations> {
        return modules.map { module ->
            val files = generateExportWithExternals(module.files)
            IrAndExportedDeclarations(module, files)
        }
    }

    private fun doStaticMembersLowering(modules: Iterable<IrModuleFragment>) {
        modules.forEach { module ->
            module.files.forEach {
                it.accept(backendContext.keeper, Keeper.KeepData(classInKeep = false, classShouldBeKept = false))
            }
        }

        modules.forEach { module ->
            module.files.forEach {
                StaticMembersLowering(backendContext).lower(it)
            }
        }
    }

    fun generateModule(
        modules: Iterable<IrModuleFragment>,
        artifactConfigurations: List<WebArtifactConfiguration>,
        outJsProgram: Boolean,
    ): CompilerResult {
        val exportData = associateIrAndExport(modules)
        doStaticMembersLowering(modules)

        val result = EnumMap<TranslationMode, CompilationOutputs>(TranslationMode::class.java)

        artifactConfigurations.filter { !it.production }.forEach {
            result[it.translationMode] = generateJsCode(exportData, it, outJsProgram)
        }

        if (artifactConfigurations.any { it.production }) {
            optimizeProgramByIr(modules, backendContext, moduleKind)
        }

        artifactConfigurations.filter { it.production }.forEach {
            result[it.translationMode] = generateJsCode(exportData, it, outJsProgram)
        }

        return CompilerResult(result)
    }

    fun makeIrFragmentsGenerators(
        dirtyFiles: Collection<IrFile>,
        allModules: Collection<IrModuleFragment>,
        granularity: JsGenerationGranularity,
    ): List<() -> JsIrProgramFragments> {
        val exportData = generateExportWithExternals(dirtyFiles)
        val mode = TranslationMode.fromFlags(production = false, granularity, minimizedMemberNames = false)

        doStaticMembersLowering(allModules)

        return exportData.map { { it.generateProgramFragment(it.file.declarations, isPrimary = true, mode) } }
    }

    private fun <E> generateExportWithExternals(rootFile: IrFile, generate: (IrPackageFragment) -> Map<IrDeclaration, List<E>>): Map<IrDeclaration, List<E>> {
        val exports = generate(rootFile)
        val additionalExports = backendContext.externalPackageFragment[rootFile.symbol]?.let(generate) ?: emptyMap()
        return additionalExports + exports
    }

    private fun generateExportWithExternals(
        irFiles: Collection<IrFile>,
    ): List<IrFileExports> {
        return irFiles.map { irFile ->
            val externalDeclarations = backendContext.externalPackageFragment[irFile.symbol]?.declarations.orEmpty().toList()
            val declarationExports = generateExportWithExternals(irFile, jsExportModelGenerator::generateExports)
            val tsDeclarationExports = tsExportModelGenerator?.let { ts ->
                generateExportWithExternals(irFile, ts::generateExports)
            } ?: emptyMap()
            IrFileExports(irFile, externalDeclarations, declarationExports, tsDeclarationExports)
        }
    }

    private fun generateJsCode(
        exportData: List<IrAndExportedDeclarations>,
        artifactConfiguration: WebArtifactConfiguration,
        outJsProgram: Boolean,
    ): CompilationOutputsBuilt {
        if (artifactConfiguration.minimizedMemberNames) {
            backendContext.fieldDataCache.clear()
            backendContext.minimizedNameGenerator.clear()
        }

        val program = JsIrProgram(
            ArtifactProducer(artifactConfiguration.translationMode)
                .generateArtifacts(exportData, artifactConfiguration.granularity)
        )

        return when (artifactConfiguration.granularity) {
            JsGenerationGranularity.WHOLE_PROGRAM -> generateSingleWrappedModuleBody(
                artifactConfiguration,
                program.asFragments(),
                sourceMapInfo,
                generateCallToMain = true,
                backendContext.configuration.useEs6ConstLet,
                outJsProgram = outJsProgram,
            )
            JsGenerationGranularity.PER_FILE,
            JsGenerationGranularity.PER_CLASS,
            JsGenerationGranularity.PER_MODULE,
                -> generateMultiWrappedModuleBody(
                artifactConfiguration,
                program,
                sourceMapInfo,
                outJsProgram,
                backendContext.configuration.useEs6ConstLet,
            )
        }
    }

    private inner class ArtifactProducer(
        private val mode: TranslationMode,
    ) : JsArtifactProducer<IrAndExportedDeclarations, IrFileExports, JsIrModule, JsIrProgramTestEnvironment> {
        override fun singleModuleToArtifact(module: IrAndExportedDeclarations, mainModule: IrAndExportedDeclarations): JsIrModule {
            val mainModuleSafeName = mainModule.fragment.safeName
            val couldBeReexportedInMain = !isEsModules && module !== mainModule
            val couldBeImportedWithEffectInMain = isEsModules && module !== mainModule

            return JsIrModule(
                moduleName = module.fragment.safeName,
                externalModuleName = moduleFragmentToNameMapper.getExternalNameFor(module.fragment),
                fragments = module.files.flatMap {
                    val fragments = it.generateProgramFragment(it.file.declarations, isPrimary = true, mode)
                    listOfNotNull(fragments.mainFragment, fragments.exportFragment)
                },
                reexportedInModuleWithName = runIf(couldBeReexportedInMain) { mainModuleSafeName },
                importedWithEffectInModuleWithName = runIf(couldBeImportedWithEffectInMain) { mainModuleSafeName },
            )
        }

        override fun makePerFileGenerator(
            mainModule: IrAndExportedDeclarations,
        ) = object : PerFileGenerator<IrAndExportedDeclarations, IrFileExports, JsIrModules, JsIrProgramTestEnvironment> {
            override val mainModuleName = mainModule.fragment.safeName
            private val JsIrModules.mainFragment get() = this.mainModule.fragments.first()

            override val IrAndExportedDeclarations.isMain get() = this === mainModule
            override val IrAndExportedDeclarations.fileList get() = files

            override val JsIrModules.artifactName get() = this.mainModule.externalModuleName
            override val JsIrModules.hasEffect get() = this.mainModule.importedWithEffectInModuleWithName != null
            override val JsIrModules.hasExport get() = exportModule != null
            override val JsIrModules.packageFqn get() = mainFragment.packageFqn
            override val JsIrModules.mainFunction get() = mainFragment.mainFunctionTag

            override fun JsIrModules.takeTestEnvironmentOwnership(): JsIrProgramTestEnvironment? {
                val fragment = mainFragment
                return fragment.testEnvironment.also { fragment.testEnvironment = null }
            }

            override val JsIrProgramTestEnvironment.testFunctionTag: String
                get() = testFunctionTag

            override val JsIrProgramTestEnvironment.suiteFunctionTag: String
                get() = suiteFunctionTag

            override fun List<JsIrModules>.merge() =
                JsIrModules(map { it.mainModule }.merge(), mapNotNull { it.exportModule }.ifNotEmpty { merge() })

            override fun IrAndExportedDeclarations.generateProxyArtifact(
                mainFunctionTag: String?,
                suiteFunctionTag: String?,
                testFunctions: CachedTestFunctionsWithTheirPackage,
                moduleNameForEffects: String?,
            ) = JsIrModules(
                generateProxyIrModuleWith(
                    fragment.safeName,
                    moduleFragmentToNameMapper.getExternalNameFor(fragment),
                    mainFunctionTag,
                    suiteFunctionTag,
                    testFunctions,
                    moduleNameForEffects
                )
            )

            override fun IrFileExports.generateArtifacts(module: IrAndExportedDeclarations): List<JsIrModules> {
                if (file.couldBeSkipped()) return []

                return when (mode.granularity) {
                    PER_CLASS -> generatePerClassArtifacts(module)
                    PER_FILE -> generatePerFileArtifacts(module)
                    else -> error("Unexpected granularity to generate artifacts for: ${mode.granularity}")
                }
            }

            private fun IrFileExports.generatePerClassArtifacts(module: IrAndExportedDeclarations): List<JsIrModules> {
                // Declarations of each class fragment: the class itself plus static members extracted from it by StaticMembersLowering
                val classFragments = mutableMapOf<IrClass, MutableList<IrDeclaration>>()
                val restDeclarations = mutableListOf<IrDeclaration>()
                // DCE can remove a class from the file without clearing the parents of the removed declarations,
                // so a static extracted from an eliminated class may still resolve to it: only group declarations
                // under classes that are still present in the file.
                val liveClasses = file.declarations.filterIsInstance<IrClass>().toSet()
                for (declaration in file.declarations) {
                    val ownerClass = declaration.findRootExtractedContainer() as? IrClass
                    when {
                        ownerClass != null && ownerClass.parent == file && ownerClass in liveClasses ->
                            classFragments.getOrPut(ownerClass) { mutableListOf() } += declaration
                        declaration is IrClass && declaration.origin != IrDeclarationOrigin.GENERATED_SAM_IMPLEMENTATION ->
                            classFragments.getOrPut(declaration) { mutableListOf() } += declaration
                        else ->
                            restDeclarations += declaration
                    }
                }

                val isSingleClassOnly = classFragments.size == 1 && restDeclarations.isEmpty()
                val isRestOnlyWithoutClasses = classFragments.isEmpty()

                // There is no need to introduce separate files for such cases, do not pollute the output
                if (isSingleClassOnly || isRestOnlyWithoutClasses) {
                    return generatePerFileArtifacts(module)
                }

                return buildList {
                    val usedPostfixes = mutableSetOf<String>()

                    for ([klass, classDeclarations] in classFragments) {
                        val fragments = generateProgramFragment(classDeclarations, isPrimary = false, mode)
                        val postfix = klass.perClassArtifactPostfix(usedPostfixes)
                        add(JsIrModules(
                            file.toJsIrModuleForPerClassFile(
                                module,
                                fragments.mainFragment,
                                postfix
                            ),
                            fragments.exportFragment?.run { file.toJsIrModuleForExport(module, this, postfix) }
                        ))
                    }

                    val restFragments = generateProgramFragment(restDeclarations, isPrimary = true, mode)
                    // The rest artifact is the primary one, so it is the only carrier of the polyfills, of the external
                    // declarations' exports and of the test environment: skip it only when it has no declarations and
                    // carries none of those.
                    val restArtifactIsNeeded = restDeclarations.isNotEmpty() ||
                            restFragments.mainFragment.polyfills.statements.isNotEmpty() ||
                            restFragments.mainFragment.testEnvironment != null ||
                            restFragments.mainFragment.mainFunctionTag != null ||
                            restFragments.exportFragment != null
                    if (restArtifactIsNeeded) {
                        add(JsIrModules(
                            file.toJsIrModule(
                                module,
                                restFragments.mainFragment,
                            ),
                            restFragments.exportFragment?.run { file.toJsIrModuleForExport(module, this) }
                        ))
                    }
                }
            }

            private fun IrFileExports.generatePerFileArtifacts(module: IrAndExportedDeclarations): List<JsIrModules> {
                val fragments = generateProgramFragment(file.declarations, isPrimary = true, mode)
                return listOf(JsIrModules(
                    file.toJsIrModule(module, fragments.mainFragment),
                    fragments.exportFragment?.run { file.toJsIrModuleForExport(module, this) }
                ))
            }
        }
    }

    private fun IrFile.toJsIrModule(module: IrAndExportedDeclarations, programFragment: JsIrProgramFragment): JsIrModule {
        return JsIrModule(
            moduleFragmentToNameMapper.getSafeNameFor(this),
            moduleFragmentToNameMapper.getExternalNameFor(this),
            listOf(programFragment),
            importedWithEffectInModuleWithName = runIf(programFragment.hasEffect) { module.fragment.safeName }
        )
    }

    private fun IrFile.toJsIrModuleForExport(
        module: IrAndExportedDeclarations,
        programFragment: JsIrProgramFragment,
        postfix: String = "",
    ): JsIrModule {
        return JsIrModule(
            moduleFragmentToNameMapper.getSafeNameExporterFor(this, postfix),
            moduleFragmentToNameMapper.getExternalNameForExporterFile(this, postfix),
            listOf(programFragment),
            module.fragment.safeName
        )
    }

    private fun IrFile.toJsIrModuleForPerClassFile(
        module: IrAndExportedDeclarations,
        programFragment: JsIrProgramFragment,
        postfix: String
    ): JsIrModule {
        return JsIrModule(
            moduleFragmentToNameMapper.getSafeNameFor(this, postfix),
            moduleFragmentToNameMapper.getExternalNameFor(this, postfix),
            listOf(programFragment),
            importedWithEffectInModuleWithName = runIf(programFragment.hasEffect) { module.fragment.safeName }
        )
    }

    private val pathPrefixMap = backendContext.configuration.getMap(JSConfigurationKeys.FILE_PATHS_PREFIX_MAP)
    private val optimizeGeneratedJs = backendContext.configuration.get(JSConfigurationKeys.OPTIMIZE_GENERATED_JS, true)

    private fun IrFileExports.generateProgramFragmentForExport(
        declarations: List<IrDeclaration>,
        mode: TranslationMode,
        nameScope: NameTable<IrDeclaration>
    ): JsIrProgramFragment? {
        val rawExports = declarations.flatMap { declarationExports[it] ?: emptyList() }
        val exports = jsExportModelGenerator.wrapInNamespaces(file.packageFqName, rawExports)

        val tsDeclarations = tsExportModelGenerator?.let { ts ->
            val rawTsExports = declarations.flatMap { tsDeclarationExports[it] ?: emptyList() }
            ts.wrapInNamespaces(file.packageFqName, rawTsExports)
                .ifNotEmpty { toTypeScriptFragment(moduleKind) }
        }

        if (exports.isEmpty()) return null

        val nameGenerator = JsNameLinkingNamer(backendContext, mode.minimizedMemberNames, isEsModules)
        val internalModuleName = ReservedJsNames.makeInternalModuleName().takeIf { !isEsModules }
        val staticContext = JsStaticContext(backendContext, nameGenerator, nameScope, mode)

        return JsIrProgramFragment("", file.packageFqName.asString())
            .also {
                it.dts = tsDeclarations
                if (exports.isNotEmpty()) {
                    it.exports.statements += ExportModelToJsStatements(staticContext)
                        .generateModuleExport(ExportedModule(mainModuleName, exports), internalModuleName, isEsModules)
                }
                it.computeAndSaveNameBindings(emptySet(), nameGenerator)
            }
    }

    private fun IrFileExports.generateProgramFragment(
        declarations: List<IrDeclaration>,
        isPrimary: Boolean,
        mode: TranslationMode
    ): JsIrProgramFragments {
        val globalNameScope = NameTable<IrDeclaration>()
        val nameGenerator = JsNameLinkingNamer(backendContext, mode.minimizedMemberNames, isEsModules)
        val staticContext = JsStaticContext(backendContext, nameGenerator, globalNameScope, mode)

        val result = JsIrProgramFragment(
            file.getJsFileName() ?: file.nameWithoutExtension,
            file.packageFqName.asString()
        ).apply {
            if (shouldGeneratePolyfills && isPrimary) {
                polyfills.statements += backendContext.polyfills.getAllPolyfillsFor(file)
            }
        }

        val statements = result.declarations.statements
        val ctx = JsGenerationContext(file.fileEntry, null, null, staticContext, useBareParameterNames = true)
        val fileStatements = declarations.map { it.accept(IrDeclarationToJsTransformer(), ctx) }
        val exportDeclarations = if (isPrimary) externalDeclarations + declarations else declarations
        val exportFragment = generateProgramFragmentForExport(exportDeclarations, mode, globalNameScope)

        if (fileStatements.isNotEmpty()) {
            var startComment = ""

            if (generateRegionComments) {
                startComment = "region "
            }

            if (generateRegionComments) {
                val originalPath = file.path
                val path = pathPrefixMap.entries
                    .find { [k, _] -> originalPath.startsWith(k) }
                    ?.let { [k, v] -> v + originalPath.substring(k.length) }
                    ?: originalPath

                startComment += "file: $path"
            }

            if (startComment.isNotEmpty()) {
                statements.add(JsSingleLineComment(startComment))
            }

            statements.addAll(fileStatements)
            if (generateRegionComments) {
                statements += JsSingleLineComment("endregion")
            }
        }

        staticContext.classModels.entries.forEach { [symbol, model] ->
            result.classes[nameGenerator.getNameForClass(symbol.owner)] =
                JsIrIcClassModel(model.dependsOnClasses.memoryOptimizedMap(staticContext::getNameForClass)).also {
                    it.preDeclarationBlock.statements += model.preDeclarationBlock.statements
                    it.postDeclarationBlock.statements += model.postDeclarationBlock.statements
                }
        }

        result.initializers.statements += staticContext.initializerBlock.statements
        result.eagerInitializers.statements += staticContext.eagerInitializerBlock.statements
        result.importedModules += nameGenerator.importedModules

        val definitionSet = declarations.toSet()

        if (backendContext.callMain && isPrimary) {
            JsMainFunctionDetector(backendContext).getMainFunctionOrNull(file)
                ?.mainFunctionWrapper
                ?.let { result.mainFunctionTag = definitionSet.computeTag(it) }
        }

        if (isPrimary) {
            backendContext.testFunsPerFile[file]
                ?.let { definitionSet.computeTag(it) }
                ?.let {
                    val suiteFun = backendContext.symbols.suiteFun!!
                    val suiteFunctionTag = definitionSet.computeTag(suiteFun.owner)
                        ?: irError("Expect suite function tag exists") {
                            withIrEntry("backendContext.suiteFun.owner", suiteFun.owner)
                        }
                    result.testEnvironment = JsIrProgramTestEnvironment(it, suiteFunctionTag)
                }
        }

        result.computeAndSaveNameBindings(definitionSet, nameGenerator)
        result.computeAndSaveImports(definitionSet, nameGenerator)
        result.computeAndSaveDefinitions(definitionSet)

        if (optimizeGeneratedJs) {
            optimizeFragmentByJsAst(result, staticContext)
        }

        return JsIrProgramFragments(result, exportFragment)
    }

    private fun Set<IrDeclaration>.computeTag(declaration: IrDeclaration): String? {
        // Use LEGACY here because the declaration may come from an old klib, in which its `IdSignature.CommonSignature`
        // doesn't have `description`, but only `id`. Hence, we always render the signature with `id` instead of `description`,
        // because otherwise there may be a mismatch when we're computing the tag first for the IrDeclaration deserialized from klib,
        // and then for the same declaration but constructed from a descriptor.
        //
        // The former won't have `description` in its `IdSignature`, the latter will have it,
        // which will result in different renders unless we use the LEGACY renderer.
        val tag = (backendContext.irFactory as IdSignatureRetriever).declarationSignature(declaration)?.render(IdSignatureRenderer.LEGACY)

        if (tag == null && !contains(declaration)) {
            irError("Signature not found for") {
                withIrEntry("declaration", declaration)
            }
        }

        return tag
    }

    private fun JsIrProgramFragment.computeAndSaveNameBindings(
        definitions: Set<IrDeclaration>,
        nameGenerator: JsNameLinkingNamer
    ) {
        nameGenerator.nameMap.entries.forEach { [declaration, name] ->
            definitions.computeTag(declaration)?.let { tag ->
                nameBindings[tag] = name
                if (isBuiltInClass(declaration) || checkIsFunctionInterface(declaration.symbol.signature)) {
                    optionalCrossModuleImports += tag
                }
            }
        }
    }

    private fun JsIrProgramFragment.computeAndSaveImports(
        definitions: Set<IrDeclaration>,
        nameGenerator: JsNameLinkingNamer
    ) {
        nameGenerator.imports.entries.forEach { [declaration, importExpression] ->
            val tag = definitions.computeTag(declaration)
                ?: irError("No tag for imported declaration") {
                    withIrEntry("declaration", declaration)
                }
            imports[tag] = importExpression
            optionalCrossModuleImports += tag
        }
    }

    private fun JsIrProgramFragment.computeAndSaveDefinitions(
        definitions: Set<IrDeclaration>,
    ) {
        definitions.forEach {
            definitions.computeTag(it)?.let { tag ->
                this.definitions += tag
            }

            if (it is IrClass && it.isInterface) {
                it.declarations.forEach {
                    definitions.computeTag(it)?.let { tag ->
                        this.definitions += tag
                    }
                }
            }
        }
    }

    private fun IrFile.couldBeSkipped(): Boolean = declarations.all { it.origin == JsCodeOutliningLowering.OUTLINED_JS_CODE_ORIGIN }
}

private fun generateMultiWrappedModuleBody(
    artifactConfiguration: WebArtifactConfiguration,
    program: JsIrProgram,
    sourceMapsInfo: SourceMapsInfo?,
    outJsProgram: Boolean,
    useEs6ConstLet: Boolean,
): CompilationOutputsBuilt {
    // mutable container allows explicitly remove elements from itself,
    // so we are able to help GC to free heavy JsIrModule objects
    // TODO: It makes sense to invent something better, because this logic can be easily broken
    val moduleToRef = program.asCrossModuleDependencies(artifactConfiguration.moduleKind, useEs6ConstLet).toMutableList()

    val mainModule = moduleToRef.removeLast().let { [main, mainRef] ->
        generateSingleWrappedModuleBody(
            artifactConfiguration,
            main.fragments,
            sourceMapsInfo,
            generateCallToMain = true,
            useEs6ConstLet,
            mainRef,
            outJsProgram
        )
    }

    mainModule.dependencies = moduleToRef.map { [module, moduleRef] ->
        generateSingleWrappedModuleBody(
            artifactConfiguration.copy(moduleName = module.externalModuleName, outputName = module.externalModuleName),
            module.fragments,
            sourceMapsInfo,
            generateCallToMain = false,
            useEs6ConstLet,
            moduleRef,
            outJsProgram,
        )
    }

    return mainModule
}

fun generateSingleWrappedModuleBody(
    artifactConfiguration: WebArtifactConfiguration,
    fragments: List<JsIrProgramFragment>,
    sourceMapsInfo: SourceMapsInfo?,
    generateCallToMain: Boolean,
    useEs6ConstLet: Boolean,
    crossModuleReferences: CrossModuleReferences = CrossModuleReferences.Empty(artifactConfiguration.moduleKind, useEs6ConstLet),
    outJsProgram: Boolean = true
): CompilationOutputsBuilt {
    val program = Merger(
        artifactConfiguration.moduleName,
        artifactConfiguration.moduleKind,
        fragments,
        crossModuleReferences,
        generateRegionComments = true,
        generateCallToMain,
        useEs6ConstLet,
    ).merge()

    program.resolveTemporaryNames()

    val jsCode = TextOutputImpl()

    val sourceMapBuilder: SourceMap3Builder?
    val sourceMapBuilderConsumer: SourceLocationConsumer
    if (sourceMapsInfo != null) {
        val sourceMapPrefix = sourceMapsInfo.sourceMapPrefix
        val outputDir = sourceMapsInfo.outputDir?.resolve(artifactConfiguration.moduleName.substringBeforeLast("/", ""))

        sourceMapBuilder = SourceMap3Builder(null, jsCode::getColumn, sourceMapPrefix).apply {
            addIgnoredSource(JsLocation.IGNORED.file)
        }

        val pathResolver = SourceFilePathResolver.create(sourceMapsInfo.sourceRoots, sourceMapPrefix, outputDir)

        val sourceMapContentEmbedding =
            sourceMapsInfo.sourceMapContentEmbedding

        sourceMapBuilderConsumer = SourceMapBuilderConsumer(
            File("."),
            sourceMapBuilder,
            pathResolver,
            sourceMapContentEmbedding != SourceMapSourceEmbedding.NEVER
        )
    } else {
        sourceMapBuilder = null
        sourceMapBuilderConsumer = NoOpSourceLocationConsumer
    }

    program.accept(JsToStringGenerationVisitor(jsCode, sourceMapBuilderConsumer))

    return CompilationOutputsBuilt(
        artifactConfiguration,
        jsCode.toString(),
        sourceMapBuilder?.build(),
        fragments.mapNotNull { it.dts }.ifNotEmpty { joinTypeScriptFragments() },
        program.takeIf { outJsProgram }
    )
}

val WebArtifactConfiguration.translationMode: TranslationMode
    get() = TranslationMode.fromFlags(production, granularity, minimizedMemberNames)
