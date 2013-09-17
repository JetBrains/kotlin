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

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.diagnostics.AbstractDiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.psi.JetConstantExpression;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

public class CompileTimeConstantResolver {
    private final KotlinBuiltIns builtIns;

    public CompileTimeConstantResolver() {
        this.builtIns = KotlinBuiltIns.getInstance();
    }

    @Nullable
    public Diagnostic checkConstantExpressionType(
            @NotNull JetConstantExpression expression,
            @NotNull JetType expectedType
    ) {
        CompileTimeConstant<?> compileTimeConstant = getCompileTimeConstant(expression, expectedType);
        Set<AbstractDiagnosticFactory> errorsThatDependOnExpectedType =
                Sets.<AbstractDiagnosticFactory>newHashSet(CONSTANT_EXPECTED_TYPE_MISMATCH, NULL_FOR_NONNULL_TYPE);

        if (compileTimeConstant instanceof ErrorValueWithDiagnostic) {
            Diagnostic diagnostic = ((ErrorValueWithDiagnostic) compileTimeConstant).getDiagnostic();
            if (errorsThatDependOnExpectedType.contains(diagnostic.getFactory())) {
                return diagnostic;
            }
        }
        return null;
    }

    @NotNull
    public CompileTimeConstant<?> getCompileTimeConstant(
            @NotNull JetConstantExpression expression,
            @NotNull JetType expectedType
    ) {
        IElementType elementType = expression.getNode().getElementType();

        CompileTimeConstant<?> value;
        if (elementType == JetNodeTypes.INTEGER_CONSTANT) {
            value = getIntegerValue(expression, expectedType);
        }
        else if (elementType == JetNodeTypes.FLOAT_CONSTANT) {
            value = getFloatValue(expression, expectedType);
        }
        else if (elementType == JetNodeTypes.BOOLEAN_CONSTANT) {
            value = getBooleanValue(expression, expectedType);
        }
        else if (elementType == JetNodeTypes.CHARACTER_CONSTANT) {
            value = getCharValue(expression, expectedType);
        }
        else if (elementType == JetNodeTypes.NULL) {
            value = getNullValue(expression, expectedType);
        }
        else {
            throw new IllegalArgumentException("Unsupported constant: " + expression);
        }
        return value;
    }

    @NotNull
    public CompileTimeConstant<?> getIntegerValue(
            @NotNull JetConstantExpression expression, @NotNull JetType expectedType
    ) {
        String text = expression.getText();
        return getIntegerValue(parseLongValue(text), expectedType, expression);
    }

