package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.typeUtil.isUnit

/**
 * This pass runs before inlining and performs the following additional transformations over some calls:
 *     - Assertion call removal.
 */
internal class SpecialCallsLowering(val context: Context) : FileLoweringPass {

    private val asserts = context.ir.symbols.asserts
    private val enableAssertions = context.config.configuration.getBoolean(KonanConfigKeys.ENABLE_ASSERTIONS)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {

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