/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class PrimitiveCompanionLowering(val context: JsIrBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                val symbol = expression.symbol
                if (!symbol.isBound) return expression
                val declaration = symbol.owner
                if (!declaration.isCompanion) return expression
                val parent = declaration.parent as IrClass
                if (!parent.defaultType.isPrimitiveType()) return expression
                val actualCompanion = context.primitiveCompanionObjects[parent.name] ?: return expression
                return expression.run { IrGetObjectValueImpl(startOffset, endOffset, actualCompanion.owner.defaultType, actualCompanion) }
            }
        })
    }
}