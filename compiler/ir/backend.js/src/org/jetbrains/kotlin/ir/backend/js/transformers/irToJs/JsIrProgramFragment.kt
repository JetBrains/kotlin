/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.serialization.cityHash64String
import org.jetbrains.kotlin.ir.backend.js.export.TypeScriptFragment
import org.jetbrains.kotlin.ir.backend.js.ic.IrModule
import org.jetbrains.kotlin.ir.backend.js.ic.IrProgramFragment
import org.jetbrains.kotlin.ir.backend.js.ic.IrProgramFragments
import org.jetbrains.kotlin.ir.backend.js.utils.serialization.serializeTo
import org.jetbrains.kotlin.ir.backend.js.utils.toJsIdentifier
import org.jetbrains.kotlin.js.backend.ast.*
import java.io.File
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.OutputStream

class JsIrProgramFragments(override val mainFragment: JsIrProgramFragment, override val exportFragment: JsIrProgramFragment? = null) :
    IrProgramFragments() {
    override fun serialize(stream: OutputStream) {
        serializeTo(stream)
    }
}

data class JsIrProgramTestEnvironment(
    val testFunctionTag: String,
    val suiteFunctionTag: String
)

class JsIrProgramFragment(val name: String, val packageFqn: String) : IrProgramFragment() {
    val nameBindings = mutableMapOf<String, JsName>()
    val optionalCrossModuleImports = hashSetOf<String>()
    val declarations = JsCompositeBlock()
    val exports = JsCompositeBlock()
    val importedModules = mutableListOf<JsImportedModule>()
    val imports = mutableMapOf<String, JsStatement>()
    var dts: TypeScriptFragment? = null
    val classes = mutableMapOf<JsName, JsIrIcClassModel>()
    val initializers = JsCompositeBlock()
    val eagerInitializers = JsCompositeBlock()
    var mainFunctionTag: String? = null
    val definitions = mutableSetOf<String>()
    val polyfills = JsCompositeBlock()
    var testEnvironment: JsIrProgramTestEnvironment? = null
}

class JsIrModule(
    override val moduleName: String,
    val externalModuleName: String,
    override val fragments: List<JsIrProgramFragment>,
    val reexportedInModuleWithName: String? = null,
    val importedWithEffectInModuleWithName: String? = null,
) : IrModule() {
    fun makeModuleHeader(): JsIrModuleHeader {
        val nameBindings = mutableMapOf<String, String>()
        val definitions = mutableSetOf<String>()
        val optionalCrossModuleImports = hashSetOf<String>()
        var hasDeclarationsToReexport = false
        for (fragment in fragments) {
            hasDeclarationsToReexport = hasDeclarationsToReexport || !fragment.exports.isEmpty
            for ((tag, name) in fragment.nameBindings.entries) {
                nameBindings[tag] = name.toString()
            }
            definitions += fragment.definitions
            optionalCrossModuleImports += fragment.optionalCrossModuleImports
        }
        return JsIrModuleHeader(
            moduleName = moduleName,
            externalModuleName = externalModuleName,
            definitions = definitions,
            nameBindings = nameBindings,
            optionalCrossModuleImports = optionalCrossModuleImports,
            reexportedInModuleWithName = reexportedInModuleWithName.takeIf { hasDeclarationsToReexport },
            importedWithEffectInModuleWithName = importedWithEffectInModuleWithName,
            associatedModule = this
        )
    }
}

class JsIrModuleHeader(
    val moduleName: String,
    val externalModuleName: String,
    val definitions: Set<String>,
    val nameBindings: Map<String, String>,
    val optionalCrossModuleImports: Set<String>,
    val reexportedInModuleWithName: String? = null,
    val importedWithEffectInModuleWithName: String? = null,
    var associatedModule: JsIrModule?,
) {
    val externalNames: Set<String> by lazy(LazyThreadSafetyMode.NONE) { nameBindings.keys - definitions }
}

class JsIrProgram(private var modules: List<JsIrModule>) {
    fun asCrossModuleDependencies(moduleKind: ModuleKind, relativeRequirePath: Boolean): List<Pair<JsIrModule, CrossModuleReferences>> {
        val resolver = CrossModuleDependenciesResolver(moduleKind, modules.map { it.makeModuleHeader() })
        modules = emptyList()
        val crossModuleReferences = resolver.resolveCrossModuleDependencies(relativeRequirePath)
        return crossModuleReferences.entries.map {
            val module = it.key.associatedModule ?: error("Internal error: module ${it.key.moduleName} is not loaded")
            it.value.initJsImportsForModule(module)
            module to it.value
        }
    }

