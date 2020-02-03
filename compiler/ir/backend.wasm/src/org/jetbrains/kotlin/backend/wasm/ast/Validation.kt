/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ast

fun isSubtypeOfStructRef(type: WasmValueType?) =
    type is WasmStructRef || type is WasmNullRefType

fun isSubtypeOfAnyRef(type: WasmValueType?) =
    isSubtypeOfStructRef(type) || type is WasmAnyRef

fun assertSubType(subtype: WasmValueType?, type: WasmValueType?) {
    if (subtype is WasmUnreachableType)
        return
    if (type is WasmStructRef) {
        val fields = type.structType.owner.fields
        check(isSubtypeOfStructRef(subtype)) {
            "subtype is not struct type $subtype"
        }

        if (subtype is WasmStructRef) {
            val subFields = subtype.structType.owner.fields
            fields.forEachIndexed { index, wasmStructField ->
                check(subFields[index].type == wasmStructField.type) {
                    "Struct types differ at index $index"
                }
            }
        }
        return
    }
    if (type is WasmAnyRef) {
        check(isSubtypeOfAnyRef(subtype)) {
            "Type $subtype is not a subtype of anyref"
        }
        return
    }
    if (type != subtype)
        error("$subtype is not subtype of $type")
}

class ExpressionValidator(val function: WasmFunction?) : WasmExpressionVisitor<Unit, Nothing?> {
    override fun visitUnary(x: WasmUnary, data: Nothing?) {
        assertSubType(x.operand.type, x.operator.operandType)
    }

    override fun visitBinary(x: WasmBinary, data: Nothing?) {
        assertSubType(x.lhs.type, x.operator.lhsType)
        assertSubType(x.rhs.type, x.operator.rhsType)
    }

    override fun visitConstant(x: WasmConstant, data: Nothing?) {
    }

    override fun visitLoad(x: WasmLoad, data: Nothing?) {
        assertSubType(x.address.type, WasmI32)
    }

    override fun visitStore(x: WasmStore, data: Nothing?) {
        assertSubType(x.address.type, WasmI32)
        assertSubType(x.value.type, x.operator.storedValueType)
    }

    override fun visitUnreachable(x: WasmUnreachable, data: Nothing?) {
    }

    override fun visitNop(x: WasmNop, data: Nothing?) {
    }

    override fun visitBlock(x: WasmBlock, data: Nothing?) {
        assertSubType(x.instructions.lastOrNull()?.type, x.type)
    }

    override fun visitLoop(x: WasmLoop, data: Nothing?) {
        assertSubType(x.instructions.lastOrNull()?.type, x.type)
    }

    override fun visitIf(x: WasmIf, data: Nothing?) {
        assertSubType(x.thenBlock.lastOrNull()?.type, x.type)
        assertSubType(x.elseBlock.lastOrNull()?.type, x.type)
    }

    override fun visitBr(x: WasmBr, data: Nothing?) {
    }

    override fun visitBrIf(x: WasmBrIf, data: Nothing?) {
    }

    override fun visitBrTable(x: WasmBrTable, data: Nothing?) {
    }

    override fun visitReturn(x: WasmReturn, data: Nothing?) {
        assertSubType(x.value?.type, function!!.type.resultType)
    }

    fun checkArguments(arguments: List<WasmInstruction>, parameters: List<WasmValueType>) {
        check(arguments.size == parameters.size) {
            "Arguments size: ${arguments.size} differs from parameters size ${parameters.size}"
        }
        for ((index, arg) in arguments.withIndex()) {
            assertSubType(arg.type, parameters[index])
        }
    }

    override fun visitCall(x: WasmCall, data: Nothing?) {
        checkArguments(x.arguments, x.symbol.owner.type.parameterTypes)
    }

    override fun visitCallIndirect(x: WasmCallIndirect, data: Nothing?) {
        checkArguments(x.arguments, x.symbol.owner.parameterTypes)
    }

    override fun visitDrop(x: WasmDrop, data: Nothing?) {
        check(x.value.type != null)
    }

    override fun visitSelect(x: WasmSelect, data: Nothing?) {
        assertSubType(x.condition.type, WasmI32)
        check(x.operand1.type != null)
        check(x.operand1.type == x.operand2.type)
    }

    override fun visitGetLocal(x: WasmGetLocal, data: Nothing?) {
    }

    override fun visitSetLocal(x: WasmSetLocal, data: Nothing?) {
        assertSubType(x.value.type, x.local.type)
    }

    override fun visitLocalTee(x: WasmLocalTee, data: Nothing?) {
        assertSubType(x.value.type, x.local.type)
    }

    override fun visitGetGlobal(x: WasmGetGlobal, data: Nothing?) {
    }

    override fun visitSetGlobal(x: WasmSetGlobal, data: Nothing?) {
        assertSubType(x.value.type, x.global.owner.type)
    }

    override fun visitStructGet(x: WasmStructGet, data: Nothing?) {
        val structRefType = x.structRef.type
        check(structRefType is WasmStructRef) {
            "ZZ"
        }
        check(structRefType.structType.owner == x.structName.owner)
    }

    override fun visitStructNew(x: WasmStructNew, data: Nothing?) {
        checkArguments(x.operands, x.structName.owner.fields.map { it.type })
    }

    override fun visitStructSet(x: WasmStructSet, data: Nothing?) {
        val field = x.structName.owner.fields[x.fieldId.owner]
        check(field.isMutable)
        assertSubType(x.value.type, field.type)
    }

    override fun visitStructNarrow(x: WasmStructNarrow, data: Nothing?) {
        assertSubType(x.type, x.fromType)
    }

    override fun visitRefNull(x: WasmRefNull, data: Nothing?) {
    }

    override fun visitRefIsNull(x: WasmRefIsNull, data: Nothing?) {
        assertSubType(x.value.type, WasmAnyRef)
    }

    override fun visitRefEq(x: WasmRefEq, data: Nothing?) {
        assertSubType(x.lhs.type, WasmAnyRef)
        assertSubType(x.rhs.type, WasmAnyRef)
    }
}