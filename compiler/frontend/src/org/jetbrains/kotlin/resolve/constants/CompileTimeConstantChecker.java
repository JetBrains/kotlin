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

package org.jetbrains.kotlin.resolve.constants;

import com.google.common.collect.Sets;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.psi.JetConstantExpression;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.*;

public class CompileTimeConstantChecker {
    private static final Set<DiagnosticFactory<?>> errorsThatDependOnExpectedType =
            Sets.<DiagnosticFactory<?>>newHashSet(CONSTANT_EXPECTED_TYPE_MISMATCH, NULL_FOR_NONNULL_TYPE);

    private final KotlinBuiltIns builtIns;
    private final BindingTrace trace;
    private final boolean checkOnlyErrorsThatDependOnExpectedType;

    public CompileTimeConstantChecker(@NotNull BindingTrace trace, boolean checkOnlyErrorsThatDependOnExpectedType) {
        this.checkOnlyErrorsThatDependOnExpectedType = checkOnlyErrorsThatDependOnExpectedType;
        this.builtIns = KotlinBuiltIns.getInstance();
        this.trace = trace;
    }

    // return true if there is an error
    public boolean checkConstantExpressionType(
            @Nullable ConstantValue<?> compileTimeConstant,
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
            @Nullable ConstantValue<?> value,
            @NotNull JetType expectedType,
            @NotNull JetConstantExpression expression
    ) {
        if (value == null) {
            return reportError(INT_LITERAL_OUT_OF_RANGE.on(expression));
        }

        if (expression.getText().endsWith("l")) {
            return reportError(WRONG_LONG_SUFFIX.on(expression));
        }

        if (!noExpectedTypeOrError(expectedType)) {
            JetType valueType = value.getType();
            if (!JetTypeChecker.DEFAULT.isSubtypeOf(valueType, expectedType)) {
                return reportError(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, "integer", expectedType));
            }
        }
        return false;
    }

    private boolean checkFloatValue(
            @Nullable ConstantValue<?> value,
            @NotNull JetType expectedType,
            @NotNull JetConstantExpression expression
    ) {
        if (value == null) {
            return reportError(FLOAT_LITERAL_OUT_OF_RANGE.on(expression));
        }
        if (!noExpectedTypeOrError(expectedType)) {
            JetType valueType = value.getType();
            if (!JetTypeChecker.DEFAULT.isSubtypeOf(valueType, expectedType)) {
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
            && !JetTypeChecker.DEFAULT.isSubtypeOf(builtIns.getBooleanType(), expectedType)) {
            return reportError(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, "boolean", expectedType));
        }
        return false;
    }

    private boolean checkCharValue(ConstantValue<?> constant, JetType expectedType, JetConstantExpression expression) {
        if (!noExpectedTypeOrError(expectedType)
            && !JetTypeChecker.DEFAULT.isSubtypeOf(builtIns.getCharType(), expectedType)) {
            return reportError(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, "character", expectedType));
        }

        if (constant != null) {
            return false;
        }

        Diagnostic diagnostic = parseCharacter(expression).getDiagnostic();
        if (diagnostic != null) {
            return reportError(diagnostic);
        }
        return false;
    }

    private boolean checkNullValue(@NotNull JetType expectedType, @NotNull JetConstantExpression expression) {
        if (!noExpectedTypeOrError(expectedType) && !TypeUtils.acceptsNullable(expectedType)) {
            return reportError(NULL_FOR_NONNULL_TYPE.on(expression, expectedType));
        }
        return false;
    }

    @NotNull
    private static CharacterWithDiagnostic parseCharacter(@NotNull JetConstantExpression expression) {
        String text = expression.getText();
        // Strip the quotes
        if (text.length() < 2 || text.charAt(0) != '\'' || text.charAt(text.length() - 1) != '\'') {
            return createErrorCharacter(INCORRECT_CHARACTER_LITERAL.on(expression));
        }
        text = text.substring(1, text.length() - 1); // now there're no quotes

        if (text.length() == 0) {
            return createErrorCharacter(EMPTY_CHARACTER_LITERAL.on(expression));
        }

        if (text.charAt(0) != '\\') {
            // No escape
            if (text.length() == 1) {
                return new CharacterWithDiagnostic(text.charAt(0));
            }
            return createErrorCharacter(TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL.on(expression, expression));
        }
        return escapedStringToCharacter(text, expression);
    }

    @NotNull
    public static CharacterWithDiagnostic escapedStringToCharacter(@NotNull String text, @NotNull JetElement expression) {
        assert text.length() > 0 && text.charAt(0) == '\\' : "Only escaped sequences must be passed to this routine: " + text;

        // Escape
        String escape = text.substring(1); // strip the slash
        switch (escape.length()) {
            case 0:
                // bare slash
                return illegalEscape(expression);
            case 1:
                // one-char escape
                Character escaped = translateEscape(escape.charAt(0));
                if (escaped == null) {
                    return illegalEscape(expression);
                }
                return new CharacterWithDiagnostic(escaped);
            case 5:
                // unicode escape
                if (escape.charAt(0) == 'u') {
                    try {
                        Integer intValue = Integer.valueOf(escape.substring(1), 16);
                        return new CharacterWithDiagnostic((char) intValue.intValue());
                    } catch (NumberFormatException e) {
                        // Will be reported below
                    }
                }
                break;
        }
        return illegalEscape(expression);
    }

    @NotNull
    private static CharacterWithDiagnostic illegalEscape(@NotNull JetElement expression) {
        return createErrorCharacter(ILLEGAL_ESCAPE.on(expression, expression));
    }

    @NotNull
    private static CharacterWithDiagnostic createErrorCharacter(@NotNull Diagnostic diagnostic) {
        return new CharacterWithDiagnostic(diagnostic);
    }

    public static class CharacterWithDiagnostic {
        private Diagnostic diagnostic;
        private Character value;

        public CharacterWithDiagnostic(@NotNull Diagnostic diagnostic) {
            this.diagnostic = diagnostic;
        }

        public CharacterWithDiagnostic(char value) {
            this.value = value;
        }

        @Nullable
        public Diagnostic getDiagnostic() {
            return diagnostic;
        }

        @Nullable
        public Character getValue() {
            return value;
        }
    }

    @Nullable
    public static Character parseChar(@NotNull JetConstantExpression expression) {
        return parseCharacter(expression).getValue();
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
