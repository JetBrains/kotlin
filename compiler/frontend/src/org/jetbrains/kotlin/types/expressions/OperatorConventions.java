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

package org.jetbrains.kotlin.types.expressions;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.JetSingleValueToken;
import org.jetbrains.kotlin.lexer.JetToken;
import org.jetbrains.kotlin.lexer.JetTokens;
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

    public static final ImmutableBiMap<JetSingleValueToken, Name> UNARY_OPERATION_NAMES = ImmutableBiMap.<JetSingleValueToken, Name>builder()
            .put(JetTokens.PLUSPLUS, INC)
            .put(JetTokens.MINUSMINUS, DEC)
            .put(JetTokens.PLUS, PLUS)
            .put(JetTokens.MINUS, MINUS)
            .put(JetTokens.EXCL, NOT)
            .build();

    public static final ImmutableBiMap<JetSingleValueToken, Name> BINARY_OPERATION_NAMES = ImmutableBiMap.<JetSingleValueToken, Name>builder()
            .put(JetTokens.MUL, TIMES)
            .put(JetTokens.PLUS, PLUS)
            .put(JetTokens.MINUS, MINUS)
            .put(JetTokens.DIV, DIV)
            .put(JetTokens.PERC, MOD)
            .put(JetTokens.RANGE, RANGE_TO)
            .build();

    public static final ImmutableSet<JetSingleValueToken> NOT_OVERLOADABLE =
            ImmutableSet.of(JetTokens.ANDAND, JetTokens.OROR, JetTokens.ELVIS, JetTokens.EQEQEQ, JetTokens.EXCLEQEQEQ);
    
    public static final ImmutableSet<JetSingleValueToken> INCREMENT_OPERATIONS =
            ImmutableSet.of(JetTokens.PLUSPLUS, JetTokens.MINUSMINUS);

    public static final ImmutableSet<JetSingleValueToken> COMPARISON_OPERATIONS =
            ImmutableSet.of(JetTokens.LT, JetTokens.GT, JetTokens.LTEQ, JetTokens.GTEQ);

    public static final ImmutableSet<JetSingleValueToken> EQUALS_OPERATIONS =
            ImmutableSet.of(JetTokens.EQEQ, JetTokens.EXCLEQ);

    public static final ImmutableSet<JetSingleValueToken> IDENTITY_EQUALS_OPERATIONS =
            ImmutableSet.of(JetTokens.EQEQEQ, JetTokens.EXCLEQEQEQ);

    public static final ImmutableSet<JetSingleValueToken> IN_OPERATIONS =
            ImmutableSet.<JetSingleValueToken>of(JetTokens.IN_KEYWORD, JetTokens.NOT_IN);

    public static final ImmutableBiMap<JetSingleValueToken, Name> ASSIGNMENT_OPERATIONS = ImmutableBiMap.<JetSingleValueToken, Name>builder()
            .put(JetTokens.MULTEQ, TIMES_ASSIGN)
            .put(JetTokens.DIVEQ, DIV_ASSIGN)
            .put(JetTokens.PERCEQ, MOD_ASSIGN)
            .put(JetTokens.PLUSEQ, PLUS_ASSIGN)
            .put(JetTokens.MINUSEQ, MINUS_ASSIGN)
            .build();

    public static final ImmutableBiMap<JetSingleValueToken, JetSingleValueToken> ASSIGNMENT_OPERATION_COUNTERPARTS = ImmutableBiMap.<JetSingleValueToken, JetSingleValueToken>builder()
            .put(JetTokens.MULTEQ, JetTokens.MUL)
            .put(JetTokens.DIVEQ, JetTokens.DIV)
            .put(JetTokens.PERCEQ, JetTokens.PERC)
            .put(JetTokens.PLUSEQ, JetTokens.PLUS)
            .put(JetTokens.MINUSEQ, JetTokens.MINUS)
            .build();

    public static final ImmutableBiMap<JetSingleValueToken, Name> BOOLEAN_OPERATIONS = ImmutableBiMap.<JetSingleValueToken, Name>builder()
             .put(JetTokens.ANDAND, AND)
             .put(JetTokens.OROR, OR)
             .build();

    public static final ImmutableSet<Name> CONVENTION_NAMES = ImmutableSet.<Name>builder()
            .add(GET, SET, INVOKE, CONTAINS, ITERATOR, NEXT, HAS_NEXT, EQUALS, COMPARE_TO)
            .addAll(UNARY_OPERATION_NAMES.values())
            .addAll(BINARY_OPERATION_NAMES.values())
            .addAll(ASSIGNMENT_OPERATIONS.values())
            .build();

    @Nullable
    public static Name getNameForOperationSymbol(@NotNull JetToken token) {
        Name name = UNARY_OPERATION_NAMES.get(token);
        if (name != null) return name;
        name = BINARY_OPERATION_NAMES.get(token);
        if (name != null) return name;
        name = ASSIGNMENT_OPERATIONS.get(token);
        if (name != null) return name;
        if (COMPARISON_OPERATIONS.contains(token)) return COMPARE_TO;
        if (EQUALS_OPERATIONS.contains(token)) return EQUALS;
        if (IN_OPERATIONS.contains(token)) return CONTAINS;
        return null;
    }

    public static boolean isConventionName(@NotNull Name name) {
        return CONVENTION_NAMES.contains(name) || COMPONENT_REGEX.matches(name.asString());
    }
}
