package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrContainerExpressionBase
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBase
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

//-----------------------------------------------------------------------------//

class IrInlineFunctionBody(startOffset: Int, endOffset: Int, type: KotlinType, origin: IrStatementOrigin? = null):
    IrContainerExpressionBase(startOffset, endOffset, type, origin), IrBlock {
    constructor(startOffset: Int, endOffset: Int, type: KotlinType, origin: IrStatementOrigin?, statements: List<IrStatement>) :
        this(startOffset, endOffset, type, origin) {
        this.statements.addAll(statements)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitBlock(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        statements.forEachIndexed { i, irStatement ->
            statements[i] = irStatement.transform(transformer, data)
        }
    }
}

//-----------------------------------------------------------------------------//

