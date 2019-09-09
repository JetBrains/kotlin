/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ast


open class SExpressionBuilder {
    protected val builder = StringBuilder()

    protected var indent = 0

    protected inline fun indented(body: () -> Unit) {
        indent++
        body()
        indent--
    }

    protected fun newLine() {
        builder.appendln()
        repeat(indent * 2) { builder.append(" ") }
    }

    protected inline fun newLineList(name: String, body: () -> Unit) {
        newLine()
        builder.append("($name")
        indented { body() }
        builder.append(")")
    }

    protected inline fun list(name: String, body: () -> Unit) {
        builder.append(" ($name")
        body()
        builder.append(")")
    }

    protected fun element(value: String) {
        builder.append(" ")
        builder.append(value)
    }

    override fun toString(): String =
        builder.toString()
}

class WatGenerator : SExpressionBuilder() {
    inner class BodyGenerator(
        val function: WasmDefinedFunction?
    ) {
        fun WasmInstruction.generate() {
            newLineList(mnemonic) {
                if (this is WasmBranchTarget) {
                    element("$${label}_${id}")
                }
                immediates.forEach { it.generate() }
                operands.forEach { it.generate() }
                blockBody.forEach { it.generate() }
            }
        }

        fun WasmImmediate.generate() {
            when (this) {
                is WasmImmediate.BlockReference -> element("$${block.label}_${block.id}")

                // SpiderMonkey jsshell won't parse Uppercase letters in literals
                is WasmImmediate.LiteralValue<*> -> element("$value".toLowerCase())
                is WasmImmediate.ResultType -> type?.let { list("result") { type.generate() } }
                is WasmImmediate.ValueType -> type.generate()
                is WasmImmediate.VariableRef -> local.generateReference()
                is WasmImmediate.FunctionSymbol.Linked -> ref.owner.generateReference()
                is WasmImmediate.FunctionTypeSymbol.Linked -> ref.owner.generateReference()
                is WasmImmediate.GlobalSymbol.Linked -> ref.owner.generateReference()
                is WasmImmediate.StructTypeSymbol.Linked -> ref.owner.generateReference()
                is WasmImmediate.StructFieldSymbol.Linked -> element(ref.owner.toString())
                is WasmImmediate.I32Symbol -> element(value.owner.toString())
            }
        }
    }


    private val topLevelBodyGenerator = BodyGenerator(null)

    fun generate(module: WasmModule) {
        with(module) {
            newLineList("module") {
                newLineList("gc_feature_opt_in") { element("3") }
                functionTypes.forEach { it.generate() }
                structTypes.forEach { it.generate() }
                importedFunctions.forEach { it.generate() }
                definedFunctions.forEach { it.generate() }
                table.generate()
                memory.generate()
                globals.forEach { it.generate() }
                exports.forEach { it.generate() }
                start?.generate()
                data.forEach { it.generate() }
            }
        }
    }

    fun WasmFunctionType.generate() {
        newLineList("type") {
            generateReference()
            list("func") {
                list("param") {
                    parameterTypes.forEach { it.generate() }
                }
                resultType?.let {
                    list("result") { it.generate() }
                }
            }
        }
    }

    fun WasmStructType.generate() {
        newLineList("type") {
            generateReference()
            list("struct") {
                fields.forEach {
                    it.generate()
                }
            }
        }
    }

    fun WasmImportedFunction.generate() {
        newLineList("func") {
            generateReference()
            list("import") {
                element(toWasString(importPair.module))
                element(toWasString(importPair.name))
            }
            list("type") { type.generateReference() }
        }
    }

    fun WasmDefinedFunction.generate() {
        newLineList("func") {
            generateReference()
            list("type") { type.generateReference() }
            locals.forEach { it.generate() }
            with(BodyGenerator(this)) {
                instructions.forEach { it.generate() }
            }
        }
    }

    fun WasmTable.generate() {
        newLineList("table") {
            element("funcref")
            newLineList("elem") {
                functions.forEach {
                    newLine()
                    it.generateReference()
                }
            }
        }
    }

    fun WasmMemory.generate() {
        newLineList("memory") {
            element(minSize.toString())
            maxSize?.let { element(maxSize.toString()) }
        }
    }

    fun WasmGlobal.generate() {
        newLineList("global") {
            generateReference()

            if (isMutable)
                list("mut") { type.generate() }
            else
                type.generate()

            with(topLevelBodyGenerator) {
                init?.generate()
            }
        }
    }

    fun WasmExport.generate() {
        newLineList("export") {
            element(toWasString(exportedName))
            list(kind.keyword) {
                function.generateReference()
            }
        }
    }

    fun WasmStart.generate() {
        newLineList("start") {
            ref.generateReference()
        }
    }

    fun WasmData.generate() {
        newLineList("data") {
            list("i32.const") {
                element(offset.toString())
            }
            element(bytes.toWatData()) // TODO: Use builder
        }
    }

    fun WasmLocal.generate() {
        newLineList(if (isParameter) "param" else "local") {
            generateReference()
            type.generate()
        }
    }

    fun WasmValueType.generate() {
        when (this) {
            is WasmSimpleValueType -> element(mnemonic)
            is WasmStructRef -> list("ref") { structType.owner.generateReference() }
        }
    }

    fun WasmStructField.generate() {
        list("field") {
            if (isMutable) {
                list("mut") { type.generate() }
            } else {
                type.generate()
            }
        }
    }

    fun WasmLocal.generateReference() {
        element("$${id}_${sanitizeWatIdentifier(name)}")
    }

    fun WasmNamedModuleField.generateReference() {
        val id = id ?: error("$prefix $name ID is unlinked")
        element("\$${sanitizeWatIdentifier(name)}___${prefix}_$id")
    }
}

fun toWasString(s: String): String {
    // TODO: escape characters according to
    //  https://webassembly.github.io/spec/core/text/values.html#strings
    return "\"" + s + "\""
}

fun Byte.toWatData() = "\\" + this.toUByte().toString(16).padStart(2, '0')
fun ByteArray.toWatData(): String = "\"" + joinToString("") { it.toWatData() } + "\""

fun sanitizeWatIdentifier(ident: String): String {
    if (ident.isEmpty())
        return "_"
    if (ident.all(::isValidWatIdentifier))
        return ident
    return ident.map { if (isValidWatIdentifier(it)) it else "_" }.joinToString("")
}

// https://webassembly.github.io/spec/core/text/values.html#text-id
fun isValidWatIdentifier(c: Char): Boolean =
    c in '0'..'9' || c in 'A'..'Z' || c in 'a'..'z'
            // TODO: SpiderMonkey js shell can't parse some of the
            //  permitted identifiers: '?', '<'
            // || c in "!#$%&′*+-./:<=>?@\\^_`|~"
            || c in "$.@_"
