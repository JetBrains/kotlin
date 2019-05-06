/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver

/**
 * This pass removes all declarations with `isExpect == true`.
 */
class ExpectDeclarationsRemoving(val context: BackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        // All declarations with `isExpect == true` are nested into a top-level declaration with `isExpect == true`.
        irFile.declarations.removeAll {
            val descriptor = it.descriptor
            if (descriptor is MemberDescriptor && descriptor.isExpect) {
                copyDefaultArgumentsFromExpectToActual(it)
                true
            } else {
                false
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

                val actualParameter = function.findActualForExpected().valueParameters[index]

                // Keep actual default value if present. They are generally not allowed but can be suppressed with
                // @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
                if (actualParameter.defaultValue != null)
                    return

                actualParameter.defaultValue = defaultValue.also {
                    it.expression = it.expression.remapExpectValueSymbols()
                    // Default value might have some declarations inside. Patching parents.
                    it.expression.patchDeclarationParents(actualParameter.parent)
                }
            }
        })
    }

    private fun IrFunction.findActualForExpected(): IrFunction =
        context.ir.symbols.externalSymbolTable.referenceFunction(descriptor.findActualForExpect()).owner

    private fun IrClass.findActualForExpected(): IrClass =
        context.ir.symbols.externalSymbolTable.referenceClass(descriptor.findActualForExpect()).owner

    private inline fun <reified T : MemberDescriptor> T.findActualForExpect() = with(ExpectedActualResolver) {
        val descriptor = this@findActualForExpect

        if (!descriptor.isExpect) error(this)

        findCompatibleActualForExpected(descriptor.module).singleOrNull() ?: error(descriptor)
    } as T

    private fun IrExpression.remapExpectValueSymbols(): IrExpression {
        return this.transform(object : IrElementTransformerVoid() {

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                expression.transformChildrenVoid()
                val newValue = remapExpectValue(expression.symbol)
                    ?: return expression

                return IrGetValueImpl(
                    expression.startOffset,
                    expression.endOffset,
                    newValue.type,
                    newValue.symbol,
                    expression.origin
                )
            }
        }, data = null)
    }

    private fun remapExpectValue(symbol: IrValueSymbol): IrValueParameter? {
        if (symbol !is IrValueParameterSymbol) {
            return null
        }

        val parameter = symbol.owner
        val parent = parameter.parent

        return when (parent) {
            is IrClass -> {
                assert(parameter == parent.thisReceiver)
                parent.findActualForExpected().thisReceiver!!
            }

            is IrFunction -> when (parameter) {
                parent.dispatchReceiverParameter ->
                    parent.findActualForExpected().dispatchReceiverParameter!!
                parent.extensionReceiverParameter ->
                    parent.findActualForExpected().extensionReceiverParameter!!
                else -> {
                    assert(parent.valueParameters[parameter.index] == parameter)
                    parent.findActualForExpected().valueParameters[parameter.index]
                }
            }

            else -> error(parent)
        }
    }
}