/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ast

import org.jetbrains.kotlin.backend.wasm.codegen.sanitizeWatIdentifier

fun WasmLocal.toWatName(idx: Int) =
    "$${idx}_${sanitizeWatIdentifier(name)}"

fun WasmNamedModuleField.watName(): String {
    val id = id ?: error("$prefix $name ID is unlinked")
    return "\$${sanitizeWatIdentifier(name)}___${prefix}_$id"
}

class WatGenerator(val wasmModule: WasmModule) {
    val builder = StringBuilder()

    inner class BodyGenerator(
        val function: WasmDefinedFunction?
    ) {
        fun WasmDefinedFunction.localToWat(idx: Int): String {
            // TODO: Reference parameters directly.
            //  Provide indices.
            if (idx < type.parameterTypes.size) {
                // TODO: Parameter names
                return "\$__$idx"
            }
            val local: WasmLocal = locals[idx - type.parameterTypes.size]
            return local.toWatName(idx)
        }

        fun WasmInstruction.toWat() {
            newLineNode(mnemonic) {
                immediates.forEach { element(it.toWat()) }
                operands.forEach { it.toWat() }
            }
        }

        fun WasmImmediate.toWat(): String = when (this) {
            is WasmImmediate.DeclarationReference -> "$$name"
            // SpiderMonkey jsshell won't parse Uppercase letters in literals
            is WasmImmediate.LiteralValue<*> -> "$value".toLowerCase()
            is WasmImmediate.ResultType -> type?.let { "(result ${type.toWatString()})" } ?: ""
            is WasmImmediate.ValueType -> type.toWatString()
            is WasmImmediate.VariableRef -> function!!.localToWat(id)
            is WasmImmediate.FunctionSymbol.Linked -> ref.owner.watName()
            is WasmImmediate.FunctionTypeSymbol.Linked -> ref.owner.watName()
            is WasmImmediate.GlobalSymbol.Linked -> ref.owner.watName()
            is WasmImmediate.StructTypeSymbol.Linked -> ref.owner.watName()
            is WasmImmediate.StructFieldSymbol.Linked -> ref.owner.toString()
            is WasmImmediate.I32Symbol -> value.owner.toString()
        }
    }

    var indent = 0

    private inline fun indented(body: () -> Unit) {
        indent++
        body()
        indent--
    }

    private fun newLine() {
        builder.appendln()
        repeat(indent * 2) { builder.append(" ") }
    }

    private inline fun newLineNode(name: String, body: () -> Unit) {
        newLine()
        builder.append("($name")
        indented { body() }
        builder.append(")")
    }

    private inline fun node(name: String, body: () -> Unit) {
        builder.append(" ($name")
        body()
        builder.append(")")
    }

    private inline fun element(body: () -> Unit) {
        builder.append(" ")
        body()
    }

    private fun element(value: String) {
        builder.append(" ")
        builder.append(value)
    }

    val topLevelBodyGenerator = BodyGenerator(null)

    fun WasmModule.wat() {
        newLineNode("module") {
            newLineNode("gc_feature_opt_in") { element("3") }
            functionTypes.forEach { it.wat() }
            structTypes.forEach { it.wat() }
            importedFunctions.forEach { it.wat() }
            definedFunctions.forEach { it.wat() }
            table.wat()
            memory.wat()
            globals.forEach { it.wat() }
            exports.forEach { it.wat() }
            start?.wat()
            data.forEach { it.wat() }
        }
    }

    fun WasmFunctionType.wat() {
        newLineNode("type") {
            element(watName())
            node("func") {
                node("param") {
                    parameterTypes.forEach { element(it.toWatString()) }
                }
                resultType?.let {
                    node("result") { element(it.toWatString()) }
                }
            }
        }
    }

    fun WasmStructType.wat() {
        newLineNode("type") {
            element(watName())
            node("struct") {
                fields.forEach {
                    element { it.wat() }
                }
            }
        }
    }

    fun WasmImportedFunction.wat() {
        newLineNode("func") {
            element(watName())
            node("import") {
                element(toWasString(importPair.module))
                element(toWasString(importPair.name))
            }
            node("type") { element(type.watName()) }
        }
    }

    fun WasmDefinedFunction.wat() {
        newLineNode("func") {
            element(watName())
            node("type") { element(type.watName()) }
            val parametersSize = type.parameterTypes.size
            type.parameterTypes.forEachIndexed { index, parType: WasmValueType ->
                node("param") {
                    element("\$__$index")
                    element(parType.toWatString())
                }
            }
            locals.forEachIndexed { index, local ->
                element { local.wat(parametersSize + index) }
            }
            with(BodyGenerator(this)) {
                instructions.forEach {
                    it.toWat()
                }
            }
        }
    }

    fun WasmTable.wat() {
        newLineNode("table") {
            element("funcref")
            newLineNode("elem") {
                functions.forEach {
                    element(it.watName())
                }
            }
        }
    }

    fun WasmMemory.wat() {
        newLineNode("memory") {
            element { builder.append(minSize) }
            maxSize?.let { element { builder.append(it) } }
        }
    }

    fun WasmGlobal.wat() {
        newLineNode("global") {
            element(watName())
            if (isMutable) node("mut") { element(type.toWatString()) }
            else element(type.toWatString())
            with(topLevelBodyGenerator) {
                init?.let { it.toWat() }
            }
        }
    }

    fun WasmExport.wat() {
        newLineNode("export") {
            element(toWasString(exportedName))
            node(kind.keyword) {
                element(function.watName())
            }
        }
    }

    fun WasmStart.wat() {
        newLineNode("start") {
            element(ref.watName())
        }
    }

    fun WasmData.wat() {
        newLineNode("data") {
            node("i32.const") {
                element(offset.toString())
            }
            element(bytes.toWatData()) // TODO: Use builder
        }
    }

    fun WasmLocal.wat(index: Int) {
        newLineNode("local") {
            element(toWatName(index))
            element(type.toWatString())
        }
    }

    fun WasmValueType.toWatString(): String = when (this) {
        is WasmSimpleValueType -> mnemonic
        is WasmStructRef -> ("(ref ${structType.owner.watName()})")
    }

    fun WasmStructField.wat() {
        node("field") {
            if (isMutable) {
                element {
                    node("mut") { element(type.toWatString()) }
                }
            } else {
                element(type.toWatString())
            }
        }
    }
}

fun toWasString(s: String): String {
    // TODO: escape characters according to
    //  https://webassembly.github.io/spec/core/text/values.html#strings
    return "\"" + s + "\""
}

fun Byte.toWatData() = "\\" + this.toUByte().toString(16).padStart(2, '0')
fun ByteArray.toWatData(): String = "\"" + joinToString("") { it.toWatData() } + "\""
