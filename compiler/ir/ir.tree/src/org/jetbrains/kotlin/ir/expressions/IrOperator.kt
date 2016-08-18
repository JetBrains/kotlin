/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.expressions

interface IrOperator {
    object UMINUS : IrOperatorImpl("UMINUS"), IrUnaryOperator
    object EXCL : IrOperatorImpl("EXCL"), IrUnaryOperator
    object EXCLEXCL : IrOperatorImpl("EXCLEXCL"), IrUnaryOperator

    object ELVIS : IrOperatorImpl("ELVIS"), IrBinaryOperator

    object LT : IrOperatorImpl("LT"), IrBinaryOperator
    object GT : IrOperatorImpl("GT"), IrBinaryOperator
    object LTEQ : IrOperatorImpl("LTEQ"), IrBinaryOperator
    object GTEQ : IrOperatorImpl("GTEQ"), IrBinaryOperator

    object EQEQ : IrOperatorImpl("EQEQ"), IrBinaryOperator
    object EQEQEQ : IrOperatorImpl("EQEQEQ"), IrBinaryOperator
    object EXCLEQ : IrOperatorImpl("EXCLEQ"), IrBinaryOperator
    object EXCLEQEQ : IrOperatorImpl("EXCLEQEQ"), IrBinaryOperator

    object IN : IrOperatorImpl("IN"), IrBinaryOperator
    object NOT_IN : IrOperatorImpl("NOT_IN"), IrBinaryOperator
    object ANDAND : IrOperatorImpl("ANDAND"), IrBinaryOperator
    object OROR : IrOperatorImpl("OROR"), IrBinaryOperator



    object PLUS : IrOperatorImpl("PLUS"), IrBinaryOperator
    object MINUS : IrOperatorImpl("MINUS"), IrBinaryOperator
    object MUL : IrOperatorImpl("MUL"), IrBinaryOperator
    object DIV : IrOperatorImpl("DIV"), IrBinaryOperator
    object PERC : IrOperatorImpl("PERC"), IrBinaryOperator
    object RANGE : IrOperatorImpl("RANGE"), IrBinaryOperator

    object INVOKE : IrOperatorImpl("INVOKE"), IrUnaryOperator

    object PREFIX_INCR : IrOperatorImpl("PREFIX_INCR"), IrUnaryOperator
    object PREFIX_DECR : IrOperatorImpl("PREFIX_DECR"), IrUnaryOperator
    object POSTFIX_INCR : IrOperatorImpl("POSTFIX_INCR"), IrUnaryOperator
    object POSTFIX_DECR : IrOperatorImpl("POSTFIX_DECR"), IrUnaryOperator

    object EQ : IrOperatorImpl("EQ"), IrBinaryOperator
    object PLUSEQ : IrOperatorImpl("PLUSEQ"), IrBinaryOperator
    object MINUSEQ : IrOperatorImpl("MINUSEQ"), IrBinaryOperator
    object MULTEQ : IrOperatorImpl("MULTEQ"), IrBinaryOperator
    object DIVEQ : IrOperatorImpl("DIVEQ"), IrBinaryOperator
    object PERCEQ : IrOperatorImpl("PERCEQ"), IrBinaryOperator

    object SYNTHETIC_BLOCK : IrOperatorImpl("SYNTHETIC_BLOCK")

    data class COMPONENT_N private constructor(val index: Int) : IrOperatorImpl("COMPONENT_$index") {
        companion object {
            private val precreatedComponents = Array(32) { i -> COMPONENT_N(i + 1) }

            fun withIndex(index: Int) =
                    if (index < precreatedComponents.size)
                        precreatedComponents[index - 1]
                    else
                        COMPONENT_N(index)
        }
    }
}

interface IrUnaryOperator : IrOperator
interface IrBinaryOperator : IrOperator

abstract class IrOperatorImpl(val debugName: String): IrOperator {
    override fun toString(): String = debugName
}