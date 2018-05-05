/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.copyAsValueParameter
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.descriptors.JsSymbolBuilder
import org.jetbrains.kotlin.ir.backend.js.descriptors.initialize
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.types.KotlinType

// TODO replace with DeclarationContainerLowerPass && do flatTransform
class FunctionReferenceLowering(val context: JsIrBackendContext) : FileLoweringPass, DeclarationContainerLoweringPass {

    private val lambdas = mutableMapOf<IrDeclaration, KotlinType>()
    private val oldToNewDeclarationMap = mutableMapOf<IrFunctionSymbol, IrFunction>()

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(FunctionReferenceCollector())
        runOnFilePostfix(irFile)
        irFile.transformChildrenVoid(FunctionReferenceVisitor())
    }

    inner class FunctionReferenceCollector : IrElementVisitorVoid {
        override fun visitFunctionReference(expression: IrFunctionReference) {
            lambdas[expression.symbol.owner as IrFunction] = expression.type
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }
    }

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat { d ->
            if (d is IrFunction) {
                lambdas[d]?.let {
                    lowerKFunctionReference(d, it)
                }
            } else null
        }
    }

    inner class FunctionReferenceVisitor : IrElementTransformerVoid() {

        override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
            val newTarget = oldToNewDeclarationMap[expression.symbol]

            return if (newTarget != null) IrCallImpl(expression.startOffset, expression.endOffset, newTarget.symbol, expression.origin).apply {
                copyTypeArgumentsFrom(expression)
                var index = 0
                for (i in 0 until expression.valueArgumentsCount) {
                    val arg = expression.getValueArgument(i)
                    if (arg != null) {
                        putValueArgument(index++, arg)
                    }
                }
            } else expression
        }
    }

    private fun lowerKFunctionReference(declaration: IrFunction, functionType: KotlinType): List<IrFunction> {
        // TODO: property reference

        // transform
        // x = Foo::bar ->
        // x = Foo_bar_KreferenceGet(c1: closure$C1, c2: closure$C2) : KFunctionN<Foo, T2, ..., TN, TReturn> {
        //   return fun Foo_bar_KreferenceClosure(p0: Foo, p1: T2, p2: T3): TReturn {
        //      return p0.bar(c1, c2, p1, p2)
        //   }
        // }

        // KFunctionN<Foo, T2, ..., TN, TReturn>, arguments.size = N + 1

        val closureParams = functionType.arguments.dropLast(1) // drop return type
        var kFunctionValueParamsCount = closureParams.size
        if (declaration.dispatchReceiverParameter != null) kFunctionValueParamsCount--
        if (declaration.extensionReceiverParameter != null) kFunctionValueParamsCount--

        assert(kFunctionValueParamsCount >= 0)

        // The `getter` function takes only parameters which have to be closured
        val getterValueParameters = declaration.valueParameters.dropLast(kFunctionValueParamsCount)
        val getterName = "${declaration.descriptor.name}_KreferenceGet"

        val refGetSymbol =
            JsSymbolBuilder.buildSimpleFunction(declaration.descriptor.containingDeclaration, getterName).apply {
                initialize(
                    valueParameters = getterValueParameters.mapIndexed { i, p -> p.descriptor.copyAsValueParameter(this.descriptor, i) },
                    type = functionType
                )
            }

        val refGetFunction = JsIrBuilder.buildFunction(refGetSymbol).apply {
            getterValueParameters.mapIndexed { i, p ->
                valueParameters += IrValueParameterImpl(p.startOffset, p.endOffset, p.origin, refGetSymbol.descriptor.valueParameters[i])
            }
        }

        val closureName = "${declaration.descriptor.name}_KreferenceClosure"
        val refClosureSymbol = JsSymbolBuilder.buildSimpleFunction(refGetSymbol.descriptor, closureName)

        // the params which are passed to closure
        val closureParamSymbols = closureParams.mapIndexed { index, p ->
            // TODO: re-use original parameter names
            JsSymbolBuilder.buildValueParameter(refClosureSymbol, index, p.type)
        }

        val closureParamDescriptors = closureParamSymbols.map { it.descriptor as ValueParameterDescriptor }

        refClosureSymbol.initialize(valueParameters = closureParamDescriptors, type = declaration.returnType)

        JsIrBuilder.buildFunction(refClosureSymbol).apply {
            closureParamSymbols.forEach { valueParameters += JsIrBuilder.buildValueParameter(it) }

            val irClosureCall = JsIrBuilder.buildCall(declaration.symbol).apply {
                var p = 0
                if (declaration.dispatchReceiverParameter != null) {
                    dispatchReceiver = JsIrBuilder.buildGetValue(closureParamSymbols[p++])
                }
                if (declaration.extensionReceiverParameter != null) {
                    extensionReceiver = JsIrBuilder.buildGetValue(closureParamSymbols[p++])
                }

                var j = 0
                refGetFunction.valueParameters.forEach { v ->
                    putValueArgument(j++, JsIrBuilder.buildGetValue(v.symbol))
                }

                closureParamSymbols.drop(p).forEach { v ->
                    putValueArgument(j++, JsIrBuilder.buildGetValue(v))
                }
            }

            val irClosureReturn = JsIrBuilder.buildReturn(refClosureSymbol, irClosureCall)

            body = JsIrBuilder.buildBlockBody(listOf(irClosureReturn))
        }

        refGetFunction.apply {
            val irClosureReference = JsIrBuilder.buildFunctionReference(functionType, refClosureSymbol)
            val irGetterReturn = JsIrBuilder.buildReturn(refGetSymbol, irClosureReference)
            body = JsIrBuilder.buildBlockBody(listOf(irGetterReturn))
        }

        oldToNewDeclarationMap[declaration.symbol] = refGetFunction

        return listOf(declaration, refGetFunction)
    }
}