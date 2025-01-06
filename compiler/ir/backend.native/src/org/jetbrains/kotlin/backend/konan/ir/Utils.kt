/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IdSignatureValues
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.utils.atMostOne

private fun IrClass.isClassTypeWithSignature(signature: IdSignature.CommonSignature): Boolean {
    return signature == symbol.signature
}

fun IrClass.isUnit() = this.isClassTypeWithSignature(IdSignatureValues.unit)

fun IrClass.isKotlinArray() = this.isClassTypeWithSignature(IdSignatureValues.array)

val IrClass.superClasses get() = this.superTypes.map { it.classifierOrFail as IrClassSymbol }
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrClass.getSuperClassNotAny() = this.superClasses.map { it.owner }.atMostOne { !it.isInterface && !it.isAny() }

fun IrClass.isAny() = this.isClassTypeWithSignature(IdSignatureValues.any)

fun IrClass.isNothing() = this.isClassTypeWithSignature(IdSignatureValues.nothing)

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrClass.getSuperInterfaces() = this.superClasses.map { it.owner }.filter { it.isInterface }

fun IrClass.isSpecialClassWithNoSupertypes() = this.isAny() || this.isNothing()

fun IrValueParameter.isInlineParameter(): Boolean =
    !this.isNoinline && (this.type.isFunction() || this.type.isSuspendFunction()) && !this.type.isMarkedNullable()

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun buildSimpleAnnotation(irBuiltIns: IrBuiltIns, startOffset: Int, endOffset: Int,
                          annotationClass: IrClass, vararg args: String): IrConstructorCall {
    val constructor = annotationClass.constructors.let {
        it.singleOrNull() ?: it.single { ctor -> ctor.valueParameters.size == args.size }
    }
    return IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, constructor.returnType, constructor.symbol).apply {
        args.forEachIndexed { index, arg ->
            assert(constructor.valueParameters[index].type == irBuiltIns.stringType) {
                "String type expected but was ${constructor.valueParameters[index].type}"
            }
            putValueArgument(index, IrConstImpl.string(startOffset, endOffset, irBuiltIns.stringType, arg))
        }
    }
}
