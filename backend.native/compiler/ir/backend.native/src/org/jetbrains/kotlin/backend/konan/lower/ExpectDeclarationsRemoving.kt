/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isExpectMember
import org.jetbrains.kotlin.backend.konan.descriptors.propertyIfAccessor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.DeepCopyTypeRemapper
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver

/**
 * This pass removes all declarations with `isExpect == true`.
 * Note: org.jetbrains.kotlin.backend.common.lower.ExpectDeclarationsRemoving is copy of this lower.
 */
internal class ExpectDeclarationsRemoving(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        // All declarations with `isExpect == true` are nested into a top-level declaration with `isExpect == true`.
        irFile.declarations.removeAll {
            if (it.descriptor.isExpectMember) {
                true
            } else {
                false
            }
        }
    }
}

internal class ExpectToActualDefaultValueCopier(val context: Context) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        // All declarations with `isExpect == true` are nested into a top-level declaration with `isExpect == true`.
        irFile.declarations.forEach {
            if (it.descriptor.isExpectMember) {
                copyDefaultArgumentsFromExpectToActual(it)
            }
        }
    }

    private fun copyDefaultArgumentsFromExpectToActual(declaration: IrDeclaration) {
        declaration.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitValueParameter(declaration: IrValueParameter) {
                super.visitValueParameter(declaration)

                val defaultValue = declaration.defaultValue ?: return
                val function = declaration.parent as IrFunction

                val index = declaration.index
                assert(function.valueParameters[index] == declaration)

                if (function is IrConstructor &&
                        ExpectedActualDeclarationChecker.isOptionalAnnotationClass(
                                function.descriptor.constructedClass
                        )
                ) {
                    return
                }

                function.findActualForExpected().valueParameters[index].defaultValue = defaultValue.also {
                    it.expression = it.expression.remapExpectValueSymbols()
                }
            }
        })
    }

    private inline fun <reified T: IrFunction> T.findActualForExpected(): T =
            context.ir.symbols.symbolTable.referenceFunction(descriptor.findActualForExpect()).owner as T

    private fun IrClass.findActualForExpected(): IrClass =
            context.ir.symbols.symbolTable.referenceClass(descriptor.findActualForExpect()).owner

    private inline fun <reified T : MemberDescriptor> T.findActualForExpect() = with(ExpectedActualResolver) {
        val descriptor = this@findActualForExpect

        if (!descriptor.isExpect) error(this)

        findCompatibleActualForExpected(descriptor.module).singleOrNull() ?: error(descriptor)
    } as T

    private fun IrExpression.remapExpectValueSymbols(): IrExpression {
        class SymbolRemapper : DeepCopySymbolRemapper() {
            override fun getReferencedClass(symbol: IrClassSymbol) =
                    if (symbol.descriptor.isExpect)
                        symbol.owner.findActualForExpected().symbol
                    else super.getReferencedClass(symbol)

            override fun getReferencedClassOrNull(symbol: IrClassSymbol?) =
                    symbol?.let { getReferencedClass(it) }

            override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol = when (symbol) {
                is IrClassSymbol -> getReferencedClass(symbol)
                is IrTypeParameterSymbol -> remapExpectTypeParameter(symbol).symbol
                else -> error("Unexpected symbol $symbol ${symbol.descriptor}")
            }

            override fun getReferencedConstructor(symbol: IrConstructorSymbol) =
                    if (symbol.descriptor.isExpect)
                        symbol.owner.findActualForExpected().symbol
                    else super.getReferencedConstructor(symbol)

            override fun getReferencedFunction(symbol: IrFunctionSymbol): IrFunctionSymbol = when (symbol) {
                is IrSimpleFunctionSymbol -> getReferencedSimpleFunction(symbol)
                is IrConstructorSymbol -> getReferencedConstructor(symbol)
                else -> error("Unexpected symbol $symbol ${symbol.descriptor}")
            }

            override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol) = when {
                symbol.descriptor.isExpect -> symbol.owner.findActualForExpected().symbol

                symbol.descriptor.propertyIfAccessor.isExpect -> {
                    val property = symbol.owner.correspondingProperty!!
                    val actualPropertyDescriptor = property.descriptor.findActualForExpect()
                    val accessorDescriptor = when (symbol.owner) {
                        property.getter -> actualPropertyDescriptor.getter!!
                        property.setter -> actualPropertyDescriptor.setter!!
                        else -> error("Unexpected accessor of $symbol ${symbol.descriptor}")
                    }
                    context.ir.symbols.symbolTable.referenceFunction(accessorDescriptor) as IrSimpleFunctionSymbol
                }

                else -> super.getReferencedSimpleFunction(symbol)
            }

            override fun getReferencedValue(symbol: IrValueSymbol) =
                    remapExpectValue(symbol)?.symbol ?: super.getReferencedValue(symbol)
        }

        val symbolRemapper = SymbolRemapper()
        acceptVoid(symbolRemapper)
        return transform(DeepCopyIrTreeWithSymbols(symbolRemapper, DeepCopyTypeRemapper(symbolRemapper)), data = null)
    }

    private fun remapExpectTypeParameter(symbol: IrTypeParameterSymbol): IrTypeParameter {
        val parameter = symbol.owner
        val parent = parameter.parent

        return when (parent) {
            is IrClass ->
                if (!parent.descriptor.isExpect)
                    parameter
                else parent.findActualForExpected().typeParameters[parameter.index]

            is IrFunction ->
                if (!parent.descriptor.isExpect)
                    parameter
                else parent.findActualForExpected().typeParameters[parameter.index]

            else -> error(parent)
        }
    }

    private fun remapExpectValue(symbol: IrValueSymbol): IrValueParameter? {
        if (symbol !is IrValueParameterSymbol) {
            return null
        }

        val parameter = symbol.owner
        val parent = parameter.parent

        return when (parent) {
            is IrClass ->
                if (!parent.descriptor.isExpect)
                    null
                else {
                    assert(parameter == parent.thisReceiver)
                    parent.findActualForExpected().thisReceiver!!
                }

            is IrFunction ->
                if (!parent.descriptor.isExpect)
                    null
                else when (parameter) {
                    parent.dispatchReceiverParameter -> parent.findActualForExpected().dispatchReceiverParameter!!
                    parent.extensionReceiverParameter -> parent.findActualForExpected().extensionReceiverParameter!!
                    else -> {
                        assert(parent.valueParameters[parameter.index] == parameter)
                        parent.findActualForExpected().valueParameters[parameter.index]
                    }
                }

            else -> error(parent)
        }
    }
}
