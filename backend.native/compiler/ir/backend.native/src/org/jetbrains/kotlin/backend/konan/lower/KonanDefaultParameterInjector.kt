/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    : DefaultParameterInjector(konanContext) {

    override fun nullConst(expression: IrElement, type: IrType): IrExpression {
        val symbols = konanContext.ir.symbols

        val startOffset = expression.startOffset
        val endOffset = expression.endOffset

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