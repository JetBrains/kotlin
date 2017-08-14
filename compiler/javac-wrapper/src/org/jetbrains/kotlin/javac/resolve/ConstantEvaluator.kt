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

package org.jetbrains.kotlin.javac.resolve

import com.sun.source.tree.CompilationUnitTree
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaClass
import kotlin.experimental.inv

class ConstantEvaluator(private val containingClass: JavaClass,
                        private val javac: JavacWrapper,
                        private val compilationUnit: CompilationUnitTree) {
    fun getValue(expr: JCTree.JCExpression): Any? {
        return when (expr) {
            is JCTree.JCLiteral -> {
                if (expr.typetag == TypeTag.BOOLEAN) {
                    expr.value != 0
                }
                else expr.value
            }
            is JCTree.JCIdent,
            is JCTree.JCFieldAccess -> javac.resolveField(expr, compilationUnit, containingClass)?.initializerValue
            is JCTree.JCBinary -> evaluateBinaryExpression(expr)
            is JCTree.JCParens -> getValue(expr.expr)
            is JCTree.JCUnary -> evaluateUnaryExpression(expr)
            else -> null
        }
    }

    private fun evaluateUnaryExpression(value: JCTree.JCUnary): Any? {
        val argValue = getValue(value.arg)
        return when (value.tag) {
            JCTree.Tag.COMPL -> {
                when (argValue) {
                    is Int -> argValue.inv()
                    is Long -> argValue.inv()
                    is Short -> argValue.inv()
                    is Byte -> argValue.inv()
                    else -> null
                }
            }
            JCTree.Tag.NOT -> (argValue as? Boolean)?.let { !it }
            else -> null
        }
    }

    private fun evaluateBinaryExpression(value: JCTree.JCBinary): Any? {
        val lhsValue = getValue(value.lhs) ?: return null
        val rhsValue = getValue(value.rhs) ?: return null

        return evaluateValue(lhsValue, rhsValue, value.tag)
    }

    private fun evaluateValue(lhsValue: Any?, rhsValue: Any?, opcode: JCTree.Tag): Any? {
        if (lhsValue is String && opcode == JCTree.Tag.PLUS) return lhsValue + rhsValue
        else if (lhsValue is Boolean && rhsValue is Boolean) {
            return when (opcode) {
                JCTree.Tag.AND -> lhsValue && rhsValue
                JCTree.Tag.OR -> lhsValue || rhsValue
                JCTree.Tag.EQ -> lhsValue == rhsValue
                JCTree.Tag.NE -> lhsValue != rhsValue
                JCTree.Tag.BITXOR -> lhsValue xor rhsValue
                JCTree.Tag.BITAND -> lhsValue and rhsValue
                JCTree.Tag.BITOR -> lhsValue or rhsValue
                else -> null
            }
        }
        else if (lhsValue is Number && rhsValue is Number) {
            val isInteger = !(lhsValue is Float || lhsValue is Double || rhsValue is Float || rhsValue is Double)
            val isWide = if (isInteger) {
                lhsValue is Long || rhsValue is Long
            }
            else {
                lhsValue is Double || rhsValue is Double
            }

            return when (opcode) {
                JCTree.Tag.PLUS -> {
                    if (isInteger) {
                        if (isWide) {
                            lhsValue.toLong() + rhsValue.toLong()
                        }
                        else {
                            lhsValue.toInt() + rhsValue.toInt()
                        }
                    }
                    else {
                        if (isWide) {
                            lhsValue.toDouble() + rhsValue.toDouble()
                        }
                        else {
                            lhsValue.toFloat() + rhsValue.toFloat()
                        }
                    }
                }
                JCTree.Tag.MINUS -> {
                    if (isInteger) {
                        if (isWide) {
                            lhsValue.toLong() - rhsValue.toLong()
                        }
                        else {
                            lhsValue.toInt() - rhsValue.toInt()
                        }
                    }
                    else {
                        if (isWide) {
                            lhsValue.toDouble() - rhsValue.toDouble()
                        }
                        else {
                            lhsValue.toFloat() - rhsValue.toFloat()
                        }
                    }
                }
                JCTree.Tag.MUL -> {
                    if (isInteger) {
                        if (isWide) {
                            lhsValue.toLong() * rhsValue.toLong()
                        }
                        else {
                            lhsValue.toInt() * rhsValue.toInt()
                        }
                    }
                    else {
                        if (isWide) {
                            lhsValue.toDouble() * rhsValue.toDouble()
                        }
                        else {
                            lhsValue.toFloat() * rhsValue.toFloat()
                        }
                    }
                }
                JCTree.Tag.DIV -> {
                    if (isInteger) {
                        if (isWide) {
                            lhsValue.toLong() / rhsValue.toLong()
                        }
                        else {
                            lhsValue.toInt() / rhsValue.toInt()
                        }
                    }
                    else {
                        if (isWide) {
                            lhsValue.toDouble() / rhsValue.toDouble()
                        }
                        else {
                            lhsValue.toFloat() / rhsValue.toFloat()
                        }
                    }
                }
                JCTree.Tag.MOD -> {
                    if (isInteger) {
                        if (isWide) {
                            lhsValue.toLong() % rhsValue.toLong()
                        }
                        else {
                            lhsValue.toInt() % rhsValue.toInt()
                        }
                    }
                    else {
                        if (isWide) {
                            lhsValue.toDouble() % rhsValue.toDouble()
                        }
                        else {
                            lhsValue.toFloat() % rhsValue.toFloat()
                        }
                    }
                }
                JCTree.Tag.SR -> {
                    if (isWide) {
                        lhsValue.toLong() shr rhsValue.toInt()
                    }
                    else {
                        lhsValue.toInt() shr rhsValue.toInt()
                    }
                }
                JCTree.Tag.SL -> {
                    if (isWide) {
                        lhsValue.toLong() shl rhsValue.toInt()
                    }
                    else {
                        lhsValue.toInt() shl rhsValue.toInt()
                    }
                }
                JCTree.Tag.BITAND -> {
                    if (isWide) {
                        lhsValue.toLong() and rhsValue.toLong()
                    }
                    else {
                        lhsValue.toInt() and rhsValue.toInt()
                    }
                }
                JCTree.Tag.BITOR -> {
                    if (isWide) {
                        lhsValue.toLong() or rhsValue.toLong()
                    }
                    else {
                        lhsValue.toInt() or rhsValue.toInt()
                    }
                }
                JCTree.Tag.BITXOR -> {
                    if (isWide) {
                        lhsValue.toLong() xor rhsValue.toLong()
                    }
                    else {
                        lhsValue.toInt() xor rhsValue.toInt()
                    }
                }
                JCTree.Tag.USR -> {
                    if (isWide) {
                        lhsValue.toLong() ushr rhsValue.toInt()
                    }
                    else {
                        lhsValue.toInt() ushr rhsValue.toInt()
                    }
                }
                JCTree.Tag.EQ -> {
                    if (isInteger) {
                        lhsValue.toLong() == rhsValue.toLong()
                    }
                    else {
                        lhsValue.toDouble() == rhsValue.toDouble()
                    }
                }
                JCTree.Tag.NE -> {
                    if (isInteger) {
                        lhsValue.toLong() != rhsValue.toLong()
                    }
                    else {
                        lhsValue.toDouble() != rhsValue.toDouble()
                    }
                }
                JCTree.Tag.LT -> {
                    if (isInteger) {
                        lhsValue.toLong() < rhsValue.toLong()
                    }
                    else {
                        lhsValue.toDouble() < rhsValue.toDouble()
                    }
                }
                JCTree.Tag.LE -> {
                    if (isInteger) {
                        lhsValue.toLong() <= rhsValue.toLong()
                    }
                    else {
                        lhsValue.toDouble() <= rhsValue.toDouble()
                    }
                }
                JCTree.Tag.GT -> {
                    if (isInteger) {
                        lhsValue.toLong() > rhsValue.toLong()
                    }
                    else {
                        lhsValue.toDouble() > rhsValue.toDouble()
                    }
                }
                JCTree.Tag.GE -> {
                    if (isInteger) {
                        lhsValue.toLong() >= rhsValue.toLong()
                    }
                    else {
                        lhsValue.toDouble() >= rhsValue.toDouble()
                    }
                }
                else -> null
            }
        }
        else return null
    }

}