/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ast

interface WasmExpressionVisitor<out R, in D> {
    fun visitUnary(x: WasmUnary, data: D): R
    fun visitBinary(x: WasmBinary, data: D): R
    fun visitConstant(x: WasmConstant, data: D): R
    fun visitLoad(x: WasmLoad, data: D): R
    fun visitStore(x: WasmStore, data: D): R

    fun visitUnreachable(x: WasmUnreachable, data: D): R
    fun visitNop(x: WasmNop, data: D): R
    fun visitBlock(x: WasmBlock, data: D): R
    fun visitLoop(x: WasmLoop, data: D): R
    fun visitIf(x: WasmIf, data: D): R
    fun visitBr(x: WasmBr, data: D): R
    fun visitBrIf(x: WasmBrIf, data: D): R
    fun visitBrTable(x: WasmBrTable, data: D): R
    fun visitReturn(x: WasmReturn, data: D): R
    fun visitCall(x: WasmCall, data: D): R
    fun visitCallIndirect(x: WasmCallIndirect, data: D): R

    fun visitDrop(x: WasmDrop, data: D): R
    fun visitSelect(x: WasmSelect, data: D): R

    fun visitGetLocal(x: WasmGetLocal, data: D): R
    fun visitSetLocal(x: WasmSetLocal, data: D): R
    fun visitLocalTee(x: WasmLocalTee, data: D): R
    fun visitGetGlobal(x: WasmGetGlobal, data: D): R
    fun visitSetGlobal(x: WasmSetGlobal, data: D): R

    fun visitStructGet(x: WasmStructGet, data: D): R
    fun visitStructNew(x: WasmStructNew, data: D): R
    fun visitStructSet(x: WasmStructSet, data: D): R
    fun visitStructNarrow(x: WasmStructNarrow, data: D): R

    fun visitRefNull(x: WasmRefNull, data: D): R
    fun visitRefIsNull(x: WasmRefIsNull, data: D): R
    fun visitRefEq(x: WasmRefEq, data: D): R
}
