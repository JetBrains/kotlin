/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.AbstractPropertyReferenceLowering
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType

class PropertyReferenceLowering(context: JsIrBackendContext) : AbstractPropertyReferenceLowering<JsIrBackendContext>(context) {

    private val referenceBuilderSymbol = context.symbols.kpropertyBuilder
    private val localDelegateBuilderSymbol = context.symbols.klocalDelegateBuilder
    private val jsClassSymbol = context.symbols.jsClass

    override fun IrBuilderWithScope.createKProperty(
        reference: IrRichPropertyReference,
        typeArguments: List<IrType>,
        getterReference: IrRichFunctionReference,
        setterReference: IrRichFunctionReference?,
    ): IrExpression {
        // 0 - name
        // 1 - paramCount
        // 2 - type
        // 3 - getter
        // 4 - setter

        return irCall(referenceBuilderSymbol).apply {
            arguments[0] = propertyReferenceNameExpression(reference)
            arguments[1] = irInt(typeArguments.size - 1)
            arguments[2] = reference.getJsTypeConstructor()
            arguments[3] = getterReference
            arguments[4] = setterReference ?: irNull()
            arguments[5] = propertyReferenceLinkageErrorExpression(reference, this@PropertyReferenceLowering.context::getVoid)
        }
    }

    override fun IrBuilderWithScope.createLocalKProperty(
        reference: IrRichPropertyReference,
        propertyName: String,
        propertyType: IrType,
        isMutable: Boolean,
    ): IrExpression {
        return irCall(localDelegateBuilderSymbol, reference.type).apply {
            // 0 - name
            // 1 - type
            // 2 - isMutable

            arguments[0] = irString(propertyName)
            arguments[1] = reference.getJsTypeConstructor()
            arguments[2] = irBoolean(reference.setterFunction != null)
        }
    }

    override fun functionReferenceClass(arity: Int): IrClassSymbol {
        return context.symbols.functionN(arity)
    }

    private fun IrExpression.getJsTypeConstructor(): IrExpression {
        val irCall = IrCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, jsClassSymbol.owner.returnType, jsClassSymbol,
            typeArgumentsCount = 1,
        )
        irCall.typeArguments[0] = type
        return irCall
    }
}
