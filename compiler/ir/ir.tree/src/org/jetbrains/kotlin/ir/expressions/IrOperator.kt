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

sealed class IrOperator(val debugName: String) {
    override fun toString(): String = debugName

    object INVOKE : IrOperator("INVOKE")
    object PREFIX_INCR : IrOperator("PREFIX_INCR")
    object PREFIX_DECR : IrOperator("PREFIX_DECR")
    object POSTFIX_INCR : IrOperator("POSTFIX_INCR")
    object POSTFIX_DECR : IrOperator("POSTFIX_DECR")
    object UMINUS : IrOperator("UMINUS")
    object EXCL : IrOperator("EXCL")
    object EXCLEXCL : IrOperator("EXCLEXCL")
    object ELVIS : IrOperator("ELVIS")
    
    object LT : IrOperator("LT")
    object GT : IrOperator("GT")
    object LTEQ : IrOperator("LTEQ")
    object GTEQ : IrOperator("GTEQ")
    
    object EQEQ : IrOperator("EQEQ")
    object EQEQEQ : IrOperator("EQEQEQ")
    object EXCLEQ : IrOperator("EXCLEQ")
    object EXCLEQEQ : IrOperator("EXCLEQEQ")
    object IN : IrOperator("IN")
    object NOT_IN : IrOperator("NOT_IN")
    object ANDAND : IrOperator("ANDAND")
    object OROR : IrOperator("OROR")
    object RANGE : IrOperator("RANGE")

    object PLUS : IrOperator("PLUS")
    object MINUS : IrOperator("MINUS")
    object MUL : IrOperator("MUL")
    object DIV : IrOperator("DIV")
    object PERC : IrOperator("PERC")

    object EQ : IrOperator("EQ")
    object PLUSEQ : IrOperator("PLUSEQ")
    object MINUSEQ : IrOperator("MINUSEQ")
    object MULTEQ : IrOperator("MULTEQ")
    object DIVEQ : IrOperator("DIVEQ")
    object PERCEQ : IrOperator("PERCEQ")

    data class COMPONENT_N private constructor(val index: Int) : IrOperator("COMPONENT_$index") {
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