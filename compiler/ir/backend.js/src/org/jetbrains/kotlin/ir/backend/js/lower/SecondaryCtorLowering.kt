/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.descriptors.JsSymbolBuilder
import org.jetbrains.kotlin.ir.backend.js.descriptors.initialize
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.isPrimary
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class SecondaryCtorLowering(val context: JsIrBackendContext) : IrElementTransformerVoid(), FileLoweringPass {

    private val oldCtorToNewMap = mutableMapOf<IrConstructorSymbol, JsIrBackendContext.SecondaryCtorPair>()

    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
        context.secondaryConstructorsMap.putAll(oldCtorToNewMap)
    }

    override fun visitFile(irFile: IrFile): IrFile {
        irFile.declarations.transformFlat { declaration ->
            if (declaration is IrClass) {
                listOf(declaration) + lowerClass(declaration)
            } else null
        }

        return irFile
    }

    private fun lowerClass(irClass: IrClass): List<IrSimpleFunction> {
        val className = irClass.name.asString()
        val oldConstructors = mutableListOf<IrConstructor>()
        val newConstructors = mutableListOf<IrSimpleFunction>()

        for (declaration in irClass.declarations) {
            if (declaration is IrConstructor && !declaration.symbol.isPrimary) {
                // TODO delegate name generation
                val constructorName = "${className}_init"
                // We should split secondary constructor into two functions,
                //   *  Initializer which contains constructor's body and takes just created object as implicit param `$this`
                //   **   This function is also delegation constructor
                //   *  Creation function which has same signature with original constructor,
                //      creates new object via `Object.create` builtIn and passes it to corresponding `Init` function
                // In other words:
                // Foo::constructor(...) {
                //   body
                // }
                // =>
                // Foo_init_$Init$(..., $this) {
                //   body[ this = $this ]
                //   return $this
                // }
                // Foo_init_$Create$(...) {
                //   val t = Object.create(Foo.prototype);
                //   return Foo_init_$Init$(..., t)
                // }
                val newInitConstructor = createInitConstructor(declaration, constructorName)
                val newCreateConstructor = createCreateConstructor(declaration, newInitConstructor, constructorName)

                oldCtorToNewMap[declaration.symbol] =
                        JsIrBackendContext.SecondaryCtorPair(newInitConstructor.symbol, newCreateConstructor.symbol)

                oldConstructors += declaration
                newConstructors += newInitConstructor
                newConstructors += newCreateConstructor
            }
        }

        irClass.declarations.removeAll(oldConstructors)

        return newConstructors
    }

    private class ThisUsageReplaceTransformer(val function: IrFunctionSymbol, val thisSymbol: IrValueParameterSymbol) :
        IrElementTransformerVoid() {

        override fun visitReturn(expression: IrReturn): IrExpression = IrReturnImpl(
            expression.startOffset,
            expression.endOffset,
            function,
            IrGetValueImpl(expression.startOffset, expression.endOffset, thisSymbol)
        )

        override fun visitGetValue(expression: IrGetValue): IrExpression =
            if (expression.descriptor.name.isSpecial && expression.descriptor.name.asString() == Namer.THIS_SPECIAL_NAME) IrGetValueImpl(
                expression.startOffset,
                expression.endOffset,
                thisSymbol,
                expression.origin
            ) else {
                expression
            }
    }

    private fun createInitConstructor(declaration: IrConstructor, name: String): IrSimpleFunction =
        JsSymbolBuilder.copyFunctionSymbol(declaration.symbol, "${name}_\$Init\$").let {

            val thisSymbol =
                JsSymbolBuilder.buildValueParameter(it, declaration.valueParameters.size, declaration.returnType, "\$this")

            it.initialize(
                dispatchParameterDescriptor = declaration.descriptor.dispatchReceiverParameter,
                typeParameters = declaration.descriptor.typeParameters,
                valueParameters = declaration.descriptor.valueParameters + thisSymbol.descriptor as ValueParameterDescriptor,
                type = declaration.descriptor.returnType,
                modality = declaration.descriptor.modality,
                visibility = declaration.descriptor.visibility
            )

            val thisParam = JsIrBuilder.buildValueParameter(thisSymbol)

            return IrFunctionImpl(
                declaration.startOffset, declaration.endOffset,
                declaration.origin, it
            ).apply {
                val retStmt = JsIrBuilder.buildReturn(it, JsIrBuilder.buildGetValue(thisSymbol))
                val statements = (declaration.body as IrStatementContainer).statements

                valueParameters += (declaration.valueParameters + thisParam)
                typeParameters += declaration.typeParameters
                parent = declaration.parent
                body = JsIrBuilder.buildBlockBody(statements + retStmt).apply {
                    transformChildrenVoid(ThisUsageReplaceTransformer(it, thisSymbol))
                }
            }

        }


    private fun createCreateConstructor(ctorOrig: IrConstructor, ctorImpl: IrSimpleFunction, name: String): IrSimpleFunction =
        JsSymbolBuilder.copyFunctionSymbol(ctorOrig.symbol, "${name}_\$Create\$").let {
            it.initialize(
                dispatchParameterDescriptor = ctorOrig.descriptor.dispatchReceiverParameter,
                typeParameters = ctorOrig.descriptor.typeParameters,
                valueParameters = ctorOrig.descriptor.valueParameters,
                type = ctorOrig.returnType,
                modality = ctorOrig.descriptor.modality,
                visibility = ctorOrig.visibility
            )

            return IrFunctionImpl(
                ctorOrig.startOffset, ctorOrig.endOffset,
                ctorOrig.origin, it
            ).apply {

                valueParameters += ctorOrig.valueParameters
                typeParameters += ctorOrig.typeParameters
                parent = ctorOrig.parent

                val returnType = ctorOrig.returnType
                val createFunctionIntrinsic = context.intrinsics.jsObjectCreate
                val irCreateCall = JsIrBuilder.buildCall(
                    createFunctionIntrinsic.symbol,
                    returnType,
                    mapOf(createFunctionIntrinsic.typeParameters[0].descriptor to returnType)
                )
                val irDelegateCall = JsIrBuilder.buildCall(ctorImpl.symbol).also {
                    for (i in 0 until valueParameters.size) {
                        it.putValueArgument(i, JsIrBuilder.buildGetValue(valueParameters[i].symbol))
                    }
//                    valueParameters.forEachIndexed { i, p -> it.putValueArgument(i, JsIrBuilder.buildGetValue(p.symbol)) }
                    it.putValueArgument(ctorOrig.valueParameters.size, irCreateCall)

//                typeParameters.mapIndexed { i, t -> ctorImpl.typeParameters[i].descriptor ->  }
                }
                val irReturn = JsIrBuilder.buildReturn(it, irDelegateCall)


                body = JsIrBuilder.buildBlockBody(listOf(irReturn))
            }
        }


    class CallsiteRedirectionTransformer(val context: JsIrBackendContext) : IrElementTransformer<IrFunction?> {

        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement = super.visitFunction(declaration, declaration)

        override fun visitCall(expression: IrCall, ownerFunc: IrFunction?): IrElement {
            if (expression.symbol.isBound) {

                val target = expression.symbol.owner as IrFunction

                if (target is IrConstructor) {
                    if (!target.descriptor.isPrimary) {
                        val ctor = context.secondaryConstructorsMap[target.symbol]
                        if (ctor != null) {

                            return redirectCall(expression, ctor.stub)
                        }
                    }
                }
            }

            return expression
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, ownerFunc: IrFunction?): IrElement {
            val target = expression.symbol.owner
            if (target.symbol.isPrimary) {
                // nothing to do here
                return expression
            }

            val fromPrimary = ownerFunc!! is IrConstructor
            val newCall = redirectCall(expression, context.secondaryConstructorsMap[target.symbol]!!.delegate)

            val readThis = if (fromPrimary) {
                IrGetValueImpl(
                    expression.startOffset,
                    expression.endOffset,
                    IrValueParameterSymbolImpl(LazyClassReceiverParameterDescriptor(target.descriptor.containingDeclaration))
                )
            } else {
                IrGetValueImpl(expression.startOffset, expression.endOffset, ownerFunc.valueParameters.last().symbol)
            }

            newCall.putValueArgument(expression.valueArgumentsCount, readThis)

            return newCall
        }

        private fun redirectCall(
            call: IrFunctionAccessExpression,
            newTarget: IrSimpleFunctionSymbol
        ) = IrCallImpl(call.startOffset, call.endOffset, newTarget).apply {

            copyTypeArgumentsFrom(call)

            for (i in 0 until call.valueArgumentsCount) {
                putValueArgument(i, call.getValueArgument(i))
            }
        }

    }
}
