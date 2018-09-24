/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class SingletonReferencesLowering(val context: JvmBackendContext) : BodyLoweringPass, IrElementTransformerVoid() {
    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(this)
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrExpression {
        val entrySymbol = context.declarationFactory.getFieldForEnumEntry(expression.symbol.owner, expression.type)
        return IrGetFieldImpl(expression.startOffset, expression.endOffset, entrySymbol.symbol, expression.type)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        val instanceField = context.declarationFactory.getFieldForObjectInstance(expression.symbol.owner)
        return IrGetFieldImpl(expression.startOffset, expression.endOffset, instanceField.symbol, expression.type)
    }
}