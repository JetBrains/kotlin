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

sealed class IrOperator {
    object INVOKE : IrOperator()
    object PREFIX_INCR : IrOperator()
    object PREFIX_DECR : IrOperator()
    object POSTFIX_INCR : IrOperator()
    object POSTFIX_DECR : IrOperator()
    object UMINUS : IrOperator()
    object EXCL : IrOperator()
    object EXCLEXCL : IrOperator()
    object ELVIS : IrOperator()
    
    object LT : IrOperator() 
    object GT : IrOperator()
    object LTEQ : IrOperator()
    object GTEQ : IrOperator()
    
    object EQEQ : IrOperator()
    object EQEQEQ : IrOperator()
    object EXCLEQ : IrOperator()
    object EXCLEQEQ : IrOperator()
    object IN : IrOperator()
    object NOT_IN : IrOperator()
    object ANDAND : IrOperator() 
    object OROR : IrOperator()
    object RANGE : IrOperator()

    object PLUS : IrOperator()
    object MINUS : IrOperator() 
    object MUL : IrOperator()
    object DIV : IrOperator()
    object MOD : IrOperator()

    object EQ : IrOperator()
    object PLUSEQ : IrOperator()
    object MINUSEQ : IrOperator()
    object MULEQ : IrOperator()
    object DIVEQ : IrOperator()
    object MODEQ : IrOperator()

    data class COMPONENT_N private constructor(val index: Int) : IrOperator() {
        companion object {
            private val precreatedComponents = Array(32, ::COMPONENT_N)

            fun withIndex(index: Int) =
                    if (index < precreatedComponents.size)
                        precreatedComponents[index]
                    else
                        COMPONENT_N(index)
        }
    }
}