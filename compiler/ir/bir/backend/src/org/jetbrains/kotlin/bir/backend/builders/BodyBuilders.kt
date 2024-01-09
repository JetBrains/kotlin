/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package org.jetbrains.kotlin.bir.backend.builders

import org.jetbrains.kotlin.bir.BirStatement
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.expressions.BirBlockBody
import org.jetbrains.kotlin.bir.expressions.BirContainerExpression
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.expressions.impl.BirBlockBodyImpl
import org.jetbrains.kotlin.bir.expressions.impl.BirBlockImpl
import org.jetbrains.kotlin.bir.expressions.impl.BirCompositeImpl
import org.jetbrains.kotlin.bir.expressions.impl.BirExpressionBodyImpl
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

/*fun birBlockBody(
    sourceSpan: SourceSpan,
    builder: BirBlockBodyBuilderScope.() -> Unit
): BirBlockBody {
    val body = BirBlockBodyImpl(sourceSpan)
    builder(BirBlockBodyBuilderScope(body))
    return body
}*/

/*context(BirBackendContext)
inline fun birBlockBody(
    crossinline builder: context(BirStatementBuilderScope) BirBlockBodyBuilderScope.() -> Unit,
): BirBlockBody {
    val statementsBuilder = BirStatementBuilderScope()
    with(statementsBuilder) {
        val body: BirBlockBodyBuilderScope.() -> Unit = { builder(statementsBuilder, this) }
        return birBlockBody(body)
    }
}*/

context(BirStatementBuilderScope)
inline fun birBlockBody(
    builder: BirBlockBodyBuilderScope.() -> Unit,
): BirBlockBody {
    val body = BirBlockBodyImpl(sourceSpan)
    builder(BirBlockBodyBuilderScope(body))
    return body
}

context(BirBackendContext)
inline fun birBlock(
    resultType: BirType? = null,
    isTransparent: Boolean = false,
    crossinline builder: context(BirStatementBuilderScope) BirBlockBuilderScope.() -> Unit,
): BirContainerExpression {
    val statementsBuilder = BirStatementBuilderScope()
    with(statementsBuilder) {
        val body: BirBlockBuilderScope.() -> Unit = { builder(statementsBuilder, this) }
        return birBlock(resultType, isTransparent, body)
    }
}

context(BirBackendContext, BirStatementBuilderScope)
inline fun birBlock(
    resultType: BirType? = null,
    isTransparent: Boolean = false,
    builder: BirBlockBuilderScope.() -> Unit,
): BirContainerExpression {
    val scope = BirBlockBuilderScope()
    builder(scope)
    return scope.build(sourceSpan, origin, resultType, isTransparent)
}


context(BirBackendContext)
inline fun birExpressionBody(
    builder: BirStatementBuilderScope.() -> BirExpression,
): BirExpressionBody {
    val scope = BirStatementBuilderScope()
    val expr = builder(scope)
    return with(scope) {
        birExpressionBody(expr)
    }
}

context(BirBackendContext, BirStatementBuilderScope)
inline fun birExpressionBody(
    expression: BirExpression,
): BirExpressionBody =
    BirExpressionBodyImpl(sourceSpan, expression)

class BirBlockBodyBuilderScope(
    private val blockBody: BirBlockBodyImpl,
) {
    operator fun <E : BirStatement> E.unaryPlus(): E {
        blockBody.statements.add(this)
        return this
    }

    operator fun Iterable<BirStatement>.unaryPlus() {
        blockBody.statements.addAll(this)
    }
}


class BirBlockBuilderScope() {
    private val statements = mutableListOf<BirStatement>()

    operator fun <E : BirStatement> E.unaryPlus(): E {
        statements.add(this)
        return this
    }

    operator fun Iterable<BirStatement>.unaryPlus() {
        statements.addAll(this)
    }

    context(BirBackendContext)
    fun build(
        sourceSpan: SourceSpan,
        origin: IrStatementOrigin?,
        resultType: BirType?,
        isTransparent: Boolean,
    ): BirContainerExpression {
        val resultType = resultType
            ?: (statements.lastOrNull() as? BirExpression)?.type
            ?: birBuiltIns.unitType
        val birBlock =
            if (isTransparent) BirCompositeImpl(sourceSpan, resultType, origin)
            else BirBlockImpl(sourceSpan, resultType, origin)
        birBlock.statements.addAll(statements)
        return birBlock
    }
}