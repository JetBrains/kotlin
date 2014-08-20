/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

public class OperatorConventions {

    public static final Name EQUALS = Name.identifier("equals");
    public static final Name IDENTITY_EQUALS = Name.identifier("identityEquals");
    public static final Name COMPARE_TO = Name.identifier("compareTo");
    public static final Name CONTAINS = Name.identifier("contains");

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

    public static final ImmutableBiMap<JetToken, Name> UNARY_OPERATION_NAMES = ImmutableBiMap.<JetToken, Name>builder()
            .put(JetTokens.PLUSPLUS, Name.identifier("inc"))
            .put(JetTokens.MINUSMINUS, Name.identifier("dec"))
            .put(JetTokens.PLUS, Name.identifier("plus"))
            .put(JetTokens.MINUS, Name.identifier("minus"))
            .put(JetTokens.EXCL, Name.identifier("not"))
            .build();

    public static final ImmutableBiMap<JetToken, Name> BINARY_OPERATION_NAMES = ImmutableBiMap.<JetToken, Name>builder()
            .put(JetTokens.MUL, Name.identifier("times"))
            .put(JetTokens.PLUS, Name.identifier("plus"))
            .put(JetTokens.MINUS, Name.identifier("minus"))
            .put(JetTokens.DIV, Name.identifier("div"))
            .put(JetTokens.PERC, Name.identifier("mod"))
            .put(JetTokens.ARROW, Name.identifier("arrow"))
            .put(JetTokens.RANGE, Name.identifier("rangeTo"))
            .build();

    public static final ImmutableSet<JetToken> NOT_OVERLOADABLE = 
            ImmutableSet.<JetToken>of(JetTokens.ANDAND, JetTokens.OROR, JetTokens.ELVIS);
    
    public static final ImmutableSet<JetToken> INCREMENT_OPERATIONS =
            ImmutableSet.<JetToken>of(JetTokens.PLUSPLUS, JetTokens.MINUSMINUS);

    public static final ImmutableSet<JetToken> COMPARISON_OPERATIONS =
            ImmutableSet.<JetToken>of(JetTokens.LT, JetTokens.GT, JetTokens.LTEQ, JetTokens.GTEQ);

    public static final ImmutableSet<JetToken> EQUALS_OPERATIONS =
            ImmutableSet.<JetToken>of(JetTokens.EQEQ, JetTokens.EXCLEQ);

    public static final ImmutableSet<JetToken> IDENTITY_EQUALS_OPERATIONS =
            ImmutableSet.<JetToken>of(JetTokens.EQEQEQ, JetTokens.EXCLEQEQEQ);

    public static final ImmutableSet<JetToken> IN_OPERATIONS =
            ImmutableSet.<JetToken>of(JetTokens.IN_KEYWORD, JetTokens.NOT_IN);

    public static final ImmutableBiMap<JetToken, Name> ASSIGNMENT_OPERATIONS = ImmutableBiMap.<JetToken, Name>builder()
            .put(JetTokens.MULTEQ, Name.identifier("timesAssign"))
            .put(JetTokens.DIVEQ, Name.identifier("divAssign"))
            .put(JetTokens.PERCEQ, Name.identifier("modAssign"))
            .put(JetTokens.PLUSEQ, Name.identifier("plusAssign"))
            .put(JetTokens.MINUSEQ, Name.identifier("minusAssign"))
            .build();

    public static final ImmutableBiMap<JetToken, JetToken> ASSIGNMENT_OPERATION_COUNTERPARTS = ImmutableBiMap.<JetToken, JetToken>builder()
            .put(JetTokens.MULTEQ, JetTokens.MUL)
            .put(JetTokens.DIVEQ, JetTokens.DIV)
            .put(JetTokens.PERCEQ, JetTokens.PERC)
            .put(JetTokens.PLUSEQ, JetTokens.PLUS)
            .put(JetTokens.MINUSEQ, JetTokens.MINUS)
            .build();

    public static final ImmutableBiMap<JetToken, Name> BOOLEAN_OPERATIONS = ImmutableBiMap.<JetToken, Name>builder()
             .put(JetTokens.ANDAND, Name.identifier("and"))
             .put(JetTokens.OROR, Name.identifier("or"))
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
}
