/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.FrontendKlibSymbols
import org.jetbrains.kotlin.backend.common.ir.KlibSymbols
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Replaces the usages of `SharedVariableBox<Int>` types with `SharedVariableBoxInt` to avoid double boxing for shared variables
 * of primitive types.
 */
class SharedVariablesPrimitiveBoxSpecializationLowering(
    private val context: CommonBackendContext,
    private val symbols: KlibSymbols,
) : BodyLoweringPass {
    private val genericSharedVariableBox = symbols.genericSharedVariableBox

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(Transformer())
    }

    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, withLocalDeclarations = true)
    }

    private fun IrType.getPrimitiveBoxClassIfPossible(): FrontendKlibSymbols.SharedVariableBoxClassInfo? {
        if (this !is IrSimpleType) return null
        if (classifier != genericSharedVariableBox.klass) return null
        val argument = arguments.getOrNull(0)?.typeOrNull ?: return null
        return symbols.primitiveSharedVariableBoxes[argument]
    }

    private fun IrType.replaceIfNeeded(): IrType = getPrimitiveBoxClassIfPossible()?.klass?.defaultType ?: this

    private inner class Transformer : IrElementTransformerVoid() {
        override fun visitVariable(declaration: IrVariable): IrStatement {
            declaration.type = declaration.type.replaceIfNeeded()
            return super.visitVariable(declaration)
        }

        override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
            declaration.type = declaration.type.replaceIfNeeded()
            return super.visitValueParameter(declaration)
        }

        override fun visitField(declaration: IrField): IrStatement {
            declaration.type = declaration.type.replaceIfNeeded()
            return super.visitField(declaration)
        }

        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
            declaration.type = declaration.type.replaceIfNeeded()
            return super.visitLocalDelegatedProperty(declaration)
        }

        override fun visitFunction(declaration: IrFunction): IrStatement {
            declaration.returnType = declaration.returnType.replaceIfNeeded()
            return super.visitFunction(declaration)
        }

        override fun visitExpression(expression: IrExpression): IrExpression {
            expression.type = expression.type.replaceIfNeeded()
            return super.visitExpression(expression)
        }

        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            if (expression.symbol != genericSharedVariableBox.constructor) {
                return super.visitConstructorCall(expression)
            }
            val primitiveBoxClass = expression.type.getPrimitiveBoxClassIfPossible() ?: return super.visitConstructorCall(expression)
            return IrConstructorCallImpl(
                expression.startOffset,
                expression.endOffset,
                primitiveBoxClass.klass.defaultType,
                primitiveBoxClass.constructor,
                typeArgumentsCount = 0,
                constructorTypeArgumentsCount = 0,
                expression.origin,
            ).apply {
                arguments[0] = expression.arguments[0]?.transform(this@Transformer, null)
            }
        }

        override fun visitCall(expression: IrCall): IrExpression {
            val primitiveBoxClass =
                expression.arguments.getOrNull(0)?.type?.getPrimitiveBoxClassIfPossible() ?: return super.visitCall(expression)
            if (expression.symbol == genericSharedVariableBox.load) {
                return IrCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    primitiveBoxClass.load,
                    origin = expression.origin,
                ).apply {
                    arguments[0] = expression.arguments[0]?.transform(this@Transformer, null)
                }
            }
            if (expression.symbol == genericSharedVariableBox.store) {
                return IrCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    context.irBuiltIns.unitType,
                    primitiveBoxClass.store,
                    origin = expression.origin,
                ).apply {
                    arguments[0] = expression.arguments[0]?.transform(this@Transformer, null)
                    arguments[1] = expression.arguments[1]?.transform(this@Transformer, null)
                }
            }
            return super.visitCall(expression)
        }
    }
}
