package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isExpectMember
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver

/**
 * This pass removes all declarations with `isExpect == true`.
 */
internal class ExpectDeclarationsRemoving(val context: Context) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        // All declarations with `isExpect == true` are nested into a top-level declaration with `isExpect == true`.
        irFile.declarations.removeAll {
            if (it.descriptor.isExpectMember) {
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

                function.findActualForExpected().valueParameters[index].defaultValue = defaultValue.also {
                    it.expression = it.expression.remapExpectValueSymbols()
                }
            }
        })
    }

    private fun IrFunction.findActualForExpected(): IrFunction =
            context.ir.symbols.symbolTable.referenceFunction(descriptor.findActualForExpect()).owner

    private fun IrClass.findActualForExpected(): IrClass =
            context.ir.symbols.symbolTable.referenceClass(descriptor.findActualForExpect()).owner

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

            is IrFunction -> when {
                parameter == parent.dispatchReceiverParameter ->
                    parent.findActualForExpected().dispatchReceiverParameter!!

                parameter == parent.extensionReceiverParameter ->
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
