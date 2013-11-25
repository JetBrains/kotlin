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

package org.jetbrains.jet.lang.resolve.constants;

import com.google.common.collect.Sets;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.psi.JetConstantExpression;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

public class CompileTimeConstantResolver {
    private static final Set<DiagnosticFactory> errorsThatDependOnExpectedType =
            Sets.<DiagnosticFactory>newHashSet(CONSTANT_EXPECTED_TYPE_MISMATCH, NULL_FOR_NONNULL_TYPE);

    private final KotlinBuiltIns builtIns;
    private final BindingTrace trace;
    private final boolean checkOnlyErrorsThatDependOnExpectedType;

    public CompileTimeConstantResolver(@NotNull BindingTrace trace, boolean checkOnlyErrorsThatDependOnExpectedType) {
        this.checkOnlyErrorsThatDependOnExpectedType = checkOnlyErrorsThatDependOnExpectedType;
        this.builtIns = KotlinBuiltIns.getInstance();
        this.trace = trace;
    }

    // return true if there is an error
    public boolean checkConstantExpressionType(
            @Nullable CompileTimeConstant compileTimeConstant,
            @NotNull JetConstantExpression expression,
            @NotNull JetType expectedType
    ) {
        IElementType elementType = expression.getNode().getElementType();

        if (elementType == JetNodeTypes.INTEGER_CONSTANT) {
            return checkIntegerValue(compileTimeConstant, expectedType, expression);
        }
        else if (elementType == JetNodeTypes.FLOAT_CONSTANT) {
            return checkFloatValue(compileTimeConstant, expectedType, expression);
        }
        else if (elementType == JetNodeTypes.BOOLEAN_CONSTANT) {
            return checkBooleanValue(expectedType, expression);
        }
        else if (elementType == JetNodeTypes.CHARACTER_CONSTANT) {
            return checkCharValue(compileTimeConstant, expectedType, expression);
        }
        else if (elementType == JetNodeTypes.NULL) {
            return checkNullValue(expectedType, expression);
        }
        return false;
    }

