/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package org.jetbrains.kotlin.bir.backend.builders

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.expressions.BirConst
import org.jetbrains.kotlin.bir.expressions.impl.BirConstImpl
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrConstKind

context(BirBackendContext)
inline fun BirConst.Companion.constNull(
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.nothingNType
): BirConstImpl<Nothing?> =
    BirConstImpl(sourceSpan, type, IrConstKind.Null, null)

context(BirBackendContext)
inline fun BirConst(
    value: Boolean,
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.booleanType,
): BirConstImpl<Boolean> =
    BirConstImpl(sourceSpan, type, IrConstKind.Boolean, value)

context(BirBackendContext)
inline fun BirConst(
    value: Byte,
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.byteType,
): BirConstImpl<Byte> =
    BirConstImpl(sourceSpan, type, IrConstKind.Byte, value)

context(BirBackendContext)
inline fun BirConst(
    value: Short,
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.shortType,
): BirConstImpl<Short> =
    BirConstImpl(sourceSpan, type, IrConstKind.Short, value)

context(BirBackendContext)
inline fun BirConst(
    value: Int,
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.intType,
): BirConstImpl<Int> =
    BirConstImpl(sourceSpan, type, IrConstKind.Int, value)

context(BirBackendContext)
inline fun BirConst(
    value: Long,
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.longType,
): BirConstImpl<Long> =
    BirConstImpl(sourceSpan, type, IrConstKind.Long, value)

context(BirBackendContext)
inline fun BirConst(
    value: Float,
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.floatType,
): BirConstImpl<Float> =
    BirConstImpl(sourceSpan, type, IrConstKind.Float, value)

context(BirBackendContext)
inline fun BirConst(
    value: Double,
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.doubleType,
): BirConstImpl<Double> =
    BirConstImpl(sourceSpan, type, IrConstKind.Double, value)

context(BirBackendContext)
inline fun BirConst(
    value: Char,
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.charType,
): BirConstImpl<Char> =
    BirConstImpl(sourceSpan, type, IrConstKind.Char, value)

context(BirBackendContext)
inline fun BirConst(
    value: String,
    sourceSpan: SourceSpan = SourceSpan.UNDEFINED,
    type: BirType = birBuiltIns.stringType
): BirConstImpl<String> =
    BirConstImpl(sourceSpan, type, IrConstKind.String, value)