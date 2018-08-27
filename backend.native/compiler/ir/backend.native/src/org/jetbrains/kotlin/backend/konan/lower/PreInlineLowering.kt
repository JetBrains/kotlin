/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This pass runs before inlining and performs the following additional transformations over some operations:
 *     - Assertion call removal.
 */
internal class PreInlineLowering(val context: Context) : FileLoweringPass {

    private val symbols get() = context.ir.symbols

    private val asserts = symbols.asserts
    private val enableAssertions = context.config.configuration.getBoolean(KonanConfigKeys.ENABLE_ASSERTIONS)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrBuildingTransformer(context) {

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                // Replace assert() call with an empty composite if assertions are not enabled.
                if (!enableAssertions && expression.symbol in asserts) {
                    assert(expression.type.isUnit())
                    return IrCompositeImpl(expression.startOffset, expression.endOffset, expression.type)
                }

                return expression
            }
        })
    }
}