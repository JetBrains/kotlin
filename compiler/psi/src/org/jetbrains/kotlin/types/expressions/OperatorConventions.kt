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
package org.jetbrains.kotlin.types.expressions

import com.google.common.collect.ImmutableBiMap
import com.google.common.collect.ImmutableSet
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames.ITERATOR
import org.jetbrains.kotlin.util.OperatorNameConventions.AND
import org.jetbrains.kotlin.util.OperatorNameConventions.COMPARE_TO
import org.jetbrains.kotlin.util.OperatorNameConventions.COMPONENT_REGEX
import org.jetbrains.kotlin.util.OperatorNameConventions.CONTAINS
import org.jetbrains.kotlin.util.OperatorNameConventions.DEC
import org.jetbrains.kotlin.util.OperatorNameConventions.DIV
import org.jetbrains.kotlin.util.OperatorNameConventions.DIV_ASSIGN
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.GET
import org.jetbrains.kotlin.util.OperatorNameConventions.GET_VALUE
import org.jetbrains.kotlin.util.OperatorNameConventions.HAS_NEXT
import org.jetbrains.kotlin.util.OperatorNameConventions.INC
import org.jetbrains.kotlin.util.OperatorNameConventions.INVOKE
import org.jetbrains.kotlin.util.OperatorNameConventions.MINUS
import org.jetbrains.kotlin.util.OperatorNameConventions.MINUS_ASSIGN
import org.jetbrains.kotlin.util.OperatorNameConventions.NEXT
import org.jetbrains.kotlin.util.OperatorNameConventions.NOT
import org.jetbrains.kotlin.util.OperatorNameConventions.OR
import org.jetbrains.kotlin.util.OperatorNameConventions.PLUS
import org.jetbrains.kotlin.util.OperatorNameConventions.PLUS_ASSIGN
import org.jetbrains.kotlin.util.OperatorNameConventions.RANGE_TO
import org.jetbrains.kotlin.util.OperatorNameConventions.RANGE_UNTIL
import org.jetbrains.kotlin.util.OperatorNameConventions.REM
import org.jetbrains.kotlin.util.OperatorNameConventions.REM_ASSIGN
import org.jetbrains.kotlin.util.OperatorNameConventions.SET
import org.jetbrains.kotlin.util.OperatorNameConventions.SET_VALUE
import org.jetbrains.kotlin.util.OperatorNameConventions.TIMES
import org.jetbrains.kotlin.util.OperatorNameConventions.TIMES_ASSIGN
import org.jetbrains.kotlin.util.OperatorNameConventions.UNARY_MINUS
import org.jetbrains.kotlin.util.OperatorNameConventions.UNARY_PLUS

object OperatorConventions {
    // Names for primitive type conversion properties
    val DOUBLE: Name = Name.identifier("toDouble")
    val FLOAT: Name = Name.identifier("toFloat")
    val LONG: Name = Name.identifier("toLong")
    val INT: Name = Name.identifier("toInt")
    val CHAR: Name = Name.identifier("toChar")
    val SHORT: Name = Name.identifier("toShort")
    val BYTE: Name = Name.identifier("toByte")


    val NUMBER_CONVERSIONS: ImmutableSet<Name> = ImmutableSet.of<Name>(
        DOUBLE, FLOAT, LONG, INT, SHORT, BYTE, CHAR
    )

    // If you add new unary, binary or assignment operators, add it to OperatorConventionNames as well
    @JvmField
    val UNARY_OPERATION_NAMES: ImmutableBiMap<KtSingleValueToken, Name> = ImmutableBiMap.builder<KtSingleValueToken, Name>()
        .put(KtTokens.PLUSPLUS, INC)
        .put(KtTokens.MINUSMINUS, DEC)
        .put(KtTokens.PLUS, UNARY_PLUS)
        .put(KtTokens.MINUS, UNARY_MINUS)
        .put(KtTokens.EXCL, NOT)
        .build()

    @JvmField
    val BINARY_OPERATION_NAMES: ImmutableBiMap<KtSingleValueToken, Name> = ImmutableBiMap.builder<KtSingleValueToken, Name>()
        .put(KtTokens.MUL, TIMES)
        .put(KtTokens.PLUS, PLUS)
        .put(KtTokens.MINUS, MINUS)
        .put(KtTokens.DIV, DIV)
        .put(KtTokens.PERC, REM)
        .put(KtTokens.RANGE, RANGE_TO)
        .put(KtTokens.RANGE_UNTIL, RANGE_UNTIL)
        .build()

