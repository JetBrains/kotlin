/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.js.backend.ast.*

class SwitchOptimizer(
    private val context: JsGenerationContext,
    private val isExpression: Boolean,
    private val lastStatementTransformer: (() -> JsStatement) -> JsStatement
) {

    // TODO: reimplement optimization on top of IR
    constructor(context: JsGenerationContext) : this(context, isExpression = false, { it() })

    private val jsEqeqeq = context.staticContext.backendContext.intrinsics.jsEqeqeq
    private val jsEqeq = context.staticContext.backendContext.intrinsics.jsEqeq

    private fun IrConst<*>.isTrueConstant(): Boolean {
        if (kind !== IrConstKind.Boolean) return false
        return value as Boolean
    }

    private sealed class SwitchBranchData(val body: IrExpression) {
        class SwitchCaseData(val cases: Collection<IrConst<*>>, body: IrExpression) : SwitchBranchData(body)
        class SwitchDefaultData(body: IrExpression) : SwitchBranchData(body)
    }

    private class SwitchData(val subject: IrValueSymbol, val cases: Collection<SwitchBranchData>)

    private fun detectSwitch(expression: IrWhen): SwitchData? {
        /* to be a switch-expression, branches have to meet following requirements
         * 1. Any comparison has to be `strict` i.e. `===`
         * 2. Type of operand has to be either string or number
         * 3. One of `===` operands has to be a constant
         * 4. Another operand has to be a same variable
         */

        var varSymbol: IrValueSymbol? = null

        val cases = mutableListOf<SwitchBranchData>()

        fun tryToExtractEqeqeqConst(irCall: IrCall): IrConst<*>? {
            // check weather the irCall is `s === #CONST`
            if (irCall.symbol !== jsEqeqeq && irCall.symbol !== jsEqeq) return null

            val op1 = irCall.getValueArgument(0)!!
            val op2 = irCall.getValueArgument(1)!!

            val constOp = op1 as? IrConst<*> ?: op2 as? IrConst<*> ?: return null
            val varOp = op1 as? IrGetValue ?: op2 as? IrGetValue ?: return null

            if (varSymbol == null) varSymbol = varOp.symbol
            if (varSymbol !== varOp.symbol) return null

            return constOp
        }

        fun checkForPrimitiveOrPattern(irWhen: IrWhen, constants: MutableList<IrConst<*>>): Boolean {
            if (irWhen.branches.size != 2) return false

            val thenBranch = irWhen.branches[0]
            val elseBranch = irWhen.branches[1]

            fun checkBranchIsOrPattern(constExpr: IrExpression, branchExpr: IrExpression): Boolean {
                if (constExpr !is IrConst<*>) return false
                if (!constExpr.isTrueConstant()) return false

                return when (branchExpr) {
                    is IrWhen -> checkForPrimitiveOrPattern(branchExpr, constants)
                    is IrCall -> when (val constant = tryToExtractEqeqeqConst(branchExpr)) {
                        null -> false
                        else -> {
                            constants += constant
                            true
                        }
                    }
                    else -> false
                }
            }

            if (!checkBranchIsOrPattern(thenBranch.result, thenBranch.condition)) return false
            if (!checkBranchIsOrPattern(elseBranch.condition, elseBranch.result)) return false

            return true
        }

        var caseCount = 0

        l@ for (branch in expression.branches) {
            when (val condition = branch.condition) {
                is IrCall -> {
                    val constant = tryToExtractEqeqeqConst(condition) ?: return null
                    caseCount++
                    cases += SwitchBranchData.SwitchCaseData(listOf(constant), branch.result)
                }

                // check for a || b ... || z pattern
                is IrWhen -> {
                    val orConstants = mutableListOf<IrConst<*>>()
                    if (checkForPrimitiveOrPattern(condition, orConstants)) {
                        caseCount += orConstants.size
                        cases += SwitchBranchData.SwitchCaseData(orConstants, branch.result)
                    } else return null
                }

                is IrConst<*> -> {
                    if (condition.isTrueConstant()) {
                        caseCount++
                        cases += SwitchBranchData.SwitchDefaultData(branch.result)
                        break@l
                    }
                }
                else -> return null
            }
        }

        val s = varSymbol

        // Seems it is not reasonable to optimize very simple when
        if (caseCount < 3) return null

        if (s?.owner?.type?.isSuitableForSwitch() == true) return SwitchData(s, cases)
        return null
    }

    private fun buildJsSwitch(switch: SwitchData): JsStatement {

        val exprTransformer = IrElementToJsExpressionTransformer()
        val stmtTransformer = IrElementToJsStatementTransformer()

        val jsExpr = context.getNameForValueDeclaration(switch.subject.owner).makeRef()

        val jsCases = mutableListOf<JsSwitchMember>()

        for (case in switch.cases) {
            val jsCase = if (case is SwitchBranchData.SwitchCaseData) {
                jsCases += case.cases.map { JsCase().apply { caseExpression = it.accept(exprTransformer, context) } }
                jsCases.last()
            } else {
                JsDefault().also { jsCases += it }
            }

            val lastStatement = if (isExpression) {
                val lastStatement = lastStatementTransformer { case.body.accept(exprTransformer, context).makeStmt() }
                jsCase.statements += lastStatement
                lastStatement
            } else {
                val jsBody = case.body.accept(stmtTransformer, context).asBlock()
                val lastStatement = jsBody.statements.lastOrNull()?.let { lastStatementTransformer { it } }

                jsCase.statements += jsBody.statements

                if (lastStatement != null) {
                    jsCase.statements[jsCase.statements.lastIndex] = lastStatement
                }

                lastStatement
            }

            if (lastStatement !is JsBreak && lastStatement !is JsContinue && lastStatement !is JsReturn && lastStatement !is JsThrow) {
                jsCase.statements += JsBreak()
            }
        }

        return JsSwitch(jsExpr, jsCases)
    }

    private fun IrType.isSuitableForSwitch(): Boolean {
        val notNullable = makeNotNull()

        // TODO: support inline-class based primitives (Char, UByte, UShort, UInt)
        return notNullable.run { isBoolean() || isByte() || isShort() || isInt() || isFloat() || isDouble() || isString() }
    }


    fun tryOptimize(irWhen: IrWhen): JsStatement? {
        return detectSwitch(irWhen)?.let { buildJsSwitch(it).withSource(irWhen, context) }
    }

}
