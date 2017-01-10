package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.types.KotlinType

class IrLoweringContext(backendContext: BackendContext) : IrGeneratorContext(backendContext.irBuiltIns)

class FunctionIrGenerator(backendContext: BackendContext, functionDescriptor: FunctionDescriptor) : IrGeneratorWithScope {
    override val context = IrLoweringContext(backendContext)
    override val scope = Scope(functionDescriptor)
}

fun BackendContext.createFunctionIrGenerator(functionDescriptor: FunctionDescriptor) =
        FunctionIrGenerator(this, functionDescriptor)

class FunctionIrBuilder(context: IrLoweringContext, scope: Scope, startOffset: Int, endOffset: Int) :
        IrBuilderWithScope(context, scope, startOffset, endOffset)

fun FunctionIrGenerator.createIrBuilder() = FunctionIrBuilder(context, scope, UNDEFINED_OFFSET, UNDEFINED_OFFSET)

/**
 * Builds [IrBlock] to be used instead of given expression.
 */
inline fun FunctionIrGenerator.irBlock(expression: IrExpression, origin: IrStatementOrigin? = null,
                                       resultType: KotlinType? = expression.type,
                                       body: IrBlockBuilder.() -> Unit) =
        this.irBlock(expression.startOffset, expression.endOffset, origin, resultType, body)

inline fun FunctionIrGenerator.irBlockBody(irElement: IrElement, body: IrBlockBodyBuilder.() -> Unit) =
        this.irBlockBody(irElement.startOffset, irElement.endOffset, body)