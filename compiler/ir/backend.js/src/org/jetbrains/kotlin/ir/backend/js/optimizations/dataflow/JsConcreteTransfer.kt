/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.isFalseConst
import org.jetbrains.kotlin.ir.util.isTrueConst
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Concrete value/type transfer over a [JsBasicBlock].
 *
 * Modeled IR nodes update [FactEnv] precisely. Every unmodelled statement goes through
 * [killWritesIn] so assignments are never silently ignored.
 *
 * [TransferFunction.transfer] returns a (possibly narrowed) OUT state per successor for
 * `is` / `==` / `!=` / null checks and `when` subjects.
 */
class JsConcreteTransfer : TransferFunction<FactEnv> {

    override fun transfer(block: JsBasicBlock, inn: FactEnv): Map<JsBasicBlock, FactEnv> {
        val env = inn.copy()
        for (statement in block.statements) {
            transferStatement(statement, env)
        }
        return when (val t = block.terminator) {
            is JsTerminator.Goto -> mapOf(t.target to env)
            is JsTerminator.Cond -> {
                t.condition.evaluateWith(env)
                val thenEnv = env.copy()
                val elseEnv = env.copy()
                narrowCondition(t.condition, thenEnv, elseEnv)
                mapOf(t.thenTarget to thenEnv, t.elseTarget to elseEnv)
            }
            is JsTerminator.MultiCond -> {
                val result = mutableMapOf<JsBasicBlock, FactEnv>()
                for (branch in t.branches) {
                    val armEnv = env.copy()
                    branch.condition?.let { cond ->
                        cond.evaluateWith(armEnv)
                        // Taken arm: condition held; remaining arms treated independently.
                        narrowCondition(cond, thenEnv = armEnv, elseEnv = FactEnv())
                    }
                    val previous = result[branch.target]
                    result[branch.target] = previous?.join(armEnv) ?: armEnv
                }
                result
            }
            is JsTerminator.Return -> {
                t.value?.evaluateWith(env)
                emptyMap()
            }
            is JsTerminator.Throw -> {
                t.value.evaluateWith(env)
                emptyMap()
            }
            JsTerminator.Unreachable -> emptyMap()
        }
    }

    /**
     * Transfer statements of [block] from [inn] up to (but not including) [stopAt],
     * or through the whole block when [stopAt] is null.
     */
    fun transferUntil(block: JsBasicBlock, inn: FactEnv, stopAt: IrElement?): FactEnv {
        val env = inn.copy()
        for (statement in block.statements) {
            if (stopAt != null && statement === stopAt) break
            transferStatement(statement, env)
            if (stopAt != null && containsElement(statement, stopAt)) break
        }
        return env
    }

    fun transferStatement(statement: IrStatement, env: FactEnv) {
        when (statement) {
            is IrVariable -> {
                val init = statement.initializer
                env[statement] = init?.evaluateWith(env) ?: JsFact.Top
            }
            is IrSetValue -> {
                val target = statement.symbol.owner
                env[target] = statement.value.evaluateWith(env)
            }
            is IrExpression -> statement.evaluateWith(env)
            else -> killWritesIn(statement, env)
        }
    }