    private boolean checkIntegerValue(
            @Nullable CompileTimeConstant value,
            @NotNull JetType expectedType,
            @NotNull JetConstantExpression expression
    ) {
        if (value == null) {
            return reportError(INT_LITERAL_OUT_OF_RANGE.on(expression));
        }
        if (!noExpectedTypeOrError(expectedType)) {
            JetType valueType = value.getType(KotlinBuiltIns.getInstance());
            if (!JetTypeChecker.INSTANCE.isSubtypeOf(valueType, expectedType)) {
                return reportError(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, "integer", expectedType));
            }
        }
        return false;
    }

    private boolean checkFloatValue(
            @Nullable CompileTimeConstant value,
            @NotNull JetType expectedType,
            @NotNull JetConstantExpression expression
    ) {
        if (value == null) {
            return reportError(FLOAT_LITERAL_OUT_OF_RANGE.on(expression));
        }
        if (!noExpectedTypeOrError(expectedType)) {
            JetType valueType = value.getType(KotlinBuiltIns.getInstance());
            if (!JetTypeChecker.INSTANCE.isSubtypeOf(valueType, expectedType)) {
                return reportError(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, "floating-point", expectedType));
            }
        }
        return false;
    }

    private boolean checkBooleanValue(
            @NotNull JetType expectedType,
            @NotNull JetConstantExpression expression
    ) {
        if (!noExpectedTypeOrError(expectedType)
            && !JetTypeChecker.INSTANCE.isSubtypeOf(builtIns.getBooleanType(), expectedType)) {
            return reportError(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, "boolean", expectedType));
        }
        return false;
    }

    private boolean checkCharValue(CompileTimeConstant<?> constant, JetType expectedType, JetConstantExpression expression) {
        String text = expression.getText();
        if (!noExpectedTypeOrError(expectedType)
            && !JetTypeChecker.INSTANCE.isSubtypeOf(builtIns.getCharType(), expectedType)) {
            return reportError(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, "character", expectedType));
        }

        // Strip the quotes
        if (text.length() < 2 || text.charAt(0) != '\'' || text.charAt(text.length() - 1) != '\'') {
            return reportError(INCORRECT_CHARACTER_LITERAL.on(expression));
        }
        text = text.substring(1, text.length() - 1); // now there're no quotes

        if (text.length() == 0) {
            return reportError(EMPTY_CHARACTER_LITERAL.on(expression));
        }

        if (text.charAt(0) != '\\') {
            // No escape
            if (text.length() == 1) {
                return false;
            }
            return reportError(TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL.on(expression, expression));
        }
        if (constant == null) {
            return reportError(ILLEGAL_ESCAPE.on(expression, expression));
        }
        return false;
    }

    private boolean checkNullValue(@NotNull JetType expectedType, @NotNull JetConstantExpression expression) {
        if (!noExpectedTypeOrError(expectedType) && !expectedType.isNullable()) {
            return reportError(NULL_FOR_NONNULL_TYPE.on(expression, expectedType));
        }
        return false;
    }

    @Nullable
    public static Long parseLong(String text) {
        try {
            long value;
            if (text.startsWith("0x") || text.startsWith("0X")) {
                String hexString = text.substring(2);
                value = Long.parseLong(hexString, 16);
            }
            else if (text.startsWith("0b") || text.startsWith("0B")) {
                String binString = text.substring(2);
                value = Long.parseLong(binString, 2);
            }
            else {
                value = Long.parseLong(text);
            }
            return value;
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    public static Double parseDouble(String text) {
        try {
            return Double.parseDouble(text);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    @NotNull
    public static Object parseBoolean(@NotNull String text) {
        if ("true".equals(text)) {
            return true;
        }
        else if ("false".equals(text)) {
            return false;
        }
        throw new IllegalStateException("Must not happen. A boolean literal has text: " + text);
    }

    @Nullable
    public static Character parseChar(@NotNull String text) {
        // Strip the quotes
        if (text.length() < 2 || text.charAt(0) != '\'' || text.charAt(text.length() - 1) != '\'') {
            return null;
        }
        text = text.substring(1, text.length() - 1); // now there're no quotes

        if (text.length() == 0) {
            return null;
        }

        if (text.charAt(0) != '\\') {
            // No escape
            if (text.length() == 1) {
                return text.charAt(0);
            }
        }
        return escapedStringToChar(text);
    }

    @Nullable
    public static Character escapedStringToChar(@NotNull String text) {
        if (!(text.length() > 0 && text.charAt(0) == '\\')) return null;

        // Escape
        String escape = text.substring(1); // strip the slash
        switch (escape.length()) {
            case 0: return null;
            case 1:
                // one-char escape
                Character escaped = translateEscape(escape.charAt(0));
                if (escaped == null) {
                    return null;
                }
                return escaped;
            case 5:
                // unicode escape
                if (escape.charAt(0) == 'u') {
                    try {
                        Integer intValue = Integer.valueOf(escape.substring(1), 16);
                        return (char) intValue.intValue();
                    } catch (NumberFormatException e) {
                        // Will be reported below
                    }
                }
                break;
        }
        return null;
    }

    @Nullable
    private static Character translateEscape(char c) {
        switch (c) {
            case 't':
                return '\t';
            case 'b':
                return '\b';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case '\'':
                return '\'';
            case '\"':
                return '\"';
            case '\\':
                return '\\';
            case '$':
                return '$';
        }
        return null;
    }

    public static boolean noExpectedTypeOrError(JetType expectedType) {
        return TypeUtils.noExpectedType(expectedType) || expectedType.isError();
    }

    private boolean reportError(@NotNull Diagnostic diagnostic) {
        if (!checkOnlyErrorsThatDependOnExpectedType || errorsThatDependOnExpectedType.contains(diagnostic.getFactory())) {
            trace.report(diagnostic);
            return true;
        }
        return false;
    }
}
