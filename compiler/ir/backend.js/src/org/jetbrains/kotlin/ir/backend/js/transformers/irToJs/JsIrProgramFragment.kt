/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.js.backend.ast.*

class JsIrProgramFragment(val packageFqn: String) {
    val nameBindings = mutableMapOf<String, JsName>()
    val declarations = JsGlobalBlock()
    val exports = JsGlobalBlock()
    val importedModules = mutableListOf<JsImportedModule>()
    val imports = mutableMapOf<String, JsExpression>()
    var dts: String? = null
    val classes = mutableMapOf<JsName, JsIrIcClassModel>()
    val initializers = JsGlobalBlock()
    var mainFunction: JsStatement? = null
    var testFunInvocation: JsStatement? = null
    var suiteFn: JsName? = null
    val definitions = mutableSetOf<String>()
    lateinit var polyfills: JsPolyfillsVisitor
}

class JsIrModule(val moduleName: String, val externalModuleName: String, val fragments: List<JsIrProgramFragment>)

class JsIrProgram(val modules: List<JsIrModule>) {

    fun crossModuleDependencies(relativeRequirePath: Boolean): Map<JsIrModule, CrossModuleReferences> {
        val moduleToBuilder = modules.associateWith { JsIrModuleCrossModuleReferecenceBuilder(it, relativeRequirePath) }
        val definitionModule = mutableMapOf<String, JsIrModuleCrossModuleReferecenceBuilder>()

        for (module in modules) {
            val moduleBuilder = moduleToBuilder[module]!!
            for (fragment in module.fragments) {
                for (definition in fragment.definitions) {
                    require(definition !in definitionModule) { "Duplicate definition: $definition" }
                    definitionModule[definition] = moduleBuilder
                }
            }
        }

        for (module in modules) {
            val moduleBuilder = moduleToBuilder[module]!!
            for (fragment in module.fragments) {
                for (tag in fragment.nameBindings.keys) {
                    val fromModuleBuilder = definitionModule[tag] ?: continue // TODO error?
                    if (fromModuleBuilder == moduleBuilder) continue

                    moduleBuilder.imports += CrossModuleRef(fromModuleBuilder, tag)
                    fromModuleBuilder.exports += tag
                }
            }
        }

        return modules.associateWith { moduleToBuilder[it]!!.buildCrossModuleRefs() }
    }

}

private class CrossModuleRef(val module: JsIrModuleCrossModuleReferecenceBuilder, val tag: String)


private class JsIrModuleCrossModuleReferecenceBuilder(val module: JsIrModule, val relativeRequirePath: Boolean) {
    val imports = mutableListOf<CrossModuleRef>()
    val exports = mutableSetOf<String>()

    private lateinit var exportNames: Map<String, String> // tag -> name

    private fun buildUniqueNames() {
        val names = module.fragments.flatMap { it.nameBindings.entries }.associate { it.key to sanitizeName(it.value.ident) }
        val nameToCnt = mutableMapOf<String, Int>()

        val result = mutableMapOf<String, String>()

        exports.sorted().forEach { tag ->
            val suggestedName = names[tag] ?: error("Name not found for tag $tag")
            val suffix = nameToCnt[suggestedName]?.let { "_$it" } ?: ""
            nameToCnt[suggestedName] = nameToCnt.getOrDefault(suggestedName, 0) + 1
            result[tag] = suggestedName + suffix
        }

        exportNames = result
    }

    fun buildCrossModuleRefs(): CrossModuleReferences {
        buildUniqueNames()

        val importedModules = mutableMapOf<JsIrModule, JsImportedModule>()

        fun JsIrModule.import(): JsName {
            return importedModules.getOrPut(this) {
                val moduleName = JsName(moduleName, false)
                JsImportedModule(externalModuleName, moduleName, null, relativeRequirePath)
            }.internalName
        }

        val tagToName = module.fragments.flatMap { it.nameBindings.entries }.associate { it.key to it.value }

        val resultImports = imports.associate {
            val tag = it.tag
            require(it.module::exportNames.isInitialized) {
                // This situation appears in case of a dependent module redefine a symbol (function) from their dependency
                "Cross module dependency resolution failed due to symbol '${tag.takeWhile { c -> c != '|' }}' redefinition"
            }
            val exportedAs = it.module.exportNames[tag]!!
            val importedAs = tagToName[tag]!!
            val moduleName = it.module.module.import()

            val importStatement = JsVars.JsVar(importedAs, JsNameRef(exportedAs, JsNameRef("\$crossModule\$", moduleName.makeRef())))

            tag to importStatement
        }

        return CrossModuleReferences(importedModules.values.toList(), resultImports, exportNames)
    }
}

class CrossModuleReferences(
    val importedModules: List<JsImportedModule>, // additional Kotlin imported modules
    val imports: Map<String, JsVars.JsVar>, // tag -> import statement
    val exports: Map<String, String>, // tag -> name
) {
    companion object {
        val Empty = CrossModuleReferences(listOf(), emptyMap(), emptyMap())
    }
}