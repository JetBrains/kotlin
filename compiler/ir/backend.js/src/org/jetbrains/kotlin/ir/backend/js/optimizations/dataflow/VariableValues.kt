/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop

/**
 * Collects the expressions that may provide the runtime value of each local variable.
 *
 * Assignments initially contribute their right-hand expressions. [computeClosure] then follows
 * reads of other locals transitively, so graph construction sees the terminal expressions that
 * may reach a local as well as its direct inputs. The recorded loop identifies the scope in which
 * the local's merge node must be placed.
 */
internal class VariableValues {
    /** The owning loop and possible value-producing expressions of one local declaration. */
    class Variable(val loop: IrLoop?, val values: MutableSet<IrExpression>)

    val elementData = mutableMapOf<IrValueDeclaration, Variable>()

    fun addEmpty(variable: IrValueDeclaration, loop: IrLoop?) {
        elementData[variable] = Variable(loop, mutableSetOf())
    }

    fun add(variable: IrValueDeclaration, element: IrExpression) =
        elementData[variable]?.values?.add(element)

    private fun add(variable: IrValueDeclaration, elements: Set<IrExpression>) =
        elementData[variable]?.values?.addAll(elements)

    /** Expands every local's values through transitive reads of other tracked locals. */
    fun computeClosure() {
        elementData.keys.forEach { key ->
            add(key, computeValueClosure(key))
        }
    }

    private fun computeValueClosure(value: IrValueDeclaration): Set<IrExpression> {
        val result = mutableSetOf<IrExpression>()
        val seen = mutableSetOf<IrValueDeclaration>()
        dfs(value, seen, result)
        return result
    }

    private fun dfs(value: IrValueDeclaration, seen: MutableSet<IrValueDeclaration>, result: MutableSet<IrExpression>) {
        seen += value
        val elements = elementData[value]?.values ?: return
        for (element in elements) {
            if (element !is IrGetValue) {
                result += element
                continue
            }
            val declaration = element.symbol.owner
            if (declaration is IrVariable && !seen.contains(declaration)) {
                dfs(declaration, seen, result)
            }
        }
    }
}
