/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.allOverridden
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
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
    val defaultValueGenerator: (IrSimpleFunction) -> IrExpression,
    val argumentsToCheck: Int,
    val needsArgumentBoxing: Boolean = false,
    val needsGenericSignature: Boolean = false,
)

class BuiltInWithDifferentJvmName(
    val needsGenericSignature: Boolean = false,
    val isOverriding: Boolean = true
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

    private val specialMethodsWithDefaults = mapOf(
        makeDescription(StandardNames.FqNames.collection, "contains", 1) to
                SpecialMethodWithDefaultInfo(::constFalse, 1),
        makeDescription(StandardNames.FqNames.mutableCollection, "remove", 1) to
                SpecialMethodWithDefaultInfo(::constFalse, 1, needsArgumentBoxing = false),
        makeDescription(StandardNames.FqNames.map, "containsKey", 1) to
                SpecialMethodWithDefaultInfo(::constFalse, 1),
        makeDescription(StandardNames.FqNames.map, "containsValue", 1) to
                SpecialMethodWithDefaultInfo(::constFalse, 1),
        makeDescription(StandardNames.FqNames.mutableMap, "remove", 2) to
                SpecialMethodWithDefaultInfo(::constFalse, 2),
        makeDescription(StandardNames.FqNames.list, "indexOf", 1) to
                SpecialMethodWithDefaultInfo(::constMinusOne, 1),
        makeDescription(StandardNames.FqNames.list, "lastIndexOf", 1) to
                SpecialMethodWithDefaultInfo(::constMinusOne, 1),
        makeDescription(StandardNames.FqNames.map, "getOrDefault", 2) to
                SpecialMethodWithDefaultInfo(::getSecondArg, 1, needsGenericSignature = true),
        makeDescription(StandardNames.FqNames.map, "get", 1) to
                SpecialMethodWithDefaultInfo(::constNull, 1, needsGenericSignature = true),
        makeDescription(StandardNames.FqNames.mutableMap, "remove", 1) to
                SpecialMethodWithDefaultInfo(::constNull, 1, needsGenericSignature = true)
    )

    private val specialProperties = mapOf(
        makeDescription(StandardNames.FqNames.collection, "size") to BuiltInWithDifferentJvmName(),
        makeDescription(StandardNames.FqNames.map, "size") to BuiltInWithDifferentJvmName(),
        makeDescription(StandardNames.FqNames.charSequence.toSafe(), "length") to BuiltInWithDifferentJvmName(),
        makeDescription(StandardNames.FqNames.map, "keys") to BuiltInWithDifferentJvmName(needsGenericSignature = true),
        makeDescription(StandardNames.FqNames.map, "values") to BuiltInWithDifferentJvmName(needsGenericSignature = true),
        makeDescription(StandardNames.FqNames.map, "entries") to BuiltInWithDifferentJvmName(needsGenericSignature = true)
    )

    private val specialMethods = mapOf(
        makeDescription(StandardNames.FqNames.number.toSafe(), "toByte") to BuiltInWithDifferentJvmName(),
        makeDescription(StandardNames.FqNames.number.toSafe(), "toShort") to BuiltInWithDifferentJvmName(),
        makeDescription(StandardNames.FqNames.number.toSafe(), "toInt") to BuiltInWithDifferentJvmName(),
        makeDescription(StandardNames.FqNames.number.toSafe(), "toLong") to BuiltInWithDifferentJvmName(),
        makeDescription(StandardNames.FqNames.number.toSafe(), "toFloat") to BuiltInWithDifferentJvmName(),
        makeDescription(StandardNames.FqNames.number.toSafe(), "toDouble") to BuiltInWithDifferentJvmName(),
        makeDescription(StandardNames.FqNames.charSequence.toSafe(), "get", 1) to BuiltInWithDifferentJvmName(),
        makeDescription(StandardNames.FqNames.mutableList, "removeAt", 1) to
                BuiltInWithDifferentJvmName(needsGenericSignature = true, isOverriding = false)
    )

    val specialMethodNames = (specialMethodsWithDefaults + specialMethods).map { (description) -> description.name }.toHashSet()
    val specialPropertyNames = specialProperties.map { (description) -> description.name }.toHashSet()

    fun findSpecialWithOverride(
        irFunction: IrSimpleFunction,
        includeSelf: Boolean = false
    ): Pair<IrSimpleFunction, SpecialMethodWithDefaultInfo>? {
        if (irFunction.parent !is IrClass)
            return null

        for (overridden in irFunction.allOverridden(includeSelf)) {
            val description = overridden.toDescription()
            specialMethodsWithDefaults[description]?.let {
                return Pair(overridden, it)
            }
        }
        return null
    }

    fun getSpecialMethodInfo(irFunction: IrSimpleFunction): SpecialMethodWithDefaultInfo? {
        val description = irFunction.toDescription()
        return specialMethodsWithDefaults[description]
    }

    fun getBuiltInWithDifferentJvmName(irFunction: IrSimpleFunction): BuiltInWithDifferentJvmName? {
        irFunction.correspondingPropertySymbol?.let {
            val classFqName = irFunction.parentAsClass.fqNameWhenAvailable
                ?: return null

            return specialProperties[makeDescription(classFqName, it.owner.name.asString())]
        }

        return specialMethods[irFunction.toDescription()]
    }
}
