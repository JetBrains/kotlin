/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.toJsIdentifier
import org.jetbrains.kotlin.js.backend.ast.*
import java.io.File
import org.jetbrains.kotlin.serialization.js.ModuleKind

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

class JsIrModule(
    val moduleName: String,
    val externalModuleName: String,
    val fragments: List<JsIrProgramFragment>
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

class CrossModuleDependenciesResolver(
    private val moduleKind: ModuleKind,
    private val headers: List<JsIrModuleHeader>
) {
    fun resolveCrossModuleDependencies(relativeRequirePath: Boolean): Map<JsIrModuleHeader, CrossModuleReferences> {
        val headerToBuilder = headers.associateWith { JsIrModuleCrossModuleReferecenceBuilder(moduleKind, it, relativeRequirePath) }
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

        return headers.associateWith { headerToBuilder[it]!!.buildCrossModuleRefs() }
    }
}

private fun String.prettyTag() = takeWhile { c -> c != '|' }

private class CrossModuleRef(val module: JsIrModuleCrossModuleReferecenceBuilder, val tag: String)

private class JsIrModuleCrossModuleReferecenceBuilder(
    val moduleKind: ModuleKind,
    val header: JsIrModuleHeader,
    val relativeRequirePath: Boolean
) {
    val imports = mutableListOf<CrossModuleRef>()
    val exports = mutableSetOf<String>()
    var transitiveJsExportFrom = emptyList<JsIrModuleHeader>()

    private lateinit var exportNames: Map<String, String> // tag -> index

    private fun buildExportNames() {
        var index = 0
        exportNames = exports.sorted().associateWith { index++.toJsIdentifier() }
    }

    fun buildCrossModuleRefs(): CrossModuleReferences {
        buildExportNames()
        val importedModules = mutableMapOf<JsIrModuleHeader, JsImportedModule>()

        fun import(moduleHeader: JsIrModuleHeader): JsName {
            return importedModules.getOrPut(moduleHeader) {
                val jsModuleName = JsName(moduleHeader.moduleName, false)
                val relativeRequirePath = relativeRequirePath(moduleHeader)

                JsImportedModule(
                    moduleHeader.externalModuleName,
                    jsModuleName,
                    null,
                    relativeRequirePath
                )
            }.internalName
        }

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
        return CrossModuleReferences(
            moduleKind,
            importedModules.values.toList(),
            transitiveExport,
            exportNames,
            resultImports
        )
    }

    private fun relativeRequirePath(moduleHeader: JsIrModuleHeader): String? {
        if (!this.relativeRequirePath) return null

        val parentMain = File(header.externalModuleName).parentFile

        if (parentMain == null) return "./${moduleHeader.externalModuleName}"

        val relativePath = File(moduleHeader.externalModuleName)
            .toRelativeString(parentMain)
            .replace(File.separator, "/")

        return relativePath.takeIf { it.startsWith("../") }
            ?: "./$relativePath"
    }
}

class CrossModuleImport(val exportedAs: String, val moduleExporter: JsName)

class CrossModuleReferences(
    val moduleKind: ModuleKind,
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
            val exportRef = JsNameRef(
                it.value.exportedAs,
                it.value.moduleExporter.let {
                    if (moduleKind == ModuleKind.ES) {
                        it.makeRef()
                    } else {
                        ReservedJsNames.makeCrossModuleNameRef(it)
                    }
                }
            )
            it.key to JsVars.JsVar(importedAs, exportRef)
        }
    }

    companion object {
        fun Empty(moduleKind: ModuleKind) = CrossModuleReferences(moduleKind, listOf(), emptyList(), emptyMap(), emptyMap())
    }
}
