/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver

// `doRemove` means should expect-declaration be removed from IR
@OptIn(DescriptorBasedIr::class)
class ExpectDeclarationRemover(val symbolTable: ReferenceSymbolTable, private val doRemove: Boolean) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        super.visitFile(declaration)
        declaration.declarations.removeAll {
            shouldRemoveTopLevelDeclaration(it)
        }
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        super.visitValueParameter(declaration)
        tryCopyDefaultArguments(declaration)
    }

    fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {

        if (declaration.isTopLevelDeclaration && shouldRemoveTopLevelDeclaration(declaration)) {
            return emptyList()
        }

        if (declaration is IrValueParameter) {
            tryCopyDefaultArguments(declaration)
        }

        return null
    }

    private fun shouldRemoveTopLevelDeclaration(declaration: IrDeclaration): Boolean {
        return doRemove && when (declaration) {
            is IrClass -> declaration.isExpect
            is IrProperty -> declaration.isExpect
            is IrFunction -> declaration.isExpect
            else -> false
        }
    }

    private fun tryCopyDefaultArguments(declaration: IrValueParameter) {
        // Keep actual default value if present. They are generally not allowed but can be suppressed with
        // @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
        if (declaration.defaultValue != null) {
            return
        }

        val function = declaration.parent as? IrFunction ?: return

        if (function is IrConstructor &&
            ExpectedActualDeclarationChecker.isOptionalAnnotationClass(function.descriptor.constructedClass)
        ) {
            return
        }

        if (!function.descriptor.isActual) return

        val index = declaration.index

        if (index < 0) return

        assert(function.valueParameters[index] == declaration)

        // If the containing declaration is an `expect class` that matches an `actual typealias`,
        // the `actual fun` or `actual constructor` for this may be in a different module.
        // Nothing we can do with those.
        // TODO they may not actually have the defaults though -- may be a frontend bug.
        val expectParameter = function.findExpectForActual()?.valueParameters?.get(index) ?: return

        val defaultValue = expectParameter.defaultValue ?: return

        defaultValue.let { originalDefault ->
            declaration.defaultValue = IrExpressionBodyImpl(originalDefault.startOffset, originalDefault.endOffset) {
                expression = originalDefault.expression.deepCopyWithSymbols(function).remapExpectValueSymbols()
            }
        }
    }

    private fun IrFunction.findActualForExpected(): IrFunction? =
        descriptor.findActualForExpect()?.let { symbolTable.referenceFunction(it).owner }

    private fun IrFunction.findExpectForActual(): IrFunction? =
        descriptor.findExpectForActual()?.let { symbolTable.referenceFunction(it).owner }

    private fun IrClass.findActualForExpected(): IrClass? =
        descriptor.findActualForExpect()?.let { symbolTable.referenceClass(it).owner }

    private inline fun <reified T : MemberDescriptor> T.findActualForExpect() = with(ExpectedActualResolver) {
        val descriptor = this@findActualForExpect

        if (!descriptor.isExpect) error(this)

        findCompatibleActualForExpected(descriptor.module).singleOrNull()
    } as T?

    private inline fun <reified T : MemberDescriptor> T.findExpectForActual() = with(ExpectedActualResolver) {
        val descriptor = this@findExpectForActual

        if (!descriptor.isActual) error(this) else {
            findCompatibleExpectedForActual(descriptor.module).singleOrNull()
        }
    } as T?

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
                parent.findActualForExpected()!!.thisReceiver!!
            }

            is IrFunction -> when (parameter) {
                parent.dispatchReceiverParameter ->
                    parent.findActualForExpected()!!.dispatchReceiverParameter!!
                parent.extensionReceiverParameter ->
                    parent.findActualForExpected()!!.extensionReceiverParameter!!
                else -> {
                    assert(parent.valueParameters[parameter.index] == parameter)
                    parent.findActualForExpected()!!.valueParameters[parameter.index]
                }
            }

            else -> error(parent)
        }
    }
}