    fun asFragments(): List<JsIrProgramFragment> {
        val fragments = modules.flatMap { it.fragments }
        modules = emptyList()
        return fragments
    }
}

class CrossModuleDependenciesResolver(private val moduleKind: ModuleKind, private val headers: List<JsIrModuleHeader>) {
    fun resolveCrossModuleDependencies(relativeRequirePath: Boolean): Map<JsIrModuleHeader, CrossModuleReferences> {
        val reexportModuleToHeader = headers.groupBy { it.reexportedInModuleWithName }
        val importedInModuleWithEffect = headers.groupBy { it.importedWithEffectInModuleWithName }
        val headerToBuilder = headers.associateWith {
            JsIrModuleCrossModuleReferenceBuilder(
                moduleKind,
                it,
                relativeRequirePath,
                reexportModuleToHeader[it.moduleName] ?: emptyList(),
                importedInModuleWithEffect[it.moduleName] ?: emptyList(),
            )
        }
        val definitionModule = mutableMapOf<String, JsIrModuleCrossModuleReferenceBuilder>()

        for (header in headers) {
            val builder = headerToBuilder[header]!!
            for (definition in header.definitions) {
                require(definition !in definitionModule) { "Duplicate definition: $definition" }
                definitionModule[definition] = builder
            }
        }

        for (header in headers) {
            val builder = headerToBuilder[header]!!
            for (tag in header.externalNames) {
                val fromModuleBuilder = definitionModule[tag]

                if (fromModuleBuilder == null) {
                    if (tag in header.optionalCrossModuleImports) {
                        continue
                    }
                    val name = header.nameBindings[tag] ?: "<unknown name>"
                    error("Internal error: cannot find external signature '$tag' for name '$name' in module ${header.moduleName}")
                }

                builder.imports += CrossModuleRef(fromModuleBuilder, tag)
                fromModuleBuilder.exports += tag
            }
        }

        headerToBuilder.forEach { it.value.buildExportNames() }

        return headers.associateWith { headerToBuilder[it]!!.buildCrossModuleRefs() }
    }
}

private class CrossModuleRef(val module: JsIrModuleCrossModuleReferenceBuilder, val tag: String)

private class JsIrModuleCrossModuleReferenceBuilder(
    val moduleKind: ModuleKind,
    val header: JsIrModuleHeader,
    val relativeRequirePath: Boolean,
    val transitiveExportFrom: List<JsIrModuleHeader>,
    val importWithEffectFrom: List<JsIrModuleHeader>,
) {
    val imports = mutableListOf<CrossModuleRef>()
    val exports = mutableSetOf<String>()

    lateinit var exportNames: Map<String, String> // tag -> index

    fun buildExportNames(startIndex: Int = 0) {
        var index = startIndex
        exportNames = exports.sorted().associateWith { tag ->
            // Bundlers should minimize the names by ourselves. Ex, webpack has `optimization.mangleExports` property
            if (moduleKind == ModuleKind.ES) {
                "${header.nameBindings[tag]}${tag.cityHash64String()}"
            } else {
                index++.toJsIdentifier()
            }
        }
    }

    fun buildCrossModuleRefs(): CrossModuleReferences {
        val isImportOptional = moduleKind == ModuleKind.ES
        val importedModules = mutableMapOf<JsIrModuleHeader, JsImportedModule>()

        fun import(moduleHeader: JsIrModuleHeader): JsImportedModule {
            return if (isImportOptional) {
                moduleHeader.toJsImportedModule()
            } else {
                importedModules.getOrPut(moduleHeader) { moduleHeader.toJsImportedModule() }
            }
        }

        val resultImports = imports.associate { crossModuleRef ->
            val tag = crossModuleRef.tag
            val exportedAs = crossModuleRef.module.exportNames[tag]!!
            val importedModule = import(crossModuleRef.module.header)

            tag to CrossModuleImport(exportedAs, importedModule)
        }

        val transitiveExport = transitiveExportFrom.mapNotNull {
            it.reexportedInModuleWithName?.run {
                CrossModuleTransitiveExport(import(it).internalName, relativeRequirePath(it) ?: it.externalModuleName)
            }
        }

        val importsWithEffect = importWithEffectFrom.map { CrossModuleImportWithEffect(import(it)) }

        return CrossModuleReferences(
            moduleKind,
            importedModules.values.toList(),
            transitiveExport,
            importsWithEffect,
            exportNames,
            resultImports
        )
    }

    private fun JsIrModuleHeader.toJsImportedModule(): JsImportedModule {
        val jsModuleName = JsName(moduleName, false)
        val relativeRequirePath = relativeRequirePath(this)

        return JsImportedModule(
            externalModuleName,
            jsModuleName,
            null,
            relativeRequirePath
        )
    }

    private fun relativeRequirePath(moduleHeader: JsIrModuleHeader): String? {
        if (!this.relativeRequirePath) return null

        val parentMain = File(header.externalModuleName).parentFile ?: return "./${moduleHeader.externalModuleName}"

        val relativePath = File(moduleHeader.externalModuleName)
            .toRelativeString(parentMain)
            .replace(File.separator, "/")

        return relativePath.takeIf { it.startsWith("../") } ?: "./$relativePath"
    }
}


