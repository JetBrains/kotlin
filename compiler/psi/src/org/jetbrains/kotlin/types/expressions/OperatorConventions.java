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

package org.jetbrains.kotlin.types.expressions;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtSingleValueToken;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;

import static org.jetbrains.kotlin.util.OperatorNameConventions.*;

public class OperatorConventions {

    private OperatorConventions() {}

    // Names for primitive type conversion properties
    public static final Name DOUBLE = Name.identifier("toDouble");
    public static final Name FLOAT = Name.identifier("toFloat");
    public static final Name LONG = Name.identifier("toLong");
    public static final Name INT = Name.identifier("toInt");
    public static final Name CHAR = Name.identifier("toChar");
    public static final Name SHORT = Name.identifier("toShort");
    public static final Name BYTE = Name.identifier("toByte");


    public static final ImmutableSet<Name> NUMBER_CONVERSIONS = ImmutableSet.of(
            DOUBLE, FLOAT, LONG, INT, SHORT, BYTE, CHAR
    );

    // If you add new unary, binary or assignment operators, add it to OperatorConventionNames as well

    public static final ImmutableBiMap<KtSingleValueToken, Name> UNARY_OPERATION_NAMES = ImmutableBiMap.<KtSingleValueToken, Name>builder()
            .put(KtTokens.PLUSPLUS, INC)
            .put(KtTokens.MINUSMINUS, DEC)
            .put(KtTokens.PLUS, UNARY_PLUS)
            .put(KtTokens.MINUS, UNARY_MINUS)
            .put(KtTokens.EXCL, NOT)
            .build();

    public static final ImmutableBiMap<KtSingleValueToken, Name> BINARY_OPERATION_NAMES = ImmutableBiMap.<KtSingleValueToken, Name>builder()
            .put(KtTokens.MUL, TIMES)
            .put(KtTokens.PLUS, PLUS)
            .put(KtTokens.MINUS, MINUS)
            .put(KtTokens.DIV, DIV)
            .put(KtTokens.PERC, REM)
            .put(KtTokens.RANGE, RANGE_TO)
            .put(KtTokens.RANGE_UNTIL, RANGE_UNTIL)
            .build();

    public static final ImmutableBiMap<Name, Name> REM_TO_MOD_OPERATION_NAMES = ImmutableBiMap.<Name, Name>builder()
            .put(REM, MOD)
            .put(REM_ASSIGN, MOD_ASSIGN)
            .build();

    public static final ImmutableSet<KtSingleValueToken> NOT_OVERLOADABLE =
            ImmutableSet.of(KtTokens.ANDAND, KtTokens.OROR, KtTokens.ELVIS, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ);
    
    public static final ImmutableSet<KtSingleValueToken> INCREMENT_OPERATIONS =
            ImmutableSet.of(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS);

    public static final ImmutableSet<KtSingleValueToken> COMPARISON_OPERATIONS =
            ImmutableSet.of(KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ);

    public static final ImmutableSet<KtSingleValueToken> EQUALS_OPERATIONS =
            ImmutableSet.of(KtTokens.EQEQ, KtTokens.EXCLEQ);

    public static final ImmutableSet<KtSingleValueToken> IDENTITY_EQUALS_OPERATIONS =
            ImmutableSet.of(KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ);

    public static final ImmutableSet<KtSingleValueToken> IN_OPERATIONS =
            ImmutableSet.of(KtTokens.IN_KEYWORD, KtTokens.NOT_IN);

    public static final ImmutableBiMap<KtSingleValueToken, Name> ASSIGNMENT_OPERATIONS = ImmutableBiMap.<KtSingleValueToken, Name>builder()
            .put(KtTokens.MULTEQ, TIMES_ASSIGN)
            .put(KtTokens.DIVEQ, DIV_ASSIGN)
            .put(KtTokens.PERCEQ, REM_ASSIGN)
            .put(KtTokens.PLUSEQ, PLUS_ASSIGN)
            .put(KtTokens.MINUSEQ, MINUS_ASSIGN)
            .build();

    public static final ImmutableBiMap<KtSingleValueToken, KtSingleValueToken> ASSIGNMENT_OPERATION_COUNTERPARTS = ImmutableBiMap.<KtSingleValueToken, KtSingleValueToken>builder()
            .put(KtTokens.MULTEQ, KtTokens.MUL)
            .put(KtTokens.DIVEQ, KtTokens.DIV)
            .put(KtTokens.PERCEQ, KtTokens.PERC)
            .put(KtTokens.PLUSEQ, KtTokens.PLUS)
            .put(KtTokens.MINUSEQ, KtTokens.MINUS)
            .build();

    public static final Name ASSIGN_METHOD = Name.identifier("assign");

    public static final ImmutableBiMap<KtSingleValueToken, Name> BOOLEAN_OPERATIONS = ImmutableBiMap.<KtSingleValueToken, Name>builder()
             .put(KtTokens.ANDAND, AND)
             .put(KtTokens.OROR, OR)
             .build();

    public static final ImmutableSet<Name> CONVENTION_NAMES = ImmutableSet.<Name>builder()
            .add(GET, SET, INVOKE, CONTAINS, ITERATOR, NEXT, HAS_NEXT, EQUALS, COMPARE_TO, GET_VALUE, SET_VALUE)
            .addAll(UNARY_OPERATION_NAMES.values())
            .addAll(BINARY_OPERATION_NAMES.values())
            .addAll(ASSIGNMENT_OPERATIONS.values())
            .build();

    @Nullable
    public static Name getNameForOperationSymbol(@NotNull KtToken token) {
        return getNameForOperationSymbol(token, true, true);
    }

    @Nullable
    public static Name getNameForOperationSymbol(@NotNull KtToken token, boolean unaryOperations, boolean binaryOperations) {
        Name name;

        if (binaryOperations) {
            name = BINARY_OPERATION_NAMES.get(token);
            if (name != null) return name;
        }

        if (unaryOperations) {
            name = UNARY_OPERATION_NAMES.get(token);
            if (name != null) return name;
        }

        name = ASSIGNMENT_OPERATIONS.get(token);
        if (name != null) return name;
        if (COMPARISON_OPERATIONS.contains(token)) return COMPARE_TO;
        if (EQUALS_OPERATIONS.contains(token)) return EQUALS;
        if (IN_OPERATIONS.contains(token)) return CONTAINS;
        return null;
    }

    @Nullable
    public static KtToken getOperationSymbolForName(@NotNull Name name) {
        if (!isConventionName(name)) return null;

        KtToken token;
        token = BINARY_OPERATION_NAMES.inverse().get(name);
        if (token != null) return token;
        token = UNARY_OPERATION_NAMES.inverse().get(name);
        if (token != null) return token;
        token = ASSIGNMENT_OPERATIONS.inverse().get(name);
        if (token != null) return token;
        return null;
    }

    public static boolean isConventionName(@NotNull Name name) {
        return CONVENTION_NAMES.contains(name) || COMPONENT_REGEX.matches(name.asString());
    }
}
