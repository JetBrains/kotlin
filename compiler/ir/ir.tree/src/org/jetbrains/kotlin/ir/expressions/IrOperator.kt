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
    abstract class IrOperatorImpl(val debugName: String): IrOperator {
        override fun toString(): String = debugName
    }

    object SAFE_CALL : IrOperatorImpl("SAFE_CALL")
    
    object UMINUS : IrOperatorImpl("UMINUS")
    object UPLUS : IrOperatorImpl("UPLUS")
    object EXCL : IrOperatorImpl("EXCL")
    object EXCLEXCL : IrOperatorImpl("EXCLEXCL")

    object ELVIS : IrOperatorImpl("ELVIS")

    object LT : IrOperatorImpl("LT")
    object GT : IrOperatorImpl("GT")
    object LTEQ : IrOperatorImpl("LTEQ")
    object GTEQ : IrOperatorImpl("GTEQ")

    object EQEQ : IrOperatorImpl("EQEQ")
    object EQEQEQ : IrOperatorImpl("EQEQEQ")
    object EXCLEQ : IrOperatorImpl("EXCLEQ")
    object EXCLEQEQ : IrOperatorImpl("EXCLEQEQ")

    object IN : IrOperatorImpl("IN")
    object NOT_IN : IrOperatorImpl("NOT_IN")
    object ANDAND : IrOperatorImpl("ANDAND")
    object OROR : IrOperatorImpl("OROR")

    object PLUS : IrOperatorImpl("PLUS")
    object MINUS : IrOperatorImpl("MINUS")
    object MUL : IrOperatorImpl("MUL")
    object DIV : IrOperatorImpl("DIV")
    object PERC : IrOperatorImpl("PERC")
    object RANGE : IrOperatorImpl("RANGE")

    object INVOKE : IrOperatorImpl("INVOKE")
    object VARIABLE_AS_FUNCTION : IrOperatorImpl("VARIABLE_AS_FUNCTION")
    object GET_ARRAY_ELEMENT : IrOperatorImpl("GET_ARRAY_ELEMENT")

    object PREFIX_INCR : IrOperatorImpl("PREFIX_INCR")
    object PREFIX_DECR : IrOperatorImpl("PREFIX_DECR")
    object POSTFIX_INCR : IrOperatorImpl("POSTFIX_INCR")
    object POSTFIX_DECR : IrOperatorImpl("POSTFIX_DECR")

    object EQ : IrOperatorImpl("EQ")
    object PLUSEQ : IrOperatorImpl("PLUSEQ")
    object MINUSEQ : IrOperatorImpl("MINUSEQ")
    object MULTEQ : IrOperatorImpl("MULTEQ")
    object DIVEQ : IrOperatorImpl("DIVEQ")
    object PERCEQ : IrOperatorImpl("PERCEQ")

    object ARGUMENTS_REORDERING_FOR_CALL : IrOperatorImpl("ARGUMENTS_REORDERING_FOR_CALL")
    object DESTRUCTURING_DECLARATION : IrOperatorImpl("DESTRUCTURING_DECLARATION")
    
    object GET_PROPERTY : IrOperatorImpl("GET_PROPERTY")

    object IF : IrOperatorImpl("IF")
    object WHEN : IrOperatorImpl("WHEN")
    object WHEN_COMMA : IrOperatorImpl("WHEN_COMMA")
    object WHILE_LOOP : IrOperatorImpl("WHILE_LOOP")
    object DO_WHILE_LOOP : IrOperatorImpl("DO_WHILE_LOOP")
    object FOR_LOOP : IrOperatorImpl("FOR_LOOP")
    object FOR_LOOP_ITERATOR : IrOperatorImpl("FOR_LOOP_ITERATOR")
    object FOR_LOOP_INNER_WHILE : IrOperatorImpl("FOR_LOOP_INNER_WHILE")
    object FOR_LOOP_HAS_NEXT : IrOperatorImpl("FOR_LOOP_HAS_NEXT")
    object FOR_LOOP_NEXT : IrOperatorImpl("FOR_LOOP_NEXT")

    object LAMBDA : IrOperatorImpl("LAMBDA")
    object ANONYMOUS_FUNCTION : IrOperatorImpl("ANONYMOUS_FUNCTION")

    object DELEGATING_CONSTRUCTOR_CALL : IrOperatorImpl("DELEGATING_CONSTRUCTOR_CALL")
    object INITIALIZE_PROPERTY_FROM_PARAMETER : IrOperatorImpl("INITIALIZE_PROPERTY_FROM_PARAMETER")
    object ANONYMOUS_INITIALIZER : IrOperatorImpl("ANONYMOUS_INITIALIZER")

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

fun IrOperator.isAssignmentOperatorWithResult() =
        when (this) {
            IrOperator.PREFIX_INCR, IrOperator.PREFIX_DECR,
            IrOperator.POSTFIX_INCR, IrOperator.POSTFIX_DECR ->
                true
            else ->
                false
        }
