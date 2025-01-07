/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.AbstractPropertyReferenceLowering
import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrRichFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.name.Name

class PropertyReferenceLowering(context: JsIrBackendContext) : AbstractPropertyReferenceLowering<JsIrBackendContext>(context) {

    private val referenceBuilderSymbol = context.kpropertyBuilder
    private val localDelegateBuilderSymbol = context.klocalDelegateBuilder
    private val jsClassSymbol = context.intrinsics.jsClass

    private val throwISE = context.ir.symbols.throwISE

    override fun IrBuilderWithScope.createKProperty(
        reference: IrRichPropertyReference,
        typeArguments: List<IrType>,
        name: String,
        getterReference: IrRichFunctionReference,
        setterReference: IrRichFunctionReference?
    ): IrExpression {
        return irCall(referenceBuilderSymbol).apply {
            arguments[0] = irString(name)
            arguments[1] = irInt(typeArguments.size - 1)
            arguments[2] = reference.getJsTypeConstructor()
            arguments[3] = getterReference
            arguments[4] = setterReference ?: irNull()
        }
    }

    override fun IrBuilderWithScope.createLocalKProperty(reference: IrRichPropertyReference, propertyName: String, propertyType: IrType): IrExpression {
        val function = context.irFactory.buildFun {
            startOffset = this@createLocalKProperty.startOffset
            endOffset = this@createLocalKProperty.endOffset
            returnType = context.irBuiltIns.nothingType
            name = Name.identifier("${propertyName}\$stub")
        }

        function.parent = scope.getLocalDeclarationParent()

        function.body = with(this@PropertyReferenceLowering.context.createIrBuilder(function.symbol)) {
            irBlockBody {
                +irReturn(irCall(throwISE))
            }
        }

        val functionReferenceType = this@PropertyReferenceLowering.context.symbols.functionN(0).typeWith(context.irBuiltIns.nothingType)
        val getterReference = IrRichFunctionReferenceImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = functionReferenceType,
            reflectionTargetSymbol = null,
            overriddenFunctionSymbol = UpgradeCallableReferences.selectSAMOverriddenFunction(functionReferenceType),
            invokeFunction = function,
            origin = IrStatementOrigin.LAMBDA,
        )
        return irCall(localDelegateBuilderSymbol, reference.type).apply {

            // 0 - name
            // 1 - type
            // 2 - isMutable
            // 3 - lambda

            arguments[0] = irString(propertyName)
            arguments[1] = reference.getJsTypeConstructor()
            arguments[2] = irBoolean(reference.setterFunction != null)
            arguments[3] = getterReference
        }
    }

    override fun functionReferenceClass(arity: Int): IrClassSymbol {
        return context.symbols.functionN(arity)
    }


    private fun IrExpression.getJsTypeConstructor(): IrExpression {
        val irCall = IrCallImpl(
            startOffset, endOffset, jsClassSymbol.owner.returnType, jsClassSymbol,
            typeArgumentsCount = 1,
        )
        irCall.typeArguments[0] = type
        return irCall
    }
}
