/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SimpleTypeRemapper
import org.jetbrains.kotlin.ir.util.withinScope
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name

class SyntheticAccessorLowering(private val context: CommonBackendContext) : BodyLoweringPass {

    private class CandidatesCollector(val candidates: MutableCollection<IrSimpleFunction>) : IrElementVisitorVoid {

        private fun IrSimpleFunction.isTopLevelPrivate(): Boolean {
            if (visibility != DescriptorVisibilities.PRIVATE) return false
            return parent is IrFile
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            if (declaration.isInline) {
                super.visitSimpleFunction(declaration)
            }
        }

        override fun visitConstructor(declaration: IrConstructor) {}

        override fun visitProperty(declaration: IrProperty) {}

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {}

        override fun visitMemberAccess(expression: IrMemberAccessExpression<*>) {

            super.visitMemberAccess(expression)

            val callee = expression.symbol.owner as? IrSimpleFunction

            if (callee != null) {
                if (callee.isInline) return

                if (callee.isTopLevelPrivate()) {
                    candidates.add(callee)
                }
            }
        }
    }

    private class FunctionCopier(fileHash: String) {
        fun copy(function: IrSimpleFunction): IrSimpleFunction {
            function.acceptVoid(symbolRemapper)
            return function.transform(copier, data = null) as IrSimpleFunction
        }

        private val symbolRemapper = object : DeepCopySymbolRemapper() {
            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                remapSymbol(functions, declaration) {
                    IrSimpleFunctionSymbolImpl()
                }
                declaration.typeParameters.forEach { it.acceptVoid(this) }
                declaration.extensionReceiverParameter?.acceptVoid(this)
                declaration.valueParameters.forEach { it.acceptVoid(this) }
            }
        }

        private val typeRemapper = SimpleTypeRemapper(symbolRemapper)

        private val copier = object : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
                val newName = Name.identifier("${declaration.name.asString()}\$accessor\$$fileHash")
                return declaration.factory.createSimpleFunction(
                    startOffset = declaration.startOffset,
                    endOffset = declaration.endOffset,
                    origin = mapDeclarationOrigin(declaration.origin),
                    name = newName,
                    visibility = DescriptorVisibilities.INTERNAL,
                    isInline = declaration.isInline,
                    isExpect = declaration.isExpect,
                    returnType = IrUninitializedType,
                    modality = declaration.modality,
                    symbol = symbolRemapper.getDeclaredFunction(declaration.symbol),
                    isTailrec = declaration.isTailrec,
                    isSuspend = declaration.isSuspend,
                    isOperator = declaration.isOperator,
                    isInfix = declaration.isInfix,
                    isExternal = declaration.isExternal,
                    containerSource = declaration.containerSource,
                    isFakeOverride = declaration.isFakeOverride,
                ).apply {
                    assert(declaration.overriddenSymbols.isEmpty()) { "Top level function overrides nothing" }
                    transformFunctionChildren(declaration)
                }
            }

            private fun IrSimpleFunction.transformFunctionChildren(declaration: IrSimpleFunction) {
                copyTypeParametersFrom(declaration)
                typeRemapper.withinScope(this) {
                    assert(declaration.dispatchReceiverParameter == null) { "Top level functions do not have dispatch receiver" }
                    extensionReceiverParameter = declaration.extensionReceiverParameter?.transform()?.also {
                        it.parent = this
                    }
                    returnType = typeRemapper.remapType(declaration.returnType)
                    valueParameters = declaration.valueParameters.transform()
                    valueParameters.forEach { it.parent = this }
                    typeParameters.forEach { it.parent = this }
                }
            }
        }
    }

    private fun IrSimpleFunction.createAccessor(fileHash: String): IrSimpleFunction {
        val copier = FunctionCopier(fileHash)
        val newFunction = copier.copy(this)

        val irCall = IrCallImpl(startOffset, endOffset, newFunction.returnType, symbol, typeParameters.size, valueParameters.size)

        newFunction.typeParameters.forEachIndexed { i, tp ->
            irCall.putTypeArgument(i, tp.defaultType)
        }

        newFunction.valueParameters.forEachIndexed { i, vp ->
            irCall.putValueArgument(i, IrGetValueImpl(startOffset, endOffset, vp.type, vp.symbol))
        }

        irCall.extensionReceiver = newFunction.extensionReceiverParameter?.let {
            IrGetValueImpl(startOffset, endOffset, it.type, it.symbol)
        }

        val irReturn = IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, newFunction.symbol, irCall)

        newFunction.body = context.irFactory.createBlockBody(startOffset, endOffset, listOf(irReturn))

        return newFunction
    }

    class CallSiteTransformer(private val functionMap: Map<IrSimpleFunction, IrSimpleFunction>) : IrElementTransformerVoid() {
        override fun visitCall(expression: IrCall): IrExpression {
            expression.transformChildrenVoid()

            val callee = expression.symbol.owner

            functionMap[callee]?.let { newFunction ->
                val newExpression = expression.run {
                    IrCallImpl(startOffset, endOffset, type, newFunction.symbol, typeArgumentsCount, valueArgumentsCount, origin)
                }

                newExpression.copyTypeArgumentsFrom(expression)
                newExpression.extensionReceiver = expression.extensionReceiver
                for (i in 0 until expression.valueArgumentsCount) {
                    newExpression.putValueArgument(i, expression.getValueArgument(i))
                }

                return newExpression
            }

            return expression
        }

        override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
            expression.transformChildrenVoid()

            val callee = expression.symbol.owner

            functionMap[callee]?.let { newFunction ->
                val newExpression = expression.run {
                    // TODO: What has to be done with `reflectionTarget`?
                    IrFunctionReferenceImpl(startOffset, endOffset, type, newFunction.symbol, typeArgumentsCount, valueArgumentsCount)
                }

                newExpression.copyTypeArgumentsFrom(expression)
                newExpression.extensionReceiver = expression.extensionReceiver
                for (i in 0 until expression.valueArgumentsCount) {
                    newExpression.putValueArgument(i, expression.getValueArgument(i))
                }

                return newExpression
            }

            return expression
        }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {}

    override fun lower(irFile: IrFile) {
        val candidates = mutableListOf<IrSimpleFunction>()
        irFile.acceptChildrenVoid(CandidatesCollector(candidates))

        if (candidates.isEmpty()) return

        val fileHash = irFile.fileEntry.name.hashCode().toUInt().toString(Character.MAX_RADIX)
        val accessors = candidates.map { context.irFactory.stageController.restrictTo(it) { it.createAccessor(fileHash) } }
        val candidatesMap = candidates.zip(accessors).toMap()

        irFile.transformChildrenVoid(CallSiteTransformer(candidatesMap))

        accessors.forEach { it.parent = irFile }
        irFile.declarations.addAll(accessors)
    }
}