/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.DefaultParameterInjector
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.irCall

internal class KonanDefaultParameterInjector(private val konanContext: KonanBackendContext)
    : DefaultParameterInjector(konanContext, skipInline = false) {

    override fun nullConst(startOffset: Int, endOffset: Int, type: IrType): IrExpression {
        val symbols = konanContext.ir.symbols

        val nullConstOfEquivalentType = when (type.computePrimitiveBinaryTypeOrNull()) {
            null -> IrConstImpl.constNull(startOffset, endOffset, context.irBuiltIns.nothingNType)
            PrimitiveBinaryType.BOOLEAN -> IrConstImpl.boolean(startOffset, endOffset, type, false)
            PrimitiveBinaryType.BYTE -> IrConstImpl.byte(startOffset, endOffset, type, 0)
            PrimitiveBinaryType.SHORT -> IrConstImpl.short(startOffset, endOffset, type, 0)
            PrimitiveBinaryType.INT -> IrConstImpl.int(startOffset, endOffset, type, 0)
            PrimitiveBinaryType.LONG -> IrConstImpl.long(startOffset, endOffset, type, 0)
            PrimitiveBinaryType.FLOAT -> IrConstImpl.float(startOffset, endOffset, type, 0.0F)
            PrimitiveBinaryType.DOUBLE -> IrConstImpl.double(startOffset, endOffset, type, 0.0)
            PrimitiveBinaryType.POINTER -> irCall(startOffset, endOffset, symbols.getNativeNullPtr.owner, emptyList())
        }

        return irCall(
                startOffset,
                endOffset,
                symbols.reinterpret.owner,
                listOf(nullConstOfEquivalentType.type, type)
        ).apply {
            extensionReceiver = nullConstOfEquivalentType
        }
    }
}