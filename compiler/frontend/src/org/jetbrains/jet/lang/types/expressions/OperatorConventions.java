package org.jetbrains.jet.lang.types.expressions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * @author abreslav
 */
public class OperatorConventions {

    private OperatorConventions() {}

    public static final ImmutableSet<String> NUMBER_CONVERSIONS = ImmutableSet.of(
            "dbl",
            "flt",
            "lng",
            "sht",
            "byt",
            "int"
    );

    public static final ImmutableMap<IElementType, String> UNARY_OPERATION_NAMES = ImmutableMap.<IElementType, String>builder()
            .put(JetTokens.PLUSPLUS, "inc")
            .put(JetTokens.MINUSMINUS, "dec")
            .put(JetTokens.PLUS, "plus")
            .put(JetTokens.MINUS, "minus")
            .put(JetTokens.EXCL, "not")
            .build();

    public static final ImmutableMap<IElementType, String> BINARY_OPERATION_NAMES = ImmutableMap.<IElementType, String>builder()
            .put(JetTokens.MUL, "times")
            .put(JetTokens.PLUS, "plus")
            .put(JetTokens.MINUS, "minus")
            .put(JetTokens.DIV, "div")
            .put(JetTokens.PERC, "mod")
            .put(JetTokens.ARROW, "arrow")
            .put(JetTokens.RANGE, "rangeTo")
            .build();

    public static final ImmutableSet<IElementType> COMPARISON_OPERATIONS = ImmutableSet.<IElementType>of(JetTokens.LT, JetTokens.GT, JetTokens.LTEQ, JetTokens.GTEQ);
    public static final ImmutableSet<IElementType> EQUALS_OPERATIONS = ImmutableSet.<IElementType>of(JetTokens.EQEQ, JetTokens.EXCLEQ);

    public static final ImmutableSet<IElementType> IN_OPERATIONS = ImmutableSet.<IElementType>of(JetTokens.IN_KEYWORD, JetTokens.NOT_IN);
    public static final ImmutableMap<IElementType, String> ASSIGNMENT_OPERATIONS = ImmutableMap.<IElementType, String>builder()
            .put(JetTokens.MULTEQ, "timesAssign")
            .put(JetTokens.DIVEQ, "divAssign")
            .put(JetTokens.PERCEQ, "modAssign")
            .put(JetTokens.PLUSEQ, "plusAssign")
            .put(JetTokens.MINUSEQ, "minusAssign")
            .build();

    public static final ImmutableMap<IElementType, IElementType> ASSIGNMENT_OPERATION_COUNTERPARTS = ImmutableMap.<IElementType, IElementType>builder()
            .put(JetTokens.MULTEQ, JetTokens.MUL)
            .put(JetTokens.DIVEQ, JetTokens.DIV)
            .put(JetTokens.PERCEQ, JetTokens.PERC)
            .put(JetTokens.PLUSEQ, JetTokens.PLUS)
            .put(JetTokens.MINUSEQ, JetTokens.MINUS)
            .build();
    
    @Nullable
    public static String getNameForOperationSymbol(@NotNull IElementType token) {
        String name = UNARY_OPERATION_NAMES.get(token);
        if (name != null) return name;
        name = BINARY_OPERATION_NAMES.get(token);
        if (name != null) return name;
        name = ASSIGNMENT_OPERATIONS.get(token);
        if (name != null) return name;
        if (COMPARISON_OPERATIONS.contains(token)) return "compareTo";
        if (EQUALS_OPERATIONS.contains(token)) return "equals";
        if (IN_OPERATIONS.contains(token)) return "contains";
        return null;
    } 

}
