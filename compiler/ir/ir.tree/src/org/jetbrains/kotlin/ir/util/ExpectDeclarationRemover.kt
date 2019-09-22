/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver

// `doRemove` means should expect-declaration be removed from IR
class ExpectDeclarationRemover(val symbolTable: ReferenceSymbolTable, private val doRemove: Boolean) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        // All declarations with `isExpect == true` are nested into a top-level declaration with `isExpect == true`.
        declaration.declarations.removeAll {
            val descriptor = it.descriptor
            if (descriptor is MemberDescriptor && descriptor.isExpect) {
                copyDefaultArgumentsFromExpectToActual(it)
                doRemove
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
        symbolTable.referenceFunction(descriptor.findActualForExpect()).owner

    private fun IrClass.findActualForExpected(): IrClass =
        symbolTable.referenceClass(descriptor.findActualForExpect()).owner

    private inline fun <reified T : MemberDescriptor> T.findActualForExpect() = with(ExpectedActualResolver) {
        val descriptor = this@findActualForExpect

        if (!descriptor.isExpect) error(this)

        findCompatibleActualForExpected(descriptor.module).singleOrNull() ?: error(descriptor)
    } as T

    private fun IrExpression.remapExpectValueSymbols(): IrExpression {
        return this.transform(object : IrElementTransformerVoid() {

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                expression.transformChildrenVoid()
                val newValue = remapExpectValue(expression.target)
                    ?: return expression

                return IrGetValueImpl(
                    expression.startOffset,
                    expression.endOffset,
                    newValue.type,
                    newValue,
                    expression.origin
                )
            }
        }, data = null)
    }

    private fun remapExpectValue(declaration: IrValueDeclaration): IrValueParameter? {
        if (declaration !is IrValueParameter) {
            return null
        }

        val parent = declaration.parent

        return when (parent) {
            is IrClass -> {
                assert(declaration == parent.thisReceiver)
                parent.findActualForExpected().thisReceiver!!
            }

            is IrFunction -> when (declaration) {
                parent.dispatchReceiverParameter ->
                    parent.findActualForExpected().dispatchReceiverParameter!!
                parent.extensionReceiverParameter ->
                    parent.findActualForExpected().extensionReceiverParameter!!
                else -> {
                    assert(parent.valueParameters[declaration.index] == declaration)
                    parent.findActualForExpected().valueParameters[declaration.index]
                }
            }

            else -> error(parent)
        }
    }
}