    val NOT_OVERLOADABLE: ImmutableSet<KtSingleValueToken> =
        ImmutableSet.of(KtTokens.ANDAND, KtTokens.OROR, KtTokens.ELVIS, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ)

    val INCREMENT_OPERATIONS: ImmutableSet<KtSingleValueToken> =
        ImmutableSet.of(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS)

    @JvmField
    val COMPARISON_OPERATIONS: ImmutableSet<KtSingleValueToken> =
        ImmutableSet.of(KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ)

    @JvmField
    val EQUALS_OPERATIONS: ImmutableSet<KtSingleValueToken> = ImmutableSet.of(KtTokens.EQEQ, KtTokens.EXCLEQ)

    @JvmField
    val IDENTITY_EQUALS_OPERATIONS: ImmutableSet<KtSingleValueToken> =
        ImmutableSet.of<KtSingleValueToken>(KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ)

    @JvmField
    val IN_OPERATIONS: ImmutableSet<KtSingleValueToken> = ImmutableSet.of(KtTokens.IN_KEYWORD, KtTokens.NOT_IN)

    @JvmField
    val ASSIGNMENT_OPERATIONS: ImmutableBiMap<KtSingleValueToken, Name> = ImmutableBiMap.builder<KtSingleValueToken, Name>()
        .put(KtTokens.MULTEQ, TIMES_ASSIGN)
        .put(KtTokens.DIVEQ, DIV_ASSIGN)
        .put(KtTokens.PERCEQ, REM_ASSIGN)
        .put(KtTokens.PLUSEQ, PLUS_ASSIGN)
        .put(KtTokens.MINUSEQ, MINUS_ASSIGN)
        .build()

    @JvmField
    val ASSIGNMENT_OPERATION_COUNTERPARTS: ImmutableBiMap<KtSingleValueToken, KtSingleValueToken> =
        ImmutableBiMap.builder<KtSingleValueToken, KtSingleValueToken>()
            .put(KtTokens.MULTEQ, KtTokens.MUL)
            .put(KtTokens.DIVEQ, KtTokens.DIV)
            .put(KtTokens.PERCEQ, KtTokens.PERC)
            .put(KtTokens.PLUSEQ, KtTokens.PLUS)
            .put(KtTokens.MINUSEQ, KtTokens.MINUS)
            .build()

    val ASSIGN_METHOD: Name = Name.identifier("assign")

    @JvmField
    val BOOLEAN_OPERATIONS: ImmutableBiMap<KtSingleValueToken, Name> = ImmutableBiMap.builder<KtSingleValueToken, Name>()
        .put(KtTokens.ANDAND, AND)
        .put(KtTokens.OROR, OR)
        .build()

    val CONVENTION_NAMES: ImmutableSet<Name> = ImmutableSet.builder<Name>()
        .add(GET, SET, INVOKE, CONTAINS, ITERATOR, NEXT, HAS_NEXT, EQUALS, COMPARE_TO, GET_VALUE, SET_VALUE)
        .addAll(UNARY_OPERATION_NAMES.values)
        .addAll(BINARY_OPERATION_NAMES.values)
        .addAll(ASSIGNMENT_OPERATIONS.values)
        .build()

    @JvmStatic
    fun getNameForOperationSymbol(token: KtToken): Name? {
        return getNameForOperationSymbol(token, unaryOperations = true, binaryOperations = true)
    }

    fun getNameForOperationSymbol(token: KtToken, unaryOperations: Boolean, binaryOperations: Boolean): Name? {
        var name: Name?

        if (binaryOperations) {
            name = BINARY_OPERATION_NAMES.get(token)
            if (name != null) return name
        }

        if (unaryOperations) {
            name = UNARY_OPERATION_NAMES.get(token)
            if (name != null) return name
        }

        name = ASSIGNMENT_OPERATIONS.get(token)
        if (name != null) return name
        if (COMPARISON_OPERATIONS.contains(token)) return COMPARE_TO
        if (EQUALS_OPERATIONS.contains(token)) return EQUALS
        if (IN_OPERATIONS.contains(token)) return CONTAINS
        return null
    }

    fun getOperationSymbolForName(name: Name): KtToken? {
        if (!isConventionName(name)) return null
        var token = BINARY_OPERATION_NAMES.inverse().get(name)
        if (token != null) return token
        token = UNARY_OPERATION_NAMES.inverse().get(name)
        if (token != null) return token
        token = ASSIGNMENT_OPERATIONS.inverse().get(name)
        if (token != null) return token
        return null
    }

    @JvmStatic
    fun isConventionName(name: Name): Boolean {
        return CONVENTION_NAMES.contains(name) || COMPONENT_REGEX.matches(name.asString())
    }
}
