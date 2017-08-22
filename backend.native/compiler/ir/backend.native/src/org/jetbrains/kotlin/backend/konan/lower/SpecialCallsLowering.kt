package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.typeUtil.isUnit

/**
 * This pass runs before inlining and performs the following additional transformations over some calls:
 *     - Assertion call removal.
 *     - Convert immutableBinaryBlobOf() arguments to special IrConst.
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

                if (expression.symbol == context.ir.symbols.immutableBinaryBlobOf) {
                    // Convert arguments of the binary blob to special IrConst<String> structure, so that
                    // vararg lowering will not affect it.
                    val args = expression.getValueArgument(0) as? IrVararg
                    if (args == null) throw Error("varargs shall not be lowered yet")
                    if (args.elements.any { it is IrSpreadElement }) {
                        context.reportCompilationError("no spread elements allowed here", irFile, args)
                    }
                    val builder = StringBuilder()
                    args.elements.forEach {
                        if (it !is IrConst<*>) {
                            context.reportCompilationError(
                                    "all elements of binary blob must be constants", irFile, it)
                        }
                        val value = when (it.kind) {
                            IrConstKind.Short ->  (it.value as Short).toInt()
                            else ->
                                context.reportCompilationError("incorrect value for binary data: $it.value", irFile, it)
                        }
                        if (value < 0 || value > 0xff)
                            context.reportCompilationError("incorrect value for binary data: $value", irFile, it)
                        // Luckily, all values in range 0x00 .. 0xff represent valid UTF-16 symbols,
                        // block 0 (Basic Latin) and block 1 (Latin-1 Supplement) in
                        // Basic Multilingual Plane, so we could just append data "as is".
                        builder.append(value.toChar())
                    }
                    expression.putValueArgument(0, IrConstImpl<String>(
                            expression.startOffset, expression.endOffset,
                            context.ir.symbols.immutableBinaryBlob.descriptor.defaultType,
                            IrConstKind.String, builder.toString()))
                }

                return expression
            }
        })
    }
}