/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.PreSerializationKlibSymbols
import org.jetbrains.kotlin.backend.common.ir.BackendKlibSymbols
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
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
@PhasePrerequisites(SharedVariablesLowering::class)
class SharedVariablesPrimitiveBoxSpecializationLowering(
    private val context: CommonBackendContext,
    private val symbols: BackendKlibSymbols,
) : BodyLoweringPass {
    private val genericSharedVariableBox = symbols.genericSharedVariableBox

    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }

    override fun lower(irFile: IrFile) { TODO() }

    private fun IrType.getPrimitiveBoxClassIfPossible(): PreSerializationKlibSymbols.SharedVariableBoxClassInfo? { TODO() }

    private fun IrType.replaceIfNeeded(): IrType = TODO()

    private inner class Transformer : IrElementTransformerVoid() {
        override fun visitVariable(declaration: IrVariable): IrStatement { TODO() }

        override fun visitValueParameter(declaration: IrValueParameter): IrStatement { TODO() }

        override fun visitField(declaration: IrField): IrStatement { TODO() }

        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement { TODO() }

        override fun visitFunction(declaration: IrFunction): IrStatement { TODO() }

        override fun visitExpression(expression: IrExpression): IrExpression { TODO() }

        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression { TODO() }

        override fun visitCall(expression: IrCall): IrExpression { TODO() }
    }
}
