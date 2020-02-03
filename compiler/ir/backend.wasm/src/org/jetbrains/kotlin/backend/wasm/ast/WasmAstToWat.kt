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

    protected fun newComment(message: String) {
        newLine()
        builder.append("(;")
        builder.append(message)
        builder.append(";) ")
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
        val validator = ExpressionValidator(function)
        fun WasmMemoryArgument.generate() {
            if (offset != 0)
                element("offset=$offset")
            if (align != 0)
                element("align=$align")
        }

        fun WasmBranchTarget.generateArgument() {
            element("$${label}_${id}")
        }

        fun generate(wasmInstruction: WasmInstruction) {
            try {
                wasmInstruction.accept(validator, null)
            } catch (e: Throwable) {
                println("VALIDATION_FAIL: $e")
                newComment("VALIDATION_FAIL: ${e.message}")
            }

            val mnemonic = wasmInstruction.operator.mnemonic
            newLineList(mnemonic) {
                wasmInstruction.accept(wasmExpressionArgumentsGenerator, null)
            }
        }

        val wasmExpressionArgumentsGenerator = object : WasmExpressionVisitor<Unit, Nothing?> {
            override fun visitUnary(x: WasmUnary, data: Nothing?) {
                generate(x.operand)
            }

            override fun visitBinary(x: WasmBinary, data: Nothing?) {
                generate(x.lhs)
                generate(x.rhs)
            }

            override fun visitConstant(x: WasmConstant, data: Nothing?) {
                element(
                    when (x) {
                        is WasmConstant.I32 -> x.value.toString()
                        is WasmConstant.I64 -> x.value.toString()
                        is WasmConstant.F32 -> x.value.toString()
                        is WasmConstant.F64 -> x.value.toString()
                        is WasmConstant.I32Symbol -> x.value.owner.toString()
                    }.toLowerCase()
                )
            }

            override fun visitLoad(x: WasmLoad, data: Nothing?) {
                x.memoryArgument.generate()
                generate(x.address)
            }

            override fun visitStore(x: WasmStore, data: Nothing?) {
                x.memoryArgument.generate()
                generate(x.address)
                generate(x.value)
            }

            override fun visitUnreachable(x: WasmUnreachable, data: Nothing?) {
            }

            override fun visitNop(x: WasmNop, data: Nothing?) {
            }

            fun generateBlockResultType(t: WasmValueType?) = when (t) {
                null, WasmUnreachableType -> {}
                else -> list("result") { t.generate() }
            }

            override fun visitBlock(x: WasmBlock, data: Nothing?) {
                x.generateArgument()
                generateBlockResultType(x.type)
                x.instructions.forEach { generate(it) }
            }

            override fun visitLoop(x: WasmLoop, data: Nothing?) {
                x.generateArgument()
                generateBlockResultType(x.type)
                x.instructions.forEach { generate(it) }
            }

            override fun visitIf(x: WasmIf, data: Nothing?) {
                generateBlockResultType(x.type)
                generate(x.condition)
                newLineList("then") {
                    x.thenBlock.forEach { generate(it) }
                }
                if (x.elseBlock.isNotEmpty()) {
                    newLineList("else") {
                        x.elseBlock.forEach { generate(it) }
                    }
                }
            }

            override fun visitBr(x: WasmBr, data: Nothing?) {
                x.target.generateArgument()
            }

            override fun visitBrIf(x: WasmBrIf, data: Nothing?) {
                x.target.generateArgument()
                generate(x.condition)
            }

            override fun visitBrTable(x: WasmBrTable, data: Nothing?) {
                TODO("not implemented")
            }

            override fun visitReturn(x: WasmReturn, data: Nothing?) {
                x.value?.let { generate(it) }
            }

            override fun visitCall(x: WasmCall, data: Nothing?) {
                x.symbol.owner.generateReference()
                x.arguments.forEach { generate(it) }
            }

            override fun visitCallIndirect(x: WasmCallIndirect, data: Nothing?) {
                x.symbol.owner.generateReference()
                x.arguments.forEach { generate(it) }
                generate(x.functionIdx)
            }

            override fun visitDrop(x: WasmDrop, data: Nothing?) {
                generate(x.value)
            }

            override fun visitSelect(x: WasmSelect, data: Nothing?) {
                generate(x.operand1)
                generate(x.operand2)
                generate(x.condition)
            }

            override fun visitGetLocal(x: WasmGetLocal, data: Nothing?) {
                x.local.generateReference()
            }

            override fun visitSetLocal(x: WasmSetLocal, data: Nothing?) {
                x.local.generateReference()
                generate(x.value)
            }

            override fun visitLocalTee(x: WasmLocalTee, data: Nothing?) {
                x.local.generateReference()
                generate(x.value)
            }

            override fun visitGetGlobal(x: WasmGetGlobal, data: Nothing?) {
                x.global.owner.generateReference()
            }

            override fun visitSetGlobal(x: WasmSetGlobal, data: Nothing?) {
                x.global.owner.generateReference()
                generate(x.value)
            }

            override fun visitStructGet(x: WasmStructGet, data: Nothing?) {
                x.structName.owner.generateReference()
                element(x.fieldId.owner.toString())
                generate(x.structRef)
            }

            override fun visitStructNew(x: WasmStructNew, data: Nothing?) {
                x.structName.owner.generateReference()
                x.operands.forEach { generate(it) }
            }

            override fun visitStructSet(x: WasmStructSet, data: Nothing?) {
                x.structName.owner.generateReference()
                element(x.fieldId.owner.toString())
                generate(x.structRef)
                generate(x.value)
            }

            override fun visitStructNarrow(x: WasmStructNarrow, data: Nothing?) {
                x.fromType.generate()
                x.type.generate()
                generate(x.value)
            }

            override fun visitRefNull(x: WasmRefNull, data: Nothing?) {
            }

            override fun visitRefIsNull(x: WasmRefIsNull, data: Nothing?) {
                generate(x.value)
            }

            override fun visitRefEq(x: WasmRefEq, data: Nothing?) {
                generate(x.lhs)
                generate(x.rhs)
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
                instructions.forEach { generate(it) }
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

            init?.let { topLevelBodyGenerator.generate(it) }
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
