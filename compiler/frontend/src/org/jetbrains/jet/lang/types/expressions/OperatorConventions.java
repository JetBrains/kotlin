/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.types.expressions;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class OperatorConventions {

    public static final String EQUALS = "equals";
    public static final String COMPARE_TO = "compareTo";
    public static final String CONTAINS = "contains";

    private OperatorConventions() {}

    // Names for primitive type conversion properties
    public static final String DOUBLE = "toDouble";
    public static final String FLOAT = "toFloat";
    public static final String LONG = "toLong";
    public static final String INT = "toInt";
    public static final String CHAR = "toChar";
    public static final String SHORT = "toShort";
    public static final String BYTE = "toByte";


    public static final ImmutableSet<String> NUMBER_CONVERSIONS = ImmutableSet.of(
            DOUBLE, FLOAT, LONG, INT, SHORT, BYTE, CHAR
    );

    public static final ImmutableBiMap<JetToken, String> UNARY_OPERATION_NAMES = ImmutableBiMap.<JetToken, String>builder()
            .put(JetTokens.PLUSPLUS, "inc")
            .put(JetTokens.MINUSMINUS, "dec")
            .put(JetTokens.PLUS, "plus")
            .put(JetTokens.MINUS, "minus")
            .put(JetTokens.EXCL, "not")
            .build();

    public static final ImmutableBiMap<JetToken, String> BINARY_OPERATION_NAMES = ImmutableBiMap.<JetToken, String>builder()
            .put(JetTokens.MUL, "times")
            .put(JetTokens.PLUS, "plus")
            .put(JetTokens.MINUS, "minus")
            .put(JetTokens.DIV, "div")
            .put(JetTokens.PERC, "mod")
            .put(JetTokens.ARROW, "arrow")
            .put(JetTokens.RANGE, "rangeTo")
            .build();

    public static final ImmutableSet<JetToken> NOT_OVERLOADABLE = 
            ImmutableSet.<JetToken>of(JetTokens.ANDAND, JetTokens.OROR, JetTokens.ELVIS);
    
    public static final ImmutableSet<JetToken> INCREMENT_OPERATIONS =
            ImmutableSet.<JetToken>of(JetTokens.PLUSPLUS, JetTokens.MINUSMINUS);

    public static final ImmutableSet<JetToken> COMPARISON_OPERATIONS =
            ImmutableSet.<JetToken>of(JetTokens.LT, JetTokens.GT, JetTokens.LTEQ, JetTokens.GTEQ);

    public static final ImmutableSet<JetToken> EQUALS_OPERATIONS =
            ImmutableSet.<JetToken>of(JetTokens.EQEQ, JetTokens.EXCLEQ);

    public static final ImmutableSet<JetToken> IN_OPERATIONS =
            ImmutableSet.<JetToken>of(JetTokens.IN_KEYWORD, JetTokens.NOT_IN);

    public static final ImmutableBiMap<JetToken, String> ASSIGNMENT_OPERATIONS = ImmutableBiMap.<JetToken, String>builder()
            .put(JetTokens.MULTEQ, "timesAssign")
            .put(JetTokens.DIVEQ, "divAssign")
            .put(JetTokens.PERCEQ, "modAssign")
            .put(JetTokens.PLUSEQ, "plusAssign")
            .put(JetTokens.MINUSEQ, "minusAssign")
            .build();

    public static final ImmutableMap<JetToken, JetToken> ASSIGNMENT_OPERATION_COUNTERPARTS = ImmutableMap.<JetToken, JetToken>builder()
            .put(JetTokens.MULTEQ, JetTokens.MUL)
            .put(JetTokens.DIVEQ, JetTokens.DIV)
            .put(JetTokens.PERCEQ, JetTokens.PERC)
            .put(JetTokens.PLUSEQ, JetTokens.PLUS)
            .put(JetTokens.MINUSEQ, JetTokens.MINUS)
            .build();
    
    @Nullable
    public static String getNameForOperationSymbol(@NotNull JetToken token) {
        String name = UNARY_OPERATION_NAMES.get(token);
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
}
