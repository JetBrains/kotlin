/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.symbols.JsSymbolBuilder
import org.jetbrains.kotlin.ir.backend.js.symbols.initialize
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class SecondaryCtorLowering(val context: JsIrBackendContext) {

    data class ConstructorPair(val delegate: IrSimpleFunctionSymbol, val stub: IrSimpleFunctionSymbol)

    private val oldCtorToNewMap = mutableMapOf<IrConstructorSymbol, ConstructorPair>()

    fun getConstructorProcessorLowering() = object : DeclarationContainerLoweringPass {
        override fun lower(irDeclarationContainer: IrDeclarationContainer) {
            irDeclarationContainer.declarations.transformFlat {
                if (it is IrClass) {
                    listOf(it) + lowerClass(it)
                } else null
            }
        }
    }::runOnFilePostfix

    fun getConstructorRedirectorLowering() = object : DeclarationContainerLoweringPass {
        override fun lower(irDeclarationContainer: IrDeclarationContainer) {
            for (it in irDeclarationContainer.declarations) {
                it.accept(CallsiteRedirectionTransformer(), null)
            }
        }
    }::runOnFilePostfix

    private fun lowerClass(irClass: IrClass): List<IrSimpleFunction> {
        val className = irClass.name.asString()
        val oldConstructors = mutableListOf<IrConstructor>()
        val newConstructors = mutableListOf<IrSimpleFunction>()

        for (declaration in irClass.declarations) {
            if (declaration is IrConstructor && !declaration.isPrimary) {
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
                val newInitConstructor = createInitConstructor(declaration, irClass, constructorName, irClass.defaultType)
                val newCreateConstructor = createCreateConstructor(declaration, newInitConstructor, constructorName, irClass.defaultType)

                oldCtorToNewMap[declaration.symbol] = ConstructorPair(newInitConstructor.symbol, newCreateConstructor.symbol)

                oldConstructors += declaration
                newConstructors += newInitConstructor
                newConstructors += newCreateConstructor
            }
        }

        irClass.declarations.removeAll(oldConstructors)

        return newConstructors
    }

    private class ThisUsageReplaceTransformer(
        val function: IrFunctionSymbol,
        val newThisSymbol: IrValueSymbol,
        val oldThisSymbol: IrValueSymbol?
    ) :
        IrElementTransformerVoid() {

        override fun visitReturn(expression: IrReturn): IrExpression = IrReturnImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            function,
            IrGetValueImpl(expression.startOffset, expression.endOffset, newThisSymbol.owner.type, newThisSymbol)
        )

        override fun visitGetValue(expression: IrGetValue): IrExpression =
            if (expression.symbol == oldThisSymbol) IrGetValueImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                newThisSymbol,
                expression.origin
            ) else {
                expression
            }
    }

    private fun createInitConstructor(
        declaration: IrConstructor,
        klass: IrClass,
        name: String,
        type: IrType
    ): IrSimpleFunction =
        JsSymbolBuilder.copyFunctionSymbol(declaration.symbol, "${name}_\$Init\$").let {

            val thisSymbol =
                JsSymbolBuilder.buildValueParameter(it, declaration.valueParameters.size, type, "\$this")

            it.initialize(
                dispatchParameterDescriptor = declaration.descriptor.dispatchReceiverParameter,
                typeParameters = declaration.descriptor.typeParameters,
                valueParameters = declaration.descriptor.valueParameters + thisSymbol.descriptor as ValueParameterDescriptor,
                returnType = type,
                modality = declaration.descriptor.modality,
                visibility = declaration.descriptor.visibility
            )

            val thisParam = JsIrBuilder.buildValueParameter(thisSymbol, type)
            val oldThisReceiver = klass.thisReceiver?.symbol

            return IrFunctionImpl(
                declaration.startOffset, declaration.endOffset,
                declaration.origin, it
            ).apply {
                val retStmt = JsIrBuilder.buildReturn(it, JsIrBuilder.buildGetValue(thisSymbol), context.irBuiltIns.nothingType)
                val statements = (declaration.body as IrStatementContainer).statements

                valueParameters += (declaration.valueParameters + thisParam)
                typeParameters += declaration.typeParameters
//                parent = declaration.parent
                body = JsIrBuilder.buildBlockBody(statements + retStmt).apply {
                    transformChildrenVoid(ThisUsageReplaceTransformer(it, thisSymbol, oldThisReceiver))
                }
            }

        }


    private fun createCreateConstructor(ctorOrig: IrConstructor, ctorImpl: IrSimpleFunction, name: String, type: IrType): IrSimpleFunction =
        JsSymbolBuilder.copyFunctionSymbol(ctorOrig.symbol, "${name}_\$Create\$").let {
            it.initialize(
                dispatchParameterDescriptor = ctorOrig.descriptor.dispatchReceiverParameter,
                typeParameters = ctorOrig.descriptor.typeParameters,
                valueParameters = ctorOrig.descriptor.valueParameters,
                returnType = type,
                modality = ctorOrig.descriptor.modality,
                visibility = ctorOrig.visibility
            )

            return IrFunctionImpl(
                ctorOrig.startOffset, ctorOrig.endOffset,
                ctorOrig.origin, it
            ).apply {

                valueParameters += ctorOrig.valueParameters
                typeParameters += ctorOrig.typeParameters
//                parent = ctorOrig.parent

                returnType = type
                val createFunctionIntrinsic = context.intrinsics.jsObjectCreate
                val irCreateCall = JsIrBuilder.buildCall(
                    createFunctionIntrinsic.symbol,
                    returnType,
                    listOf(returnType)
                )
                val irDelegateCall = JsIrBuilder.buildCall(ctorImpl.symbol, type).also {
                    for (i in 0 until valueParameters.size) {
                        it.putValueArgument(i, JsIrBuilder.buildGetValue(valueParameters[i].symbol))
                    }
//                    valueParameters.forEachIndexed { i, p -> it.putValueArgument(i, JsIrBuilder.buildGetValue(p.symbol)) }
                    it.putValueArgument(ctorOrig.valueParameters.size, irCreateCall)

//                typeParameters.mapIndexed { i, t -> ctorImpl.typeParameters[i].descriptor ->  }
                }
                val irReturn = JsIrBuilder.buildReturn(it, irDelegateCall, context.irBuiltIns.nothingType)


                body = JsIrBuilder.buildBlockBody(listOf(irReturn))
            }
        }


    inner class CallsiteRedirectionTransformer : IrElementTransformer<IrFunction?> {

        override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement = super.visitFunction(declaration, declaration)

        override fun visitCall(expression: IrCall, data: IrFunction?): IrElement {
            super.visitCall(expression, data)

            // TODO: figure out the reason why symbol is not bound
            if (expression.symbol.isBound) {

                val target = expression.symbol.owner

                if (target is IrConstructor) {
                    if (!target.descriptor.isPrimary) {
                        val ctor = oldCtorToNewMap[target.symbol]
                        if (ctor != null) {
                            return redirectCall(expression, ctor.stub)
                        }
                    }
                }
            }

            return expression
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: IrFunction?): IrElement {
            super.visitDelegatingConstructorCall(expression, data)

            val target = expression.symbol
            if (target.owner.isPrimary) {
                // nothing to do here
                return expression
            }

            val fromPrimary = data!! is IrConstructor
            // TODO: what is `deserialized` constructor?
            val ctor = oldCtorToNewMap[target] ?: return expression
            val newCall = redirectCall(expression, ctor.delegate)

            val readThis = if (fromPrimary) {
                IrGetValueImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    IrValueParameterSymbolImpl(LazyClassReceiverParameterDescriptor(target.descriptor.containingDeclaration))
                )
            } else {
                IrGetValueImpl(expression.startOffset, expression.endOffset, expression.type, data.valueParameters.last().symbol)
            }

            newCall.putValueArgument(expression.valueArgumentsCount, readThis)

            return newCall
        }

        private fun redirectCall(
            call: IrFunctionAccessExpression,
            newTarget: IrSimpleFunctionSymbol
        ) = IrCallImpl(call.startOffset, call.endOffset, call.type, newTarget).apply {

            copyTypeArgumentsFrom(call)

            for (i in 0 until call.valueArgumentsCount) {
                putValueArgument(i, call.getValueArgument(i))
            }
        }

    }
}
