/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.Name
import kotlin.text.Regex

object OperatorNameConventions {

    val EQUALS = Name.identifier("equals")
    val IDENTITY_EQUALS = Name.identifier("identityEquals");
    val COMPARE_TO = Name.identifier("compareTo")
    val CONTAINS = Name.identifier("contains")
    val INVOKE = Name.identifier("invoke")
    val ITERATOR = Name.identifier("iterator")
    val GET = Name.identifier("get")
    val SET = Name.identifier("set")
    val NEXT = Name.identifier("next")
    val HAS_NEXT = Name.identifier("hasNext")

    val COMPONENT_REGEX = Regex("component\\d+")

    val AND = Name.identifier("and")
    val OR = Name.identifier("or")

    val INC = Name.identifier("inc")
    val DEC = Name.identifier("dec")
    val PLUS = Name.identifier("plus")
    val MINUS = Name.identifier("minus")
    val NOT = Name.identifier("not")

    val TIMES = Name.identifier("times")
    val DIV = Name.identifier("div")
    val MOD = Name.identifier("mod")
    val RANGE_TO = Name.identifier("rangeTo")

    val TIMES_ASSIGN = Name.identifier("timesAssign")
    val DIV_ASSIGN = Name.identifier("divAssign")
    val MOD_ASSIGN = Name.identifier("modAssign")
    val PLUS_ASSIGN = Name.identifier("plusAssign")
    val MINUS_ASSIGN = Name.identifier("minusAssign")

    // If you add new unary, binary or assignment operators, add it to OperatorConventions as well

    private val UNARY_OPERATION_NAMES = setOf(INC, DEC, PLUS, MINUS, NOT)
    private val BINARY_OPERATION_NAMES = setOf(TIMES, PLUS, MINUS, DIV, MOD, RANGE_TO)
    private val ASSIGNMENT_OPERATIONS = setOf(TIMES_ASSIGN, DIV_ASSIGN, MOD_ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN)

    fun canBeOperator(functionDescriptor: FunctionDescriptor): Boolean {
        val name = functionDescriptor.name

        return when {
            GET == name -> true
            SET == name -> true
            INVOKE == name -> true
            CONTAINS == name -> true
            ITERATOR == name -> true
            NEXT == name -> true
            HAS_NEXT == name -> true
            EQUALS == name -> true
            COMPARE_TO == name -> true
            UNARY_OPERATION_NAMES.any { it == name } && functionDescriptor.valueParameters.isEmpty() -> true
            BINARY_OPERATION_NAMES.any { it == name } && functionDescriptor.valueParameters.size() == 1 -> true
            ASSIGNMENT_OPERATIONS.any { it == name } -> true
            name.asString().matches(COMPONENT_REGEX) -> true
            else -> false
        }
    }


}
