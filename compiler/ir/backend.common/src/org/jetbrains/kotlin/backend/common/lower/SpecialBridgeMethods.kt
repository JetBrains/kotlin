/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
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

data class SpecialMethodWithDefaultInfo(
    val defaultValueGenerator: (IrSimpleFunction) -> IrExpression, val argumentsToCheck: Int, val needsArgumentBoxing: Boolean = false
)

class SpecialBridgeMethods(val context: CommonBackendContext) {
    private data class SpecialMethodDescription(val kotlinFqClassName: FqName?, val name: Name, val arity: Int)

    private fun makeDescription(classFqName: FqName, funName: String, arity: Int = 0) =
        SpecialMethodDescription(
            classFqName,
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

    private val SPECIAL_METHODS_WITH_DEFAULTS_MAP = mapOf(
        makeDescription(KotlinBuiltIns.FQ_NAMES.collection, "contains", 1) to SpecialMethodWithDefaultInfo(::constFalse, 1),
        makeDescription(KotlinBuiltIns.FQ_NAMES.mutableCollection, "remove", 1) to SpecialMethodWithDefaultInfo(::constFalse, 1, true),
        makeDescription(KotlinBuiltIns.FQ_NAMES.map, "containsKey", 1) to SpecialMethodWithDefaultInfo(::constFalse, 1),
        makeDescription(KotlinBuiltIns.FQ_NAMES.map, "containsValue", 1) to SpecialMethodWithDefaultInfo(::constFalse, 1),
        makeDescription(KotlinBuiltIns.FQ_NAMES.mutableMap, "remove", 2) to SpecialMethodWithDefaultInfo(::constFalse, 2),
        makeDescription(KotlinBuiltIns.FQ_NAMES.map, "getOrDefault", 2) to SpecialMethodWithDefaultInfo(::getSecondArg, 1),
        makeDescription(KotlinBuiltIns.FQ_NAMES.map, "get", 1) to SpecialMethodWithDefaultInfo(::constNull, 1),
        makeDescription(KotlinBuiltIns.FQ_NAMES.mutableMap, "remove", 1) to SpecialMethodWithDefaultInfo(::constNull, 1),
        makeDescription(KotlinBuiltIns.FQ_NAMES.list, "indexOf", 1) to SpecialMethodWithDefaultInfo(::constMinusOne, 1),
        makeDescription(KotlinBuiltIns.FQ_NAMES.list, "lastIndexOf", 1) to SpecialMethodWithDefaultInfo(::constMinusOne, 1)
    )

    private val SPECIAL_PROPERTIES_SET = setOf(
        makeDescription(KotlinBuiltIns.FQ_NAMES.collection, "size"),
        makeDescription(KotlinBuiltIns.FQ_NAMES.map, "size"),
        makeDescription(KotlinBuiltIns.FQ_NAMES.charSequence.toSafe(), "length"),
        makeDescription(KotlinBuiltIns.FQ_NAMES.map, "keys"),
        makeDescription(KotlinBuiltIns.FQ_NAMES.map, "values"),
        makeDescription(KotlinBuiltIns.FQ_NAMES.map, "entries")
    )

    private val SPECIAL_METHODS_SETS = setOf(
        makeDescription(KotlinBuiltIns.FQ_NAMES.number.toSafe(), "toByte"),
        makeDescription(KotlinBuiltIns.FQ_NAMES.number.toSafe(), "toShort"),
        makeDescription(KotlinBuiltIns.FQ_NAMES.number.toSafe(), "toInt"),
        makeDescription(KotlinBuiltIns.FQ_NAMES.number.toSafe(), "toLong"),
        makeDescription(KotlinBuiltIns.FQ_NAMES.number.toSafe(), "toFloat"),
        makeDescription(KotlinBuiltIns.FQ_NAMES.number.toSafe(), "toDouble"),
        makeDescription(KotlinBuiltIns.FQ_NAMES.mutableList, "removeAt", 1),
        makeDescription(KotlinBuiltIns.FQ_NAMES.charSequence.toSafe(), "get", 1)
    )

    fun findSpecialWithOverride(irFunction: IrSimpleFunction): Pair<IrSimpleFunction, SpecialMethodWithDefaultInfo>? {
        irFunction.allOverridden().forEach { overridden ->
            val description = overridden.toDescription()
            SPECIAL_METHODS_WITH_DEFAULTS_MAP[description]?.let {
                return Pair(overridden, it)
            }
        }
        return null
    }

    fun getSpecialMethodInfo(irFunction: IrSimpleFunction): SpecialMethodWithDefaultInfo? {
        val description = irFunction.toDescription()
        return SPECIAL_METHODS_WITH_DEFAULTS_MAP[description]
    }

    fun isBuiltInWithDifferentJvmName(irFunction: IrSimpleFunction): Boolean {
        irFunction.correspondingPropertySymbol?.let {
            val classFqName = irFunction.parentAsClass.fqNameWhenAvailable
                ?: return false

            return makeDescription(classFqName, it.owner.name.asString()) in SPECIAL_PROPERTIES_SET
        }

        return irFunction.toDescription() in SPECIAL_METHODS_SETS
    }
}

fun IrSimpleFunction.allOverridden(includeSelf: Boolean = false): Sequence<IrSimpleFunction> {
    val visited = mutableSetOf<IrSimpleFunction>()

    fun IrSimpleFunction.search(): Sequence<IrSimpleFunction> {
        if (this in visited) return emptySequence()
        return sequence {
            yield(this@search)
            visited.add(this@search)
            overriddenSymbols.forEach { yieldAll(it.owner.search()) }
        }
    }

    return if (includeSelf) search() else search().drop(1)
}