    @NotNull
    public CompileTimeConstant<?> getIntegerValue(
            @Nullable Long value,
            @NotNull JetType expectedType,
            @NotNull JetConstantExpression expression
    ) {
        if (value == null) {
            return createErrorValue(INT_LITERAL_OUT_OF_RANGE.on(expression));
        }
        if (noExpectedTypeOrError(expectedType)) {
            if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
                return new IntValue(value.intValue());
            }
            return new LongValue(value);
        }
        Function<Long, ? extends CompileTimeConstant<?>> create;
        long lowerBound;
        long upperBound;
        TypeConstructor constructor = expectedType.getConstructor();
        if (constructor == builtIns.getInt().getTypeConstructor()) {
            create = IntValue.CREATE;
            lowerBound = Integer.MIN_VALUE;
            upperBound = Integer.MAX_VALUE;
        }
        else if (constructor == builtIns.getLong().getTypeConstructor()) {
            create = LongValue.CREATE;
            lowerBound = Long.MIN_VALUE;
            upperBound = Long.MAX_VALUE;
        }
        else if (constructor == builtIns.getShort().getTypeConstructor()) {
            create = ShortValue.CREATE;
            lowerBound = Short.MIN_VALUE;
            upperBound = Short.MAX_VALUE;
        }
        else if (constructor == builtIns.getByte().getTypeConstructor()) {
            create = ByteValue.CREATE;
            lowerBound = Byte.MIN_VALUE;
            upperBound = Byte.MAX_VALUE;
        }
        else  {
            JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;
            JetType intType = builtIns.getIntType();
            JetType longType = builtIns.getLongType();
            if (typeChecker.isSubtypeOf(intType, expectedType)) {
                return getIntegerValue(value, intType, expression);
            }
            else if (typeChecker.isSubtypeOf(longType, expectedType)) {
                return getIntegerValue(value, longType, expression);
            }
            else {
                return createErrorValue(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, "integer", expectedType));
            }
        }

        if (value != null && lowerBound <= value && value <= upperBound) {
            return create.apply(value);
        }
        return createErrorValue(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, "integer", expectedType));
    }

    @Nullable
    public static Long parseLongValue(String text) {
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
    public static Double parseDoubleValue(String text) {
        try {
            return Double.parseDouble(text);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    @NotNull
    public CompileTimeConstant<?> getFloatValue(
            @NotNull JetConstantExpression expression, @NotNull JetType expectedType
    ) {
        String text = expression.getText();
        try {
            if (noExpectedTypeOrError(expectedType)
                || JetTypeChecker.INSTANCE.isSubtypeOf(builtIns.getDoubleType(), expectedType)) {
                return new DoubleValue(Double.parseDouble(text));
            }
            else if (JetTypeChecker.INSTANCE.isSubtypeOf(builtIns.getFloatType(), expectedType)) {
                return new FloatValue(Float.parseFloat(text));
            }
            else {
                return createErrorValue(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, "floating-point", expectedType));
            }
        }
        catch (NumberFormatException e) {
            return createErrorValue(FLOAT_LITERAL_OUT_OF_RANGE.on(expression));
        }
    }

    @Nullable
    private static CompileTimeConstant<?> checkNativeType(
            JetType expectedType,
            String title,
            JetType nativeType,
            JetConstantExpression expression
    ) {
        if (!noExpectedTypeOrError(expectedType)
            && !JetTypeChecker.INSTANCE.isSubtypeOf(nativeType, expectedType)) {

            return createErrorValue(CONSTANT_EXPECTED_TYPE_MISMATCH.on(expression, title, expectedType));
        }
        return null;
    }

    @NotNull
    public CompileTimeConstant<?> getBooleanValue(
            @NotNull JetConstantExpression expression, @NotNull JetType expectedType
    ) {
        String text = expression.getText();
        CompileTimeConstant<?> error = checkNativeType(expectedType, "boolean", builtIns.getBooleanType(), expression);
        if (error != null) {
            return error;
        }
        if ("true".equals(text)) {
            return BooleanValue.TRUE;
        }
        else if ("false".equals(text)) {
            return BooleanValue.FALSE;
        }
        throw new IllegalStateException("Must not happen. A boolean literal has text: " + text);
    }

    @NotNull
    public CompileTimeConstant<?> getCharValue(
            @NotNull JetConstantExpression expression, @NotNull JetType expectedType
    ) {
        String text = expression.getText();
        CompileTimeConstant<?> error = checkNativeType(expectedType, "character", builtIns.getCharType(), expression);
        if (error != null) {
            return error;
        }

        // Strip the quotes
        if (text.length() < 2 || text.charAt(0) != '\'' || text.charAt(text.length() - 1) != '\'') {
            return createErrorValue(INCORRECT_CHARACTER_LITERAL.on(expression));
        }
        text = text.substring(1, text.length() - 1); // now there're no quotes

        if (text.length() == 0) {
            return createErrorValue(EMPTY_CHARACTER_LITERAL.on(expression));
        }

        if (text.charAt(0) != '\\') {
            // No escape
            if (text.length() == 1) {
                return new CharValue(text.charAt(0));
            }
            return createErrorValue(TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL.on(expression, expression));
        }
        return escapedStringToCharValue(text, expression);
    }

    @NotNull
    public static CompileTimeConstant<?> escapedStringToCharValue(
            @NotNull String text,
            @NotNull JetElement expression
    ) {
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
                return new CharValue(escaped);
            case 5:
                // unicode escape
                if (escape.charAt(0) == 'u') {
                    try {
                        Integer intValue = Integer.valueOf(escape.substring(1), 16);
                        return new CharValue((char) intValue.intValue());
                    } catch (NumberFormatException e) {
                        // Will be reported below
                    }
                }
                break;
        }
        return illegalEscape(expression);
    }

    @NotNull
    private static CompileTimeConstant<?> illegalEscape(@NotNull JetElement expression) {
        return createErrorValue(ILLEGAL_ESCAPE.on(expression, expression));
    }

    @Nullable
    public static Character translateEscape(char c) {
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

    @NotNull
    public static CompileTimeConstant<?> getNullValue(@NotNull JetConstantExpression expression, @NotNull JetType expectedType) {
        if (noExpectedTypeOrError(expectedType) || expectedType.isNullable()) {
            return NullValue.NULL;
        }
        return createErrorValue(NULL_FOR_NONNULL_TYPE.on(expression, expectedType));
    }

    private static boolean noExpectedTypeOrError(JetType expectedType) {
        return TypeUtils.noExpectedType(expectedType) || expectedType.isError();
    }

    @NotNull
    private static ErrorValue createErrorValue(@NotNull Diagnostic diagnostic) {
        return new ErrorValueWithDiagnostic(diagnostic);
    }

    public static class ErrorValueWithDiagnostic extends ErrorValue {
        private final Diagnostic diagnostic;

        public ErrorValueWithDiagnostic(@NotNull Diagnostic diagnostic) {
            this.diagnostic = diagnostic;
        }

        @NotNull
        public Diagnostic getDiagnostic() {
            return diagnostic;
        }

        @NotNull
        @Override
        public JetType getType(@NotNull KotlinBuiltIns kotlinBuiltIns) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return DefaultErrorMessages.RENDERER.render(diagnostic);
        }
    }
}
