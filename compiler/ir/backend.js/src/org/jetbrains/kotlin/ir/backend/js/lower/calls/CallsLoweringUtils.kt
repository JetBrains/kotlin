/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.SimpleType

typealias SymbolToTransformer = MutableMap<IrFunctionSymbol, (IrCall) -> IrExpression>

internal fun SymbolToTransformer.add(from: Map<SimpleType, IrFunctionSymbol>, to: IrFunctionSymbol) {
    from.forEach { _, func ->
        add(func, to)
    }
}

internal fun SymbolToTransformer.add(from: Map<SimpleType, IrFunctionSymbol>, to: (IrCall) -> IrExpression) {
    from.forEach { _, func ->
        add(func, to)
    }
}

internal fun SymbolToTransformer.add(from: IrFunctionSymbol, to: (IrCall) -> IrExpression) {
    put(from, to)
}

internal fun SymbolToTransformer.add(from: IrFunctionSymbol, to: IrFunctionSymbol, dispatchReceiverAsFirstArgument: Boolean = false) {
    put(from) { call -> irCall(call, to, dispatchReceiverAsFirstArgument) }
}

internal fun <K> MutableMap<K, (IrCall) -> IrExpression>.addWithPredicate(
    from: K,
    predicate: (IrCall) -> Boolean,
    action: (IrCall) -> IrExpression
) {
    put(from) { call: IrCall -> if (predicate(call)) action(call) else call }
}

internal typealias MemberToTransformer = HashMap<SimpleMemberKey, (IrCall) -> IrExpression>

internal fun MemberToTransformer.add(type: IrType, name: Name, v: IrFunctionSymbol) {
    add(type, name) { irCall(it, v, dispatchReceiverAsFirstArgument = true) }
}

internal fun MemberToTransformer.add(type: IrType, name: Name, v: IrFunction) {
    add(type, name, v.symbol)
}

internal fun MemberToTransformer.add(type: IrType, name: Name, v: (IrCall) -> IrExpression) {
    put(SimpleMemberKey(type, name), v)
}

internal data class SimpleMemberKey(val klass: IrType, val name: Name)

enum class PrimitiveType {
    FLOATING_POINT_NUMBER,
    INTEGER_NUMBER,
    STRING,
    BOOLEAN,
    OTHER
}

fun IrType.getPrimitiveType() = makeNotNull().run {
    when {
        isBoolean() -> PrimitiveType.BOOLEAN
        isByte() || isShort() || isInt() -> PrimitiveType.INTEGER_NUMBER
        isFloat() || isDouble() -> PrimitiveType.FLOATING_POINT_NUMBER
        isString() -> PrimitiveType.STRING
        else -> PrimitiveType.OTHER
    }
}
