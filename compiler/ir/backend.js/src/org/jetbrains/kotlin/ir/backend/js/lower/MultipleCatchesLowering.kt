/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCatchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

/**
 * Since JS does not support multiple catch blocks by default we should replace them with similar `when` statement, so
 *
 * try {}
 * catch (ex1: Ex1) { catch1(ex1) }
 * catch (ex2: Ex2) { catch2(ex2) }
 * catch (ex3: Ex3) { catch3(ex3) }
 * [catch (exd: dynamic) { catch_dynamic(exd) } ]
 * finally {}
 *
 * is converted into
 *
 * try {}
 * catch ($p: dynamic) {
 *   when ($p) {
 *     ex is Ex1 -> catch1((Ex1)$p)
 *     ex is Ex2 -> catch2((Ex2)$p)
 *     ex is Ex3 -> catch3((Ex3)$p)
 *     else throw $p [ | catch_dynamic($p) ]
 *   }
 * }
 * finally {}
 */

class MultipleCatchesLowering(private val context: JsIrBackendContext) : FileLoweringPass {
    private val litTrue get() = JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true)
    private val nothingType = context.irBuiltIns.nothingType

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(object : IrElementTransformer<IrDeclarationParent> {

            override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclarationParent): IrStatement {
                val parent = (declaration as? IrDeclarationParent) ?: data
                return super.visitDeclaration(declaration, parent)
            }

            override fun visitTry(aTry: IrTry, data: IrDeclarationParent): IrExpression {
                aTry.transformChildren(this, data)

                if (aTry.catches.isEmpty()) return aTry.also { assert(it.finallyExpression != null) }

                val pendingExceptionDeclaration = JsIrBuilder.buildVar(context.dynamicType, data, "\$p")
                val pendingException = { JsIrBuilder.buildGetValue(pendingExceptionDeclaration.symbol) }

                val branches = mutableListOf<IrBranch>()
                var isCaughtDynamic = false

                for (catch in aTry.catches) {
                    assert(!catch.catchParameter.isVar) { "caught exception parameter has to be immutable" }
                    val type = catch.catchParameter.type

                    val castedPendingException = {
                        if (type !is IrDynamicType)
                            buildImplicitCast(pendingException(), type)
                        else
                            pendingException()
                    }

                    val catchBody = catch.result.transform(object : IrElementTransformer<IrValueSymbol> {
                        override fun visitGetValue(expression: IrGetValue, data: IrValueSymbol) =
                            if (expression.symbol == data)
                                castedPendingException()
                            else
                                expression
                    }, catch.catchParameter.symbol)

                    if (type is IrDynamicType) {
                        branches += IrElseBranchImpl(catch.startOffset, catch.endOffset, litTrue, catchBody)
                        isCaughtDynamic = true
                        break
                    } else {
                        val typeCheck = buildIsCheck(pendingException(), type)
                        branches += IrBranchImpl(catch.startOffset, catch.endOffset, typeCheck, catchBody)
                    }
                }

                if (!isCaughtDynamic) {
                    val throwStatement = JsIrBuilder.buildThrow(nothingType, pendingException())
                    branches += IrElseBranchImpl(litTrue, JsIrBuilder.buildBlock(nothingType, listOf(throwStatement)))
                }

                val whenStatement = JsIrBuilder.buildWhen(aTry.type, branches)

                val newCatch = aTry.run {
                    IrCatchImpl(catches.first().startOffset, catches.last().endOffset, pendingExceptionDeclaration, whenStatement)
                }

                return aTry.run { IrTryImpl(startOffset, endOffset, type, tryResult, listOf(newCatch), finallyExpression) }
            }

            private fun buildIsCheck(value: IrExpression, toType: IrType) =
                JsIrBuilder.buildTypeOperator(context.irBuiltIns.booleanType, IrTypeOperator.INSTANCEOF, value, toType)

            private fun buildImplicitCast(value: IrExpression, toType: IrType) =
                JsIrBuilder.buildTypeOperator(toType, IrTypeOperator.IMPLICIT_CAST, value, toType)

        }, irFile)
    }
}