/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.backend.js.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.CompilerResult
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.eliminateDeadDeclarations
import org.jetbrains.kotlin.ir.backend.js.export.*
import org.jetbrains.kotlin.ir.backend.js.lower.StaticMembersLowering
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.backend.js.utils.serialization.JsIrAstSerializer
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.NoOpSourceLocationConsumer
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.ByteArrayOutputStream

class IrModuleToJsTransformerTmp(
    private val backendContext: JsIrBackendContext,
    private val mainArguments: List<String>?,
    private val generateScriptModule: Boolean = false,
    var namer: NameTables = NameTables(emptyList(), context = backendContext),
    private val fullJs: Boolean = true,
    private val dceJs: Boolean = false,
    private val multiModule: Boolean = false,
    private val relativeRequirePath: Boolean = false,
    private val moduleToName: Map<IrModuleFragment, String> = emptyMap(),
    private val removeUnusedAssociatedObjects: Boolean = true,
) {
    private val generateRegionComments = backendContext.configuration.getBoolean(JSConfigurationKeys.GENERATE_REGION_COMMENTS)

    private val mainModuleName = backendContext.configuration[CommonConfigurationKeys.MODULE_NAME]!!
    private val moduleKind = backendContext.configuration[JSConfigurationKeys.MODULE_KIND]!!

    fun generateModule(modules: Iterable<IrModuleFragment>): CompilerResult {
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

        val jsCode = if (fullJs) generateWrappedModuleBody(
            multiModule,
            mainModuleName,
            moduleKind,
            generateProgramFragments(modules, exportData),
            relativeRequirePath
        ) else null

        val dceJsCode = if (dceJs) {
            eliminateDeadDeclarations(modules, backendContext, removeUnusedAssociatedObjects)

            generateWrappedModuleBody(
                multiModule,
                mainModuleName,
                moduleKind,
                generateProgramFragments(modules, exportData),
                relativeRequirePath
            )
        } else null

        return CompilerResult(jsCode, dceJsCode, dts)
    }

    fun generateBinaryAst(files: Iterable<IrFile>): Map<String, ByteArray> {
        val exportModelGenerator = ExportModelGenerator(backendContext, generateNamespacesForPackages = true)

        val exportData = files.associate { file ->
            file to exportModelGenerator.generateExportWithExternals(file)
        }

        files.forEach { StaticMembersLowering(backendContext).lower(it) }

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

        val staticContext = JsStaticContext(
            backendContext = backendContext,
            irNamer = nameGenerator,
            globalNameScope = namer.globalNames
        )

        val result = JsIrProgramFragment(file.fqName.asString())

        val internalModuleName = JsName("_", false)
        val globalNames = NameTable<String>(namer.globalNames)
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

        val fileStatements = file.accept(IrFileToJsTransformer(), staticContext).statements
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

        fun computeTag(declaration: IrDeclaration): String {
            // TODO proper tags
            return System.identityHashCode(declaration).toString()
        }

        nameGenerator.nameMap.entries.forEach { (declaration, name) ->
            val tag = computeTag(declaration)
            result.nameBindings[tag] = name
        }

        nameGenerator.imports.entries.forEach { (declaration, importExpression) ->
            val tag = computeTag(declaration)
            result.imports[tag] = importExpression
        }

        file.declarations.forEach {
            result.definitions += computeTag(it)
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

private fun generateWrappedModuleBody(
    multiModule: Boolean,
    mainModuleName: String,
    moduleKind: ModuleKind,
    program: JsIrProgram,
    relativeRequirePath: Boolean,
): CompilationOutputs {
    if (multiModule) {

        val moduleToRef = program.crossModuleDependencies(relativeRequirePath)

        val main = program.modules.last()
        val others = program.modules.dropLast(1)

        val mainModule = generateSingleWrappedModuleBody(
            mainModuleName,
            moduleKind,
            main.fragments,
            moduleToRef[main]!!,
            generateCallToMain = true,
        )

        val dependencies = others.map { module ->
            val moduleName = module.moduleName

            moduleName to generateSingleWrappedModuleBody(
                moduleName,
                moduleKind,
                module.fragments,
                moduleToRef[module]!!,
                generateCallToMain = false,
            )
        }

        return CompilationOutputs(mainModule.jsCode, mainModule.jsProgram, mainModule.sourceMap, dependencies)
    } else {
        return generateSingleWrappedModuleBody(
            mainModuleName,
            moduleKind,
            program.modules.flatMap { it.fragments },
            generateCallToMain = true,
        )
    }
}

fun generateSingleWrappedModuleBody(
    moduleName: String,
    moduleKind: ModuleKind,
    fragments: List<JsIrProgramFragment>,
    crossModuleReferences: CrossModuleReferences = CrossModuleReferences.Empty,
    generateCallToMain: Boolean,
): CompilationOutputs {
    val program = Merger(
        moduleName,
        moduleKind,
        fragments,
        crossModuleReferences,
        generateScriptModule = false,
        generateRegionComments = true,
        generateCallToMain,
    ).merge()

    program.resolveTemporaryNames()

    val jsCode = TextOutputImpl()

    program.accept(JsToStringGenerationVisitor(jsCode, NoOpSourceLocationConsumer))

    return CompilationOutputs(
        jsCode.toString(),
        null
    )
}