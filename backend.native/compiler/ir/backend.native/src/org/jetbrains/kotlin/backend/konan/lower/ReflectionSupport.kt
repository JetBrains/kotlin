/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.ir.typeWithStarProjections
import org.jetbrains.kotlin.backend.konan.isObjCClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers

internal fun IrBuilderWithScope.irKType(context: KonanBackendContext, type: IrType): IrExpression {
    val kTypeImplSymbol = context.ir.symbols.kTypeImpl
    val kTypeImplForGenericsSymbol = context.ir.symbols.kTypeImplForGenerics

    val kTypeImplConstructorSymbol = kTypeImplSymbol.constructors.single()
    val kTypeImplForGenericsConstructorSymbol = kTypeImplForGenericsSymbol.constructors.single()

    val classifierOrNull = type.classifierOrNull

    return if (classifierOrNull !is IrClassSymbol) {
        // IrTypeParameterSymbol
        irCall(kTypeImplForGenericsConstructorSymbol)
    } else {
        val returnKClass = irKClass(context, classifierOrNull)
        irCall(kTypeImplConstructorSymbol).apply {
            putValueArgument(0, returnKClass)
            putValueArgument(1, irBoolean(type.isMarkedNullable()))
        }
    }
}

internal fun IrBuilderWithScope.irKClass(context: KonanBackendContext, symbol: IrClassifierSymbol): IrExpression {
    val symbols = context.ir.symbols
    return when {
        symbol !is IrClassSymbol -> // E.g. for `T::class` in a body of an inline function itself.
            irCall(symbols.ThrowNullPointerException.owner)

        symbol.descriptor.isObjCClass() ->
            irKClassUnsupported(context, "KClass for Objective-C classes is not supported yet")

        symbol.descriptor.getAllSuperClassifiers().any {
            it is ClassDescriptor && it.fqNameUnsafe == InteropFqNames.nativePointed
        } -> irKClassUnsupported(context, "KClass for interop types is not supported yet")

        else -> irCall(symbols.kClassImplConstructor.owner).apply {
            putValueArgument(0, irCall(symbols.getClassTypeInfo, listOf(symbol.typeWithStarProjections)))
        }
    }
}

private fun IrBuilderWithScope.irKClassUnsupported(context: KonanBackendContext, message: String) =
        irCall(context.ir.symbols.kClassUnsupportedImplConstructor.owner).apply {
            putValueArgument(0, irString(message))
        }
