/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrWhileLoopImpl
import org.jetbrains.kotlin.ir.util.implicitCastIfNeededTo

class IndexedGetLoopHeader(
    headerInfo: IndexedGetHeaderInfo,
    builder: DeclarationIrBuilder,
    context: CommonBackendContext
) : NumericForLoopHeader<IndexedGetHeaderInfo>(headerInfo, builder, context) {

    private val preferJavaLikeCounterLoop = context.preferJavaLikeCounterLoop
    private val javaLikeCounterLoopBuilder = JavaLikeCounterLoopBuilder(context)

    override val loopInitStatements =
        listOfNotNull(headerInfo.objectVariable, inductionVariable, lastVariableIfCanCacheLast, stepVariable)

    override fun initializeIteration(
        loopVariables: List<IrVariable>,
        loopVariableComponents: Map<Int, List<IrVariable>>,
        builder: DeclarationIrBuilder,
        backendContext: CommonBackendContext,
    ): List<IrStatement> = TODO()

    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?): LoopReplacement = TODO()
}