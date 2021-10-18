/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class JsNameLinkingNamer(private val context: JsIrBackendContext) : IrNamerBase() {

    val nameMap = mutableMapOf<IrDeclaration, JsName>()

    private fun IrDeclarationWithName.getName(prefix: String = ""): JsName {
        return nameMap.getOrPut(this) { JsName(sanitizeName(prefix + getJsNameOrKotlinName().asString()), true) }
    }

    val importedModules = mutableListOf<JsImportedModule>()
    val imports = mutableMapOf<IrDeclaration, JsExpression>()

    override fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): JsName {

        if (declaration.isEffectivelyExternal()) {
            val jsModule: String? = declaration.getJsModule()
            val maybeParentFile: IrFile? = declaration.parent as? IrFile
            val fileJsModule: String? = maybeParentFile?.getJsModule()
            val jsQualifier: String? = maybeParentFile?.getJsQualifier()

            when {
                jsModule != null -> {
                    val name = JsName(declaration.getJsNameOrKotlinName().asString(), false)
                    importedModules += JsImportedModule(jsModule, name, name.makeRef())
                    return name
                }

                fileJsModule != null -> {
                    if (declaration !in nameMap) {
                        val moduleName = JsName("\$module\$$jsModule", true)
                        importedModules += JsImportedModule(fileJsModule, moduleName, null)
                        val qualifiedReference =
                            if (jsQualifier == null) moduleName.makeRef() else JsNameRef(jsQualifier, moduleName.makeRef())
                        imports[declaration] = JsNameRef(declaration.getJsNameOrKotlinName().identifier, qualifiedReference)
                        return declaration.getName()
                    }
                }

                else -> {
                    var name = declaration.getJsNameOrKotlinName().identifier
                    if (jsQualifier != null)
                        name = "$jsQualifier.$name"

                    return name.toJsName(temporary = false)
                }
            }
        }

        return declaration.getName()
    }

    override fun getNameForMemberFunction(function: IrSimpleFunction): JsName {
        require(function.dispatchReceiverParameter != null)
        val signature = jsFunctionSignature(function, context)
        return signature.toJsName()
    }

    override fun getNameForMemberField(field: IrField): JsName {
        require(!field.isStatic)
        // TODO this looks funny. Rethink.
        return JsName(field.parentAsClass.fieldData()[field]!!, false)
    }

    private val fieldDataCache = mutableMapOf<IrClass, Map<IrField, String>>()

    private fun IrClass.fieldData(): Map<IrField, String> {
        return fieldDataCache.getOrPut(this) {
            val nameCnt = mutableMapOf<String, Int>()

            val allClasses = DFS.topologicalOrder(listOf(this)) { node ->
                node.superTypes.mapNotNull {
                    it.safeAs<IrSimpleType>()?.classifier.safeAs<IrClassSymbol>()?.owner
                }
            }

            val result = mutableMapOf<IrField, String>()

            allClasses.reversed().forEach {
                it.declarations.forEach {
                    when {
                        it is IrField -> {
                            val safeName = it.safeName()
                            val suffix = nameCnt.getOrDefault(safeName, 0) + 1
                            nameCnt[safeName] = suffix
                            result[it] = safeName + "_$suffix"
                        }
                        it is IrFunction && it.dispatchReceiverParameter != null -> {
                            nameCnt[jsFunctionSignature(it, context)] = 1 // avoid clashes with member functions
                        }
                    }
                }
            }

            return result
        }
    }
}

private fun IrField.safeName(): String {
    return sanitizeName(name.asString()).let {
        if (it.lastOrNull()!!.isDigit()) it + "_" else it // Avoid name clashes
    }
}