/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.effectsystem.impls

import org.jetbrains.kotlin.effectsystem.structure.ESClause
import org.jetbrains.kotlin.effectsystem.structure.EffectSchema
import org.jetbrains.kotlin.effectsystem.structure.ESExpressionVisitor

class Printer : ESExpressionVisitor<Unit> {
    private val builder = StringBuilder()

    private fun print(string: String) {
        builder.append(string)
    }

    private inline fun line(block: () -> Unit) {
        block()
    }

    fun print(schema: EffectSchema) {
        schema.clauses.forEach { visitClause(it) }
    }

    fun visitClause(clause: ESClause) {
        line {
            clause.condition.accept(this)
            print(" -> ")
            print(clause.effect.toString())
        }
    }

    override fun visitIs(isOperator: ESIs) {
        isOperator.left.accept(this)
        print(" ${if (isOperator.functor.isNegated) "!" else ""}is ")
        print(isOperator.functor.type.toString())
    }

    override fun visitEqual(equal: ESEqual) {
        equal.left.accept(this)
        print(" ${if (equal.functor.isNegated) "!=" else "=="} ")
        equal.right.accept(this)
    }

    override fun visitAnd(and: ESAnd) {
        and.left.accept(this)
        print(" && ")
        and.right.accept(this)
    }

    override fun visitNot(not: ESNot) {
        print("!")
        not.arg.accept(this)
    }

    override fun visitOr(or: ESOr) {
        or.left.accept(this)
        print(" || ")
        or.right.accept(this)
    }

    override fun visitVariable(esVariable: ESVariable) {
        print(esVariable.id.toString())
    }

    override fun visitConstant(esConstant: ESConstant) {
        print(esConstant.id.toString())
    }

    override fun visitLambda(esLambda: ESLambda) {
        print(esLambda.id.toString())
    }

    fun getString(): String = builder.toString()
}

fun EffectSchema.print(): String = Printer().also { it.print(this) }.getString()

fun ESClause.print() = Printer().also { it.visitClause(this) }.getString()