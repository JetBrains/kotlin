/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.IrArrayBuilder
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArray
import org.jetbrains.kotlin.backend.jvm.ir.irArrayOf
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.*

val varargPhase = makeIrFilePhase(
    ::VarargLowering,
    name = "VarargLowering",
    description = "Replace varargs with array arguments and lower arrayOf calls"
)

private class VarargLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    // Ignore annotations
    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val constructor = expression.symbol.owner
        if (constructor.constructedClass.isAnnotationClass)
            return expression
        return super.visitConstructorCall(expression)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        expression.transformChildrenVoid()
        val function = expression.symbol

        // Replace empty varargs with empty arrays
        for (i in 0 until expression.valueArgumentsCount) {
            if (expression.getValueArgument(i) != null)
                continue

            val parameter = function.owner.valueParameters[i]
            if (parameter.varargElementType != null && !parameter.hasDefaultValue()) {
                // Compute the correct type for the array argument.
                val arrayType = parameter.type.substitute(expression.typeSubstitutionMap)
                expression.putValueArgument(i, createBuilder().irArrayOf(arrayType))
            }
        }

        // Lower `arrayOf` calls. When `isArrayOf` returns true we know that the function has exactly one
        // vararg parameter. Meanwhile, the code above ensures that the corresponding argument is not null.
        return if (function.isArrayOf) expression.getValueArgument(0)!! else expression
    }

    override fun visitVararg(expression: IrVararg): IrExpression =
        createBuilder(expression.startOffset, expression.endOffset).irArray(expression.type) { addVararg(expression) }

    private fun IrArrayBuilder.addVararg(expression: IrVararg) {
        loop@ for (element in expression.elements) {
            when (element) {
                is IrExpression -> +element.transform(this@VarargLowering, null)
                is IrSpreadElement -> {
                    val spread = element.expression
                    if (spread is IrFunctionAccessExpression && spread.symbol.isArrayOf) {
                        // Skip empty arrays and don't copy immediately created arrays
                        val argument = spread.getValueArgument(0) ?: continue@loop
                        if (argument is IrVararg) {
                            addVararg(argument)
                            continue@loop
                        }
                    }
                    addSpread(spread.transform(this@VarargLowering, null))
                }
                else -> error("Unexpected IrVarargElement subclass: $element")
            }
        }
    }

    private fun createBuilder(startOffset: Int = UNDEFINED_OFFSET, endOffset: Int = UNDEFINED_OFFSET) =
        context.createJvmIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)

    private val IrFunctionSymbol.isArrayOf
        get() = this == context.ir.symbols.arrayOf || owner.isPrimitiveArrayOf

    companion object {
        private val PRIMITIVE_ARRAY_OF_NAMES: Set<String> =
            (PrimitiveType.values().map { type -> type.name } + UnsignedType.values().map { type -> type.typeName.asString() })
                .map { name -> name.toLowerCase() + "ArrayOf" }.toSet()

        private val IrFunction.isPrimitiveArrayOf: Boolean
            get() {
                val parent = when (val directParent = parent) {
                    is IrClass -> directParent.getPackageFragment() ?: return false
                    is IrPackageFragment -> directParent
                    else -> return false
                }
                return parent.fqName == KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME &&
                        name.asString() in PRIMITIVE_ARRAY_OF_NAMES &&
                        extensionReceiverParameter == null &&
                        dispatchReceiverParameter == null &&
                        valueParameters.size == 1 &&
                        valueParameters[0].isVararg
            }
    }
}
