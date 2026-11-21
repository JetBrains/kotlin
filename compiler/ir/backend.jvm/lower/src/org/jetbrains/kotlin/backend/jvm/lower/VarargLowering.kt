/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.isArrayOfOrArrayDotOf
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.IrArrayBuilder
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArray
import org.jetbrains.kotlin.backend.jvm.ir.irArrayOf
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.*

/**
 * Replaces varargs with array arguments, and lowers [arrayOf] and [emptyArray] calls.
 */
@PhasePrerequisites(PolymorphicSignatureLowering::class)
internal class VarargLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    // Ignore annotations
    override fun visitAnnotation(expression: IrAnnotation): IrExpression {
        return expression
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        expression.transformChildrenVoid()
        val function = expression.symbol

        // Replace empty varargs with empty arrays
        val irFunction = function.owner
        for ([parameter, argument] in irFunction.parameters zip expression.arguments) {
            if (argument != null) continue

            if (parameter.varargElementType != null && !parameter.hasDefaultValue()) {
                // Compute the correct type for the array argument.
                val arrayType = parameter.type.substitute(expression.typeSubstitutionMap).makeNotNull()
                // Cannot use `expression.arguments[parameter]` to get argument
                // because of the different owner in the case of polymorphic varargs calls (e.g. `invokeExact` call)
                expression.arguments[parameter.indexInParameters] = createBuilder().irArrayOf(arrayType)
            }
        }

        return expression
    }

    override fun visitVararg(expression: IrVararg): IrExpression =
        createBuilder(expression.startOffset, expression.endOffset).irArray(expression.type) { addVararg(expression) }

    private fun IrArrayBuilder.addVararg(expression: IrVararg) {
        loop@ for (element in expression.elements) {
            when (element) {
                is IrExpression -> +element.transform(this@VarargLowering, null)
                is IrSpreadElement -> {
                    val spread = element.expression
                    if (spread is IrFunctionAccessExpression && spread.symbol.owner.isArrayOfOrArrayDotOf()) {
                        // Skip empty arrays and don't copy immediately created arrays
                        val argument = spread.arguments[0] ?: continue@loop
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
        context.createJvmIrBuilder(currentScope!!, startOffset, endOffset)

}

internal fun IrFunction.isEmptyArray(): Boolean = isTopLevelInPackage("emptyArray", StandardNames.BUILT_INS_PACKAGE_FQ_NAME)
