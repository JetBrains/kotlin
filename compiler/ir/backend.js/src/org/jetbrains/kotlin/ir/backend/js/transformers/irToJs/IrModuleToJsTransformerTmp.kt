/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.ir.backend.js.export.*
import org.jetbrains.kotlin.ir.backend.js.lower.StaticMembersLowering
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.backend.js.utils.serialization.JsIrAstSerializer
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.NoOpSourceLocationConsumer
import org.jetbrains.kotlin.js.backend.SourceLocationConsumer
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.js.sourceMap.SourceMapBuilderConsumer
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

enum class TranslationMode(val dce: Boolean, val perModule: Boolean) {
    FULL(dce = false, perModule = false),
    FULL_DCE(dce = true, perModule = false),
    PER_MODULE(dce = false, perModule = true),
    PER_MODULE_DCE(dce = true, perModule = true);

    companion object {
        fun fromFlags(dce: Boolean, perModule: Boolean): TranslationMode {
            return if (perModule) {
                if (dce) PER_MODULE_DCE else PER_MODULE
            } else {
                if (dce) FULL_DCE else FULL
            }
        }
    }
}

class IrModuleToJsTransformerTmp(
    private val backendContext: JsIrBackendContext,
    private val mainArguments: List<String>?,
    private val generateScriptModule: Boolean = false,
    private val relativeRequirePath: Boolean = false,
    private val moduleToName: Map<IrModuleFragment, String> = emptyMap(),
    private val removeUnusedAssociatedObjects: Boolean = true,
) {
    private val generateRegionComments = backendContext.configuration.getBoolean(JSConfigurationKeys.GENERATE_REGION_COMMENTS)

    private val mainModuleName = backendContext.configuration[CommonConfigurationKeys.MODULE_NAME]!!
    private val moduleKind = backendContext.configuration[JSConfigurationKeys.MODULE_KIND]!!

    fun generateModule(modules: Iterable<IrModuleFragment>, modes: Set<TranslationMode>): CompilerResult {
        val exportModelGenerator = ExportModelGenerator(backendContext, generateNamespacesForPackages = true)

        val exportData = modules.associate { module ->
            module to module.files.associate { file ->
                file to exportModelGenerator.generateExportWithExternals(file)
            }
        }

        val dts = wrapTypeScript(mainModuleName, moduleKind, exportData.values.flatMap { it.values.flatMap { it } }.toTypeScript(moduleKind))

        modules.forEach { module ->
            module.files.forEach { StaticMembersLowering(backendContext).lower(it) }
        }

        fun compilationOutput(multiModule: Boolean) = generateWrappedModuleBody(
            multiModule,
            mainModuleName,
            moduleKind,
            generateProgramFragments(modules, exportData),
            SourceMapsInfo.from(backendContext.configuration),
            relativeRequirePath,
            generateScriptModule,
        )

        val result = EnumMap<TranslationMode, CompilationOutputs>(TranslationMode::class.java)

        modes.filter { !it.dce }.forEach {
            result[it] = compilationOutput(it.perModule)
        }

        if (modes.any { it.dce }) {
            eliminateDeadDeclarations(modules, backendContext, removeUnusedAssociatedObjects)
        }

        modes.filter { it.dce }.forEach {
            result[it] = compilationOutput(it.perModule)
        }

        return CompilerResult(result, dts)
    }

    fun generateBinaryAst(files: Iterable<IrFile>, allModules: Iterable<IrModuleFragment>): Map<String, ByteArray> {
        val exportModelGenerator = ExportModelGenerator(backendContext, generateNamespacesForPackages = true)

        val exportData = files.associate { file ->
            file to exportModelGenerator.generateExportWithExternals(file)
        }

        allModules.forEach {
            it.files.forEach {
                StaticMembersLowering(backendContext).lower(it)
            }
        }

        val serializer = JsIrAstSerializer()

        val result = mutableMapOf<String, ByteArray>()
        files.forEach { f ->
            val exports = exportData[f]!! // TODO
            val fragment = generateProgramFragment(f, exports)
            val output = ByteArrayOutputStream()
            serializer.serialize(fragment, output)
            val binaryAst = output.toByteArray()
            result[f.fileEntry.name] = binaryAst
        }

        return result
    }

    private fun ExportModelGenerator.generateExportWithExternals(irFile: IrFile): List<ExportedDeclaration> {
        val exports = generateExport(irFile)
        val additionalExports = backendContext.externalPackageFragment[irFile.symbol]?.let { generateExport(it) } ?: emptyList()
        return additionalExports + exports
    }

    private fun IrModuleFragment.externalModuleName(): String {
        return moduleToName[this] ?: sanitizeName(safeName)
    }

    private fun generateProgramFragments(
        modules: Iterable<IrModuleFragment>,
        exportData: Map<IrModuleFragment, Map<IrFile, List<ExportedDeclaration>>>,
    ): JsIrProgram {

        return JsIrProgram(
            modules.map { m ->
                JsIrModule(m.safeName, m.externalModuleName(), m.files.map { f ->
                    val exports = exportData[m]!![f]!!
                    generateProgramFragment(f, exports)
                })
            }
        )
    }

    private val generateFilePaths = backendContext.configuration.getBoolean(JSConfigurationKeys.GENERATE_COMMENTS_WITH_FILE_PATH)
    private val pathPrefixMap = backendContext.configuration.getMap(JSConfigurationKeys.FILE_PATHS_PREFIX_MAP)

    private fun generateProgramFragment(file: IrFile, exports: List<ExportedDeclaration>): JsIrProgramFragment {
        val nameGenerator = JsNameLinkingNamer(backendContext)

        val globalNameScope = NameTable<IrDeclaration>()

        val staticContext = JsStaticContext(
            backendContext = backendContext,
            irNamer = nameGenerator,
            globalNameScope = globalNameScope
        )

        val result = JsIrProgramFragment(file.fqName.asString())

        val internalModuleName = ReservedJsNames.makeInternalModuleName()
        val globalNames = NameTable<String>(globalNameScope)
        val exportStatements =
            ExportModelToJsStatements(staticContext, { globalNames.declareFreshName(it, it) }).generateModuleExport(
                ExportedModule(mainModuleName, moduleKind, exports),
                internalModuleName,
            )

        result.exports.statements += exportStatements

        if (exports.isNotEmpty()) {
            result.dts = exports.toTypeScript(moduleKind)
        }

        val statements = result.declarations.statements

        val fileStatements = file.accept(IrFileToJsTransformer(useBareParameterNames = true), staticContext).statements
        if (fileStatements.isNotEmpty()) {
            var startComment = ""

            if (generateRegionComments) {
                startComment = "region "
            }

            if (generateRegionComments || generateFilePaths) {
                val originalPath = file.path
                val path = pathPrefixMap.entries
                    .find { (k, _) -> originalPath.startsWith(k) }
                    ?.let { (k, v) -> v + originalPath.substring(k.length) }
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

        staticContext.classModels.entries.forEach { (symbol, model) ->
            result.classes[nameGenerator.getNameForClass(symbol.owner)] =
                JsIrIcClassModel(model.klass.superTypes.map { staticContext.getNameForClass((it.classifierOrFail as IrClassSymbol).owner) }).also {
                    it.preDeclarationBlock.statements += model.preDeclarationBlock.statements
                    it.postDeclarationBlock.statements += model.postDeclarationBlock.statements
                }
        }

        result.initializers.statements += staticContext.initializerBlock.statements

        if (mainArguments != null) {
            JsMainFunctionDetector(backendContext).getMainFunctionOrNull(file)?.let {
                val jsName = staticContext.getNameForStaticFunction(it)
                val generateArgv = it.valueParameters.firstOrNull()?.isStringArrayParameter() ?: false
                val generateContinuation = it.isLoweredSuspendFunction(backendContext)
                result.mainFunction = JsInvocation(jsName.makeRef(), generateMainArguments(generateArgv, generateContinuation, staticContext)).makeStmt()
            }
        }

        backendContext.testFunsPerFile[file]?.let {
            result.testFunInvocation = JsInvocation(staticContext.getNameForStaticFunction(it).makeRef()).makeStmt()
            result.suiteFn = staticContext.getNameForStaticFunction(backendContext.suiteFun!!.owner)
        }

        result.importedModules += nameGenerator.importedModules

        val definitionSet = file.declarations.toSet()

        fun computeTag(declaration: IrDeclaration): String? {
            val tag = (backendContext.irFactory as IdSignatureRetriever).declarationSignature(declaration)?.toString()

            if (tag == null && declaration !in definitionSet) {
                error("signature for ${declaration.render()} not found")
            }

            return tag
        }

        nameGenerator.nameMap.entries.forEach { (declaration, name) ->
            computeTag(declaration)?.let { tag ->
                result.nameBindings[tag] = name
            }
        }

        nameGenerator.imports.entries.forEach { (declaration, importExpression) ->
            val tag = computeTag(declaration) ?: error("No tag for imported declaration ${declaration.render()}")
            result.imports[tag] = importExpression
        }

        file.declarations.forEach {
            computeTag(it)?.let { tag ->
                result.definitions += tag
            }

            if (it is IrClass && it.isInterface) {
                it.declarations.forEach {
                    computeTag(it)?.let { tag ->
                        result.definitions += tag
                    }
                }
            }
        }

        return result
    }

    private fun generateMainArguments(
        generateArgv: Boolean,
        generateContinuation: Boolean,
        staticContext: JsStaticContext,
    ): List<JsExpression> {
        val mainArguments = this.mainArguments!!
        val mainArgumentsArray =
            if (generateArgv) JsArrayLiteral(mainArguments.map { JsStringLiteral(it) }) else null

        val continuation = if (generateContinuation) {
            backendContext.coroutineEmptyContinuation.owner
                .let { it.getter!! }
                .let { staticContext.getNameForStaticFunction(it) }
                .let { JsInvocation(it.makeRef()) }
        } else null

        return listOfNotNull(mainArgumentsArray, continuation)
    }
}

fun generateWrappedModuleBody(
    multiModule: Boolean,
    mainModuleName: String,
    moduleKind: ModuleKind,
    program: JsIrProgram,
    sourceMapsInfo: SourceMapsInfo?,
    relativeRequirePath: Boolean,
    generateScriptModule: Boolean
): CompilationOutputs {
    if (multiModule) {

        val moduleToRef = program.crossModuleDependencies(relativeRequirePath)

        val main = program.mainModule
        val others = program.otherModules

        val mainModule = generateSingleWrappedModuleBody(
            mainModuleName,
            moduleKind,
            main.fragments,
            sourceMapsInfo,
            generateScriptModule,
            generateCallToMain = true,
            moduleToRef[main]!!,
        )

        val dependencies = others.map { module ->
            val moduleName = module.externalModuleName

            moduleName to generateSingleWrappedModuleBody(
                moduleName,
                moduleKind,
                module.fragments,
                sourceMapsInfo,
                generateScriptModule,
                generateCallToMain = false,
                moduleToRef[module]!!,
            )
        }

        return CompilationOutputs(mainModule.jsCode, mainModule.jsProgram, mainModule.sourceMap, dependencies)
    } else {
        return generateSingleWrappedModuleBody(
            mainModuleName,
            moduleKind,
            program.modules.flatMap { it.fragments },
            sourceMapsInfo,
            generateScriptModule,
            generateCallToMain = true,
        )
    }
}

private fun generateSingleWrappedModuleBody(
    moduleName: String,
    moduleKind: ModuleKind,
    fragments: List<JsIrProgramFragment>,
    sourceMapsInfo: SourceMapsInfo?,
    generateScriptModule: Boolean,
    generateCallToMain: Boolean,
    crossModuleReferences: CrossModuleReferences = CrossModuleReferences.Empty
): CompilationOutputs {
    val program = Merger(
        moduleName,
        moduleKind,
        fragments,
        crossModuleReferences,
        generateScriptModule,
        generateRegionComments = true,
        generateCallToMain,
    ).merge()

    program.resolveTemporaryNames()

    val jsCode = TextOutputImpl()

    val sourceMapBuilder: SourceMap3Builder?
    val sourceMapBuilderConsumer: SourceLocationConsumer
    if (sourceMapsInfo != null) {
        val sourceMapPrefix = sourceMapsInfo.sourceMapPrefix
        sourceMapBuilder = SourceMap3Builder(null, jsCode, sourceMapPrefix)

        val sourceRoots = sourceMapsInfo.sourceRoots.map(::File)
        val generateRelativePathsInSourceMap = sourceMapPrefix.isEmpty() && sourceRoots.isEmpty()
        val outputDir = if (generateRelativePathsInSourceMap) sourceMapsInfo.outputDir else null

        val pathResolver = SourceFilePathResolver(sourceRoots, outputDir)

        val sourceMapContentEmbedding =
            sourceMapsInfo.sourceMapContentEmbedding

        sourceMapBuilderConsumer = SourceMapBuilderConsumer(
            File("."),
            sourceMapBuilder,
            pathResolver,
            sourceMapContentEmbedding == SourceMapSourceEmbedding.ALWAYS,
            sourceMapContentEmbedding != SourceMapSourceEmbedding.NEVER
        )
    } else {
        sourceMapBuilder = null
        sourceMapBuilderConsumer = NoOpSourceLocationConsumer
    }

    program.accept(JsToStringGenerationVisitor(jsCode, sourceMapBuilderConsumer))

    return CompilationOutputs(
        jsCode.toString(),
        program,
        sourceMapBuilder?.build()
    )
}

class SourceMapsInfo(
    val sourceMapPrefix: String,
    val sourceRoots: List<String>,
    val outputDir: File?,
    val sourceMapContentEmbedding: SourceMapSourceEmbedding,
) {
    companion object {
        fun from(configuration: CompilerConfiguration) : SourceMapsInfo {
            return SourceMapsInfo(
                configuration.get(JSConfigurationKeys.SOURCE_MAP_PREFIX, ""),
                configuration.get(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, emptyList<String>()),
                configuration.get(JSConfigurationKeys.OUTPUT_DIR),
                configuration.get(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, SourceMapSourceEmbedding.INLINING),
            )
        }
    }
}