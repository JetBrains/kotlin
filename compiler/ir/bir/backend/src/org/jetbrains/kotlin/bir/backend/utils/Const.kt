/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.utils

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.expressions.BirConst
import org.jetbrains.kotlin.bir.expressions.impl.BirConstImpl
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrConstKind

context(BirBackendContext)
fun BirConst.Companion.constNull(
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.nothingNType
): BirConstImpl<Nothing?> =
    BirConstImpl(sourceSpan, type, IrConstKind.Null, null)

context(BirBackendContext)
fun BirConst.Companion.boolean(
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.booleanType,
    value: Boolean
): BirConstImpl<Boolean> =
    BirConstImpl(sourceSpan, type, IrConstKind.Boolean, value)

context(BirBackendContext)
fun BirConst.Companion.constTrue(
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.booleanType
): BirConstImpl<Boolean> =
    boolean(sourceSpan, type, true)

context(BirBackendContext)
fun BirConst.Companion.constFalse(
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.booleanType
): BirConstImpl<Boolean> =
    boolean(sourceSpan, type, false)

context(BirBackendContext)
fun BirConst.Companion.byte(
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.byteType,
    value: Byte
): BirConstImpl<Byte> =
    BirConstImpl(sourceSpan, type, IrConstKind.Byte, value)

context(BirBackendContext)
fun BirConst.Companion.short(
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.shortType,
    value: Short
): BirConstImpl<Short> =
    BirConstImpl(sourceSpan, type, IrConstKind.Short, value)

context(BirBackendContext)
fun BirConst.Companion.int(
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.intType,
    value: Int
): BirConstImpl<Int> =
    BirConstImpl(sourceSpan, type, IrConstKind.Int, value)

context(BirBackendContext)
fun BirConst.Companion.long(
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.longType,
    value: Long
): BirConstImpl<Long> =
    BirConstImpl(sourceSpan, type, IrConstKind.Long, value)

context(BirBackendContext)
fun BirConst.Companion.float(
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.floatType,
    value: Float
): BirConstImpl<Float> =
    BirConstImpl(sourceSpan, type, IrConstKind.Float, value)

context(BirBackendContext)
fun BirConst.Companion.double(
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.doubleType,
    value: Double
): BirConstImpl<Double> =
    BirConstImpl(sourceSpan, type, IrConstKind.Double, value)

context(BirBackendContext)
fun BirConst.Companion.char(
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.charType,
    value: Char
): BirConstImpl<Char> =
    BirConstImpl(sourceSpan, type, IrConstKind.Char, value)

context(BirBackendContext)
fun BirConst.Companion.string(
    value: String,
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.stringType
): BirConstImpl<String> =
    BirConstImpl(sourceSpan, type, IrConstKind.String, value)