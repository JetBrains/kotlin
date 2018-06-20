/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.symbols.JsSymbolBuilder
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCatchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

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
 * catch (ex: Ex = LCA(Ex1, Ex2, Ex3) {
 *   when (ex) {
 *     ex is Ex1 -> catch1((Ex1)ex)
 *     ex is Ex2 -> catch2((Ex2)ex)
 *     ex is Ex3 -> catch3((Ex3)ex)
 *     else throw ex [ | catch_dynamic(ex) ]
 *   }
 * }
 * finally {}
 */

class MultipleCatchesLowering(val context: JsIrBackendContext) : FileLoweringPass {
    val litTrue = JsIrBuilder.buildBoolean(context.irBuiltIns.bool, true)
    val unitType = context.irBuiltIns.unit
    val nothingType = context.irBuiltIns.nothing

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(object : IrElementTransformer<IrDeclaration?> {

            override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclaration?) =
                super.visitDeclaration(declaration, declaration)

            override fun visitTry(aTry: IrTry, data: IrDeclaration?): IrExpression {
                aTry.transformChildren(this, data)

                if (aTry.catches.isEmpty()) return aTry.apply { assert(finallyExpression != null) }

                val commonType = mergeTypes(aTry.catches.map { it.catchParameter.type })

                val pendingExceptionSymbol = JsSymbolBuilder.buildVar(data!!.descriptor, commonType, "\$pending\$", false)
                val pendingExceptionDeclaration = JsIrBuilder.buildVar(pendingExceptionSymbol)
                val pendingException = JsIrBuilder.buildGetValue(pendingExceptionSymbol)

                val branches = mutableListOf<IrBranch>()

                for (catch in aTry.catches) {
                    assert(!catch.catchParameter.isVar) { "caught exception parameter has to immutable" }
                    val type = catch.catchParameter.type
                    val typeSymbol = context.symbolTable.referenceClassifier(type.constructor.declarationDescriptor!!)
                    val castedPendingException = buildImplicitCast(pendingException, type, typeSymbol)
                    val catchBody = catch.result.transform(object : IrElementTransformer<VariableDescriptor> {
                        override fun visitGetValue(expression: IrGetValue, data: VariableDescriptor) =
                            if (expression.descriptor == data) castedPendingException else expression
                    }, catch.parameter)

                    if (type.isDynamic()) {
                        branches += IrElseBranchImpl(catch.startOffset, catch.endOffset, litTrue, catchBody)
                        break
                    } else {
                        val typeCheck = buildIsCheck(pendingException, type, typeSymbol)
                        branches += IrBranchImpl(catch.startOffset, catch.endOffset, typeCheck, catchBody)
                    }
                }


                if (!commonType.isDynamic()) {
                    val throwStatement = JsIrBuilder.buildThrow(nothingType, pendingException)
                    branches += IrElseBranchImpl(litTrue, JsIrBuilder.buildBlock(nothingType, listOf(throwStatement)))
                }

                val whenStatement = JsIrBuilder.buildWhen(aTry.type, branches)

                val newCatch = aTry.run {
                    IrCatchImpl(catches.first().startOffset, catches.last().endOffset, pendingExceptionDeclaration, whenStatement)
                }

                return aTry.run { IrTryImpl(startOffset, endOffset, type, tryResult, listOf(newCatch), finallyExpression) }
            }

            private fun buildIsCheck(value: IrExpression, toType: KotlinType, toTypeSymbol: IrClassifierSymbol) =
                JsIrBuilder.buildTypeOperator(context.irBuiltIns.bool, IrTypeOperator.INSTANCEOF, value, toType, toTypeSymbol)

            private fun buildImplicitCast(value: IrExpression, toType: KotlinType, toTypeSymbol: IrClassifierSymbol) =
                JsIrBuilder.buildTypeOperator(toType, IrTypeOperator.IMPLICIT_CAST, value, toType, toTypeSymbol)

            private fun mergeTypes(types: List<KotlinType>) = CommonSupertypes.commonSupertype(types).also {
                assert(it.isSubtypeOf(context.builtIns.throwable.defaultType) || it.isDynamic())
            }

        }, null)
    }
}