    /**
     * Strong-kill every local write inside [element] to [JsFact.Top].
     * Mandatory route for unmodelled IR — never a silent no-op on writes.
     */
    fun killWritesIn(element: IrElement, env: FactEnv) {
        element.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitVariable(declaration: IrVariable) {
                env[declaration] = JsFact.Top
                declaration.acceptChildrenVoid(this)
            }

            override fun visitSetValue(expression: IrSetValue) {
                env[expression.symbol.owner] = JsFact.Top
                expression.acceptChildrenVoid(this)
            }
        })
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrExpression.evaluateWith(env: FactEnv): JsFact {
        return when (this) {
            is IrConst -> toConstFact()
            is IrGetValue -> env[symbol.owner] ?: symbol.owner.type.toFact()
            is IrGetObjectValue -> {
                val irClass = symbol.owner
                if (type.isUnit()) {
                    JsFact(value = JsValueLattice.Unit, type = JsTypeLattice.exactOf(irClass))
                } else {
                    JsFact(value = JsValueLattice.Top, type = JsTypeLattice.exactOf(irClass))
                }
            }
            is IrGetEnumValue -> symbol.owner.toEnumFact()
            is IrConstructorCall -> {
                arguments.forEach { it?.evaluateWith(env) }
                val irClass = symbol.owner.constructedClass
                JsFact(value = JsValueLattice.Top, type = JsTypeLattice.exactOf(irClass))
            }
            is IrCall -> {
                arguments.forEach { it?.evaluateWith(env) }
                when {
                    type.isUnit() -> JsFact(value = JsValueLattice.Unit, type = JsTypeLattice.Top)
                    else -> type.toFact()
                }
            }
            is IrTypeOperatorCall -> {
                val argFact = argument.evaluateWith(env)
                when (operator) {
                    IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST -> {
                        val castType = typeOperand.toFact().type
                        val joined = argFact.type.join(castType)
                        val type = when {
                            argFact.type is JsTypeLattice.Exact && joined !is JsTypeLattice.Top -> argFact.type
                            else -> castType
                        }
                        JsFact(argFact.value, type)
                    }
                    IrTypeOperator.IMPLICIT_NOTNULL -> {
                        val value = when {
                            argFact.value.isNull() -> JsValueLattice.Top
                            else -> argFact.value
                        }
                        val type = when (val t = argFact.type) {
                            is JsTypeLattice.Exact -> t.copy(nullable = false)
                            is JsTypeLattice.UpperBound -> t.copy(nullable = false)
                            else -> t
                        }
                        JsFact(value, type)
                    }
                    IrTypeOperator.SAFE_CAST -> {
                        val bound = typeOperand.toFact()
                        JsFact(JsValueLattice.Top, bound.type)
                    }
                    IrTypeOperator.INSTANCEOF -> {
                        // Condition expression: value is Boolean Top; narrowing happens on edges.
                        JsFact(JsValueLattice.Top, JsTypeLattice.Top)
                    }
                    else -> argFact
                }
            }
            is IrWhen -> evaluateWhen(env)
            is IrBlock -> {
                var last: JsFact = JsFact.Top
                for (s in statements) {
                    last = when (s) {
                        is IrExpression -> s.evaluateWith(env)
                        else -> {
                            transferStatement(s, env)
                            JsFact.Top
                        }
                    }
                }
                last
            }
            is IrStringConcatenation -> {
                arguments.forEach { it.evaluateWith(env) }
                JsFact.Top
            }
            is IrVararg -> {
                elements.forEach { el -> if (el is IrExpression) el.evaluateWith(env) }
                JsFact.Top
            }
            is IrFunctionExpression, is IrRawFunctionReference, is IrFunctionReference -> JsFact.Top
            is IrClassReference -> JsFact.Top
            is IrGetField, is IrSetField -> {
                killWritesIn(element = this, env)
                JsFact.Top
            }
            is IrTry -> {
                killWritesIn(element = this, env)
                JsFact.Top
            }
            else -> {
                killWritesIn(element = this, env)
                JsFact.Top
            }
        }
    }

    private fun IrWhen.evaluateWhen(env: FactEnv): JsFact {
        val trueBranches = mutableListOf<IrBranch>()
        var sawNonConst = false
        for (branch in branches) {
            val condFact = branch.condition.evaluateWith(env)
            val constBool = condFact.value.asBooleanOrNull()
            when {
                branch.condition.isTrueConst() || constBool == true -> {
                    trueBranches += branch
                    break
                }
                branch.condition.isFalseConst() || constBool == false -> continue
                else -> {
                    sawNonConst = true
                    trueBranches += branch
                }
            }
        }
        if (!sawNonConst && trueBranches.size == 1) {
            return trueBranches[0].result.evaluateWith(env)
        }
        var joined = JsFact.Bottom
        for (branch in trueBranches) {
            joined = joined.join(branch.result.evaluateWith(env))
        }
        return if (joined == JsFact.Bottom) JsFact.Top else joined
    }

    /**
     * Refine [thenEnv] / [elseEnv] for a boolean condition (is / == / != / null).
     */
    fun narrowCondition(condition: IrExpression, thenEnv: FactEnv, elseEnv: FactEnv) {
        when (condition) {
            is IrTypeOperatorCall -> when (condition.operator) {
                IrTypeOperator.INSTANCEOF -> {
                    refineInstanceOf(condition.argument, condition.typeOperand, thenEnv, positive = true)
                    refineInstanceOf(condition.argument, condition.typeOperand, elseEnv, positive = false)
                }
                IrTypeOperator.NOT_INSTANCEOF -> {
                    refineInstanceOf(condition.argument, condition.typeOperand, thenEnv, positive = false)
                    refineInstanceOf(condition.argument, condition.typeOperand, elseEnv, positive = true)
                }
                else -> Unit
            }
            is IrCall -> narrowEqualityCall(condition, thenEnv, elseEnv)
            is IrGetValue -> {
                // Boolean local used as condition: then ⇒ true, else ⇒ false when Exact Bool possible.
                val irValue = condition.symbol.owner
                thenEnv[irValue] = JsFact(
                    value = JsValueLattice.Const(IrConstKind.Boolean, true),
                    type = thenEnv[irValue]?.type ?: JsTypeLattice.Top,
                )
                elseEnv[irValue] = JsFact(
                    value = JsValueLattice.Const(IrConstKind.Boolean, false),
                    type = elseEnv[irValue]?.type ?: JsTypeLattice.Top,
                )
            }
            else -> Unit
        }
    }

    private fun narrowEqualityCall(call: IrCall, thenEnv: FactEnv, elseEnv: FactEnv) {
        val name = call.symbol.owner.name.asString()
        if (name != "EQEQ" && name != "equals" && name != "ieee754equals") return
        val left = call.arguments.getOrNull(0) ?: return
        val right = call.arguments.getOrNull(1) ?: return
        narrowEquality(left, right, thenEnv, elseEnv)
    }

    private fun narrowEquality(
        left: IrExpression,
        right: IrExpression,
        thenEnv: FactEnv,
        elseEnv: FactEnv,
    ) {
        val (value, other = comparedTo) = when {
            left is IrGetValue -> EqualityOperands(left, right)
            right is IrGetValue -> EqualityOperands(right, left)
            else -> return
        }
        val target = value.symbol.owner
        when (other) {
            is IrConst if other.kind == IrConstKind.Null -> {
                val nullFact = JsFact(JsValueLattice.Null, JsTypeLattice.Top)
                thenEnv[target] = nullFact
                // else: not null — clear null const, mark non-nullable if Exact
                val old = elseEnv[target]
                if (old != null) {
                    elseEnv[target] = stripNullability(old)
                }
            }
            is IrConst -> {
                val constFact = other.toConstFact()
                thenEnv[target] = constFact
            }
            is IrGetEnumValue -> {
                val fact = other.symbol.owner.toEnumFact()
                thenEnv[target] = fact
            }
        }
    }

    private fun refineInstanceOf(
        argument: IrExpression,
        typeOperand: org.jetbrains.kotlin.ir.types.IrType,
        env: FactEnv,
        positive: Boolean,
    ) {
        val get = argument as? IrGetValue ?: return
        val irClass = typeOperand.classOrNull?.owner ?: return
        if (!positive) return // negative instanceof: leave as-is (conservative)
        val old = env[get.symbol.owner]
        env[get.symbol.owner] = JsFact(
            value = old?.value ?: JsValueLattice.Top,
            type = JsTypeLattice.exactOf(irClass, nullable = false),
        )
    }

    private fun stripNullability(fact: JsFact): JsFact {
        val type = when (val t = fact.type) {
            is JsTypeLattice.Exact -> t.copy(nullable = false)
            is JsTypeLattice.UpperBound -> t.copy(nullable = false)
            else -> t
        }
        val value = if (fact.value.isNull()) JsValueLattice.Top else fact.value
        return JsFact(value, type)
    }

    private fun containsElement(root: IrElement, target: IrElement): Boolean {
        var found = false
        root.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                if (element === target) {
                    found = true
                    return
                }
                if (!found) element.acceptChildrenVoid(this)
            }
        })
        return found
    }
}

private class EqualityOperands(
    val value: IrGetValue,
    val comparedTo: IrExpression,
)

fun IrConst.toConstFact(): JsFact {
    val latticeValue = when (kind) {
        IrConstKind.Null -> JsValueLattice.Null
        IrConstKind.Boolean -> JsValueLattice.Const(IrConstKind.Boolean, value)
        IrConstKind.Int -> JsValueLattice.Const(IrConstKind.Int, value)
        IrConstKind.Long -> JsValueLattice.Const(IrConstKind.Long, value)
        IrConstKind.String -> JsValueLattice.Const(IrConstKind.String, value)
        IrConstKind.Byte -> JsValueLattice.Const(IrConstKind.Byte, value)
        IrConstKind.Short -> JsValueLattice.Const(IrConstKind.Short, value)
        IrConstKind.Char -> JsValueLattice.Const(IrConstKind.Char, value)
        IrConstKind.Float, IrConstKind.Double -> return JsFact.Top
    }
    val type = when (kind) {
        IrConstKind.Null -> JsTypeLattice.Top
        else -> type.toFact().type
    }
    return JsFact(latticeValue, type)
}
