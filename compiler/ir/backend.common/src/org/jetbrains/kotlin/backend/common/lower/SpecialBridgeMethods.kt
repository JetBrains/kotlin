/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class SpecialBridgeMethods(val context: CommonBackendContext) {
    private data class SpecialMethodDescription(val kotlinFqClassName: FqName?, val name: Name, val arity: Int)

    private fun makeDescription(classFqName: String, funName: String, arity: Int) =
        SpecialMethodDescription(
            FqName(classFqName),
            Name.identifier(funName),
            arity
        )

    private fun IrSimpleFunction.toDescription() = SpecialMethodDescription(
        parentAsClass.fqNameWhenAvailable,
        name,
        valueParameters.size
    )

    @Suppress("UNUSED_PARAMETER")
    private fun constFalse(bridge: IrSimpleFunction) =
        IrConstImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.booleanType, IrConstKind.Boolean, false)

    @Suppress("UNUSED_PARAMETER")
    private fun constNull(bridge: IrSimpleFunction) =
        IrConstImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.anyNType, IrConstKind.Null, null)

    @Suppress("UNUSED_PARAMETER")
    private fun constMinusOne(bridge: IrSimpleFunction) =
        IrConstImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.intType, IrConstKind.Int, -1)

    private fun getSecondArg(bridge: IrSimpleFunction) =
        IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, bridge.valueParameters[1].symbol)

    private val SPECIAL_METHODS_WITH_DEFAULTS_MAP = mapOf<SpecialMethodDescription, (IrSimpleFunction) -> IrExpression>(
        makeDescription("kotlin.collections.Collection", "contains", 1) to ::constFalse,
        makeDescription("kotlin.collections.MutableCollection", "remove", 1) to ::constFalse,
        makeDescription("kotlin.collections.Map", "containsKey", 1) to ::constFalse,
        makeDescription("kotlin.collections.Map", "containsValue", 1) to ::constFalse,
        makeDescription("kotlin.collections.MutableMap", "remove", 2) to ::constFalse,
        makeDescription("kotlin.collections.Map", "getOrDefault", 1) to ::getSecondArg,
        makeDescription("kotlin.collections.Map", "get", 1) to ::constNull,
        makeDescription("kotlin.collections.MutableMap", "remove", 1) to ::constNull,
        makeDescription("kotlin.collections.List", "indexOf", 1) to ::constMinusOne,
        makeDescription("kotlin.collections.List", "lastIndexOf", 1) to ::constMinusOne
    )

    fun findSpecialWithOverride(irFunction: IrSimpleFunction): Pair<IrSimpleFunction, (IrSimpleFunction) -> IrExpression>? {
        irFunction.allOverridden().forEach { overridden ->
            val description = overridden.toDescription()
            SPECIAL_METHODS_WITH_DEFAULTS_MAP[description]?.let {
                return Pair(overridden, it)
            }
        }
        return null
    }
}

fun IrSimpleFunction.allOverridden(): Sequence<IrSimpleFunction> {
    val visited = mutableSetOf<IrSimpleFunction>()

    fun IrSimpleFunction.search(): Sequence<IrSimpleFunction> {
        if (this in visited) return emptySequence()
        return sequence {
            yield(this@search)
            visited.add(this@search)
            overriddenSymbols.forEach { yieldAll(it.owner.search()) }
        }
    }

    return search().drop(1) // First element is `this`
}
