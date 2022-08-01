/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.extensions.IrToJsExtensionKey
import org.jetbrains.kotlin.ir.backend.js.utils.toJsIdentifier
import org.jetbrains.kotlin.js.backend.ast.*

class JsIrProgramFragment(val packageFqn: String) {
    val nameBindings = mutableMapOf<String, JsName>()
    val declarations = JsCompositeBlock()
    val exports = JsCompositeBlock()
    val importedModules = mutableListOf<JsImportedModule>()
    val imports = mutableMapOf<String, JsExpression>()
    var dts: String? = null
    val classes = mutableMapOf<JsName, JsIrIcClassModel>()
    val initializers = JsCompositeBlock()
    var mainFunction: JsStatement? = null
    var testFunInvocation: JsStatement? = null
    var suiteFn: JsName? = null
    val definitions = mutableSetOf<String>()
    val polyfills = JsCompositeBlock()
}

sealed class JsModuleOrigin {
    object Source : JsModuleOrigin()
    class Extension(val extensionKey: IrToJsExtensionKey) : JsModuleOrigin()
}

class JsIrModule(
    val moduleName: String,
    val externalModuleName: String,
    val fragments: List<JsIrProgramFragment>,
    var origin: JsModuleOrigin = JsModuleOrigin.Source
) {
    fun makeModuleHeader(): JsIrModuleHeader {
        val nameBindings = mutableMapOf<String, String>()
        val definitions = mutableSetOf<String>()
        var hasJsExports = false
        for (fragment in fragments) {
            hasJsExports = hasJsExports || !fragment.exports.isEmpty
            for ((tag, name) in fragment.nameBindings.entries) {
                nameBindings[tag] = name.toString()
            }
            definitions += fragment.definitions
        }
        return JsIrModuleHeader(moduleName, externalModuleName, definitions, nameBindings, hasJsExports, this)
    }
}

class JsIrModuleHeader(
    val moduleName: String,
    val externalModuleName: String,
    val definitions: Set<String>,
    val nameBindings: Map<String, String>,
    val hasJsExports: Boolean,
    var associatedModule: JsIrModule?
) {
    val externalNames: Set<String> by lazy { nameBindings.keys - definitions }
}

class JsIrProgram(val modules: List<JsIrModule>) {
    val mainModule = modules.last()
    val otherModules = modules.dropLast(1)

    fun crossModuleDependencies(relativeRequirePath: Boolean): Map<JsIrModule, CrossModuleReferences> {
        val resolver = CrossModuleDependenciesResolver(modules.map { it.makeModuleHeader() })
        val crossModuleReferences = resolver.resolveCrossModuleDependencies(relativeRequirePath)
        return crossModuleReferences.entries.associate {
            val module = it.key.associatedModule ?: error("Internal error: module ${it.key.moduleName} is not loaded")
            it.value.initJsImportsForModule(module)
            module to it.value
        }
    }
}

class CrossModuleDependenciesResolver(private val headers: List<JsIrModuleHeader>) {
    fun resolveCrossModuleDependencies(relativeRequirePath: Boolean): Map<JsIrModuleHeader, CrossModuleReferences> {
        val headerToBuilder = headers.associateWith { JsIrModuleCrossModuleReferecenceBuilder(it, relativeRequirePath) }
        val definitionModule = mutableMapOf<String, JsIrModuleCrossModuleReferecenceBuilder>()

        val mainModuleHeader = headers.last()
        val otherModuleHeaders = headers.dropLast(1)
        headerToBuilder[mainModuleHeader]!!.transitiveJsExportFrom = otherModuleHeaders

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
                val fromModuleBuilder = definitionModule[tag] ?: continue // TODO error?

                builder.imports += CrossModuleRef(fromModuleBuilder, tag)
                fromModuleBuilder.exports += tag
            }
        }

        val references = mutableMapOf<JsIrModuleHeader, CrossModuleReferences>()
        val (generatedByExtensionsHeaders, sourceHeaders) = headers.partition { it.associatedModule?.origin is JsModuleOrigin.Extension }

        sourceHeaders.associateWithTo(references) { headerToBuilder[it]!!.buildCrossModuleRefs() }
        // Generated modules can affect module order, so we separate them explicitly
        generatedByExtensionsHeaders.associateWithTo(references) { headerToBuilder[it]!!.buildCrossModuleRefs(sourceHeaders) }

        return references
    }
}

private fun String.prettyTag() = takeWhile { c -> c != '|' }

private class CrossModuleRef(val module: JsIrModuleCrossModuleReferecenceBuilder, val tag: String)

private class JsIrModuleCrossModuleReferecenceBuilder(val header: JsIrModuleHeader, val relativeRequirePath: Boolean) {
    val imports = mutableListOf<CrossModuleRef>()
    val exports = mutableSetOf<String>()
    var transitiveJsExportFrom = emptyList<JsIrModuleHeader>()

    private lateinit var exportNames: Map<String, String> // tag -> index

    private fun buildExportNames() {
        var index = 0
        exportNames = exports.sorted().associateWith { index++.toJsIdentifier() }
    }

    fun buildCrossModuleRefs(additionalImports: List<JsIrModuleHeader> = emptyList()): CrossModuleReferences {
        buildExportNames()
        val importedModules = mutableMapOf<JsIrModuleHeader, JsImportedModule>()

        fun import(moduleHeader: JsIrModuleHeader): JsName {
            return importedModules.getOrPut(moduleHeader) {
                val jsModuleName = JsName(moduleHeader.moduleName, false)
                JsImportedModule(moduleHeader.externalModuleName, jsModuleName, null, relativeRequirePath)
            }.internalName
        }

        additionalImports.forEach { import(it) }

        val resultImports = imports.associate { crossModuleRef ->
            val tag = crossModuleRef.tag
            require(crossModuleRef.module::exportNames.isInitialized) {
                // This situation appears in case of a dependent module redefine a symbol (function) from their dependency
                "Cross module dependency resolution failed due to symbol '${tag.prettyTag()}' redefinition"
            }
            val exportedAs = crossModuleRef.module.exportNames[tag]!!
            val moduleName = import(crossModuleRef.module.header)

            tag to CrossModuleImport(exportedAs, moduleName)
        }

        val transitiveExport = transitiveJsExportFrom.mapNotNull {
            if (it.hasJsExports) import(it) else null
        }
        return CrossModuleReferences(importedModules.values.toList(), transitiveExport, exportNames, resultImports)
    }
}

class CrossModuleImport(val exportedAs: String, val moduleExporter: JsName)

class CrossModuleReferences(
    val importedModules: List<JsImportedModule>, // additional Kotlin imported modules
    val transitiveJsExportFrom: List<JsName>, // the list of modules which provide their js exports for transitive export
    val exports: Map<String, String>, // tag -> index
    val imports: Map<String, CrossModuleImport>, // tag -> import statement
) {
    // built from imports
    var jsImports = emptyMap<String, JsVars.JsVar>() // tag -> import statement
        private set

    fun initJsImportsForModule(module: JsIrModule) {
        val tagToName = module.fragments.flatMap { it.nameBindings.entries }.associate { it.key to it.value }
        jsImports = imports.entries.associate {
            val importedAs = tagToName[it.key] ?: error("Internal error: cannot find imported name for symbol ${it.key.prettyTag()}")
            val exportRef = JsNameRef(it.value.exportedAs, ReservedJsNames.makeCrossModuleNameRef(it.value.moduleExporter))
            it.key to JsVars.JsVar(importedAs, exportRef)
        }
    }

    companion object {
        val Empty = CrossModuleReferences(listOf(), emptyList(), emptyMap(), emptyMap())
    }
}