class CrossModuleImport(val exportedAs: String, val moduleExporter: JsImportedModule)
class CrossModuleImportWithEffect(val moduleExporter: JsImportedModule)
class CrossModuleTransitiveExport(val internalName: JsName, val externalName: String)

fun CrossModuleTransitiveExport.getRequireEsmName() = "$externalName$ESM_EXTENSION"

class CrossModuleReferences(
    val moduleKind: ModuleKind,
    val importedModules: List<JsImportedModule>, // additional Kotlin imported modules
    val transitiveExportFrom: List<CrossModuleTransitiveExport>, // the list of modules which provide their exports for transitive export
    val importsWithEffect: List<CrossModuleImportWithEffect>, // the list of modules which provide their effects for import
    val exports: Map<String, String>, // tag -> index
    val imports: Map<String, CrossModuleImport>, // tag -> import statement
) {
    // built from imports
    var jsImports = emptyMap<String, JsStatement>() // tag -> import statement
        private set

    val jsImportsWithEffect: List<JsStatement> = importsWithEffect.map { it.generateCrossModuleImportStatement() }

    fun initJsImportsForModule(module: JsIrModule) {
        val tagToName = module.fragments.flatMap { it.nameBindings.entries }.associate { it.key to it.value }
        jsImports = imports.entries.associate {
            val importedAs = tagToName[it.key] ?: error("Internal error: cannot find imported name for signature ${it.key}")
            it.key to it.value.generateCrossModuleImportStatement(importedAs)
        }
    }

    private fun CrossModuleImport.generateCrossModuleImportStatement(importedAs: JsName): JsStatement {
        return when (moduleKind) {
            ModuleKind.ES -> generateJsImportStatement(importedAs)
            else -> generateImportVariableDeclaration(importedAs)
        }
    }

    private fun CrossModuleImportWithEffect.generateCrossModuleImportStatement(): JsStatement {
        return when (moduleKind) {
            ModuleKind.ES -> generateJsImportStatement()
            else -> error("Should not appear in non ES-modules compilations")
        }
    }

    private fun CrossModuleImportWithEffect.generateJsImportStatement(): JsStatement {
        return JsImport(moduleExporter.getRequireName(true), JsImport.Target.Effect)
    }

    private fun CrossModuleImport.generateImportVariableDeclaration(importedAs: JsName): JsStatement {
        val exportRef = JsNameRef(exportedAs, ReservedJsNames.makeCrossModuleNameRef(moduleExporter.internalName))
        return JsVars(JsVars.JsVar(importedAs, exportRef))
    }

    private fun CrossModuleImport.generateJsImportStatement(importedAs: JsName): JsStatement {
        return JsImport(
            moduleExporter.getRequireName(true),
            JsImport.Element(JsName(exportedAs, false), importedAs.makeRef())
        )
    }

    companion object {
        fun Empty(moduleKind: ModuleKind) = CrossModuleReferences(moduleKind, listOf(), emptyList(), emptyList(), emptyMap(), emptyMap())
    }
}

fun JsStatement.renameImportedSymbolInternalName(newName: JsName): JsStatement {
    return when (this) {
        is JsImport -> JsImport(module, JsImport.Element((target as JsImport.Target.Elements).elements.single().name, newName.makeRef()))
        is JsVars -> JsVars(JsVars.JsVar(newName, vars.single().initExpression))
        else -> error("Unexpected cross-module import statement ${this::class.qualifiedName}")
    }
}

val JsIrProgramFragment.hasEffect: Boolean get() = eagerInitializers.statements.isNotEmpty()