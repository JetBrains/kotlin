/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAssignmentOperatorStatement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.impl.FirAssignmentOperatorStatementImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirAssignmentOperatorStatementBuilder : FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    lateinit var operation: FirOperation
    lateinit var leftArgument: FirExpression
    lateinit var rightArgument: FirExpression

    override fun build(): FirAssignmentOperatorStatement {
        return FirAssignmentOperatorStatementImpl(
            source,
            annotations,
            operation,
            leftArgument,
            rightArgument,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildAssignmentOperatorStatement(init: FirAssignmentOperatorStatementBuilder.() -> Unit): FirAssignmentOperatorStatement {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirAssignmentOperatorStatementBuilder().apply(init).build()
}
