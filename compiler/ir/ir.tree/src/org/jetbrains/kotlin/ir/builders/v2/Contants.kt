/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.v2

import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType


fun IrBuilderNew.irNull(irType: IrType) : IrConst =
    IrConstImpl.constNull(startOffset, endOffset, irType)

fun IrBuilderNew.irByte(value: Byte, irType: IrType): IrConst =
    IrConstImpl.byte(startOffset, endOffset, irType, value)

fun IrBuilderNew.irShort(value: Short, irType: IrType): IrConst =
    IrConstImpl.short(startOffset, endOffset, irType, value)

fun IrBuilderNew.irInt(value: Int, irType: IrType): IrConst =
    IrConstImpl.int(startOffset, endOffset, irType, value)

fun IrBuilderNew.irLong(value: Long, irType: IrType): IrConst =
    IrConstImpl.long(startOffset, endOffset, irType, value)

fun IrBuilderNew.irChar(value: Char, irType: IrType): IrConst =
    IrConstImpl.char(startOffset, endOffset, irType, value)

fun IrBuilderNew.irString(value: String, irType: IrType): IrConst =
    IrConstImpl.string(startOffset, endOffset, irType, value)

fun IrBuilderNew.irBoolean(value: Boolean, irType: IrType): IrConst =
    IrConstImpl.boolean(startOffset, endOffset, irType, value)

fun IrBuilderNew.irFloat(value: Float, irType: IrType): IrConst =
    IrConstImpl.float(startOffset, endOffset, irType, value)

fun IrBuilderNew.irDouble(value: Double, irType: IrType): IrConst =
    IrConstImpl.double(startOffset, endOffset, irType, value)

fun IrBuilderNew.irTrue(irType: IrType): IrConst =
    irBoolean(true, irType)

fun IrBuilderNew.irFalse(irType: IrType): IrConst =
    irBoolean(false, irType)


context(context: IrBuiltInsAware)
fun IrBuilderNew.irNull(): IrConst = 
    irNull(context.irBuiltIns.nothingNType)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irByte(value: Byte): IrConst =
    irByte(value, context.irBuiltIns.byteType)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irShort(value: Short): IrConst =
    irShort(value, context.irBuiltIns.shortType)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irInt(value: Int): IrConst =
    irInt(value, context.irBuiltIns.intType)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irLong(value: Long): IrConst =
    irLong(value, context.irBuiltIns.longType)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irChar(value: Char): IrConst =
    irChar(value, context.irBuiltIns.charType)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irString(value: String): IrConst =
    irString(value, context.irBuiltIns.stringType)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irBoolean(value: Boolean): IrConst =
    irBoolean(value, context.irBuiltIns.booleanType)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irFloat(value: Float): IrConst =
    irFloat(value, context.irBuiltIns.floatType)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irDouble(value: Double): IrConst =
    irDouble(value, context.irBuiltIns.doubleType)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irTrue(): IrConst = irBoolean(true)

context(context: IrBuiltInsAware)
fun IrBuilderNew.irFalse(): IrConst = irBoolean(false)


context(context: IrBuiltInsAware)
fun IrBuilderNew.irUnit() =
    irGetObjectValue(context.irBuiltIns.unitType)

