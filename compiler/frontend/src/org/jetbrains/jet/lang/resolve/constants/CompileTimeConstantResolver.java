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
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.List;

public class CompileTimeConstantResolver {
    public static final ErrorValue OUT_OF_RANGE = new ErrorValue("The value is out of range");

    private final KotlinBuiltIns builtIns;

    public CompileTimeConstantResolver() {
        this.builtIns = KotlinBuiltIns.getInstance();
    }

    @NotNull
    public CompileTimeConstant<?> getCompileTimeConstant(
            @NotNull JetConstantExpression expression,
            @NotNull JetType expectedType
    ) {
        IElementType elementType = expression.getNode().getElementType();
        String text = expression.getNode().getText();

        CompileTimeConstant<?> value;
        if (elementType == JetNodeTypes.INTEGER_CONSTANT) {
            value = getIntegerValue(text, expectedType);
        }
        else if (elementType == JetNodeTypes.FLOAT_CONSTANT) {
            value = getFloatValue(text, expectedType);
        }
        else if (elementType == JetNodeTypes.BOOLEAN_CONSTANT) {
            value = getBooleanValue(text, expectedType);
        }
        else if (elementType == JetNodeTypes.CHARACTER_CONSTANT) {
            value = getCharValue(text, expectedType);
        }
        else if (elementType == JetNodeTypes.NULL) {
            value = getNullValue(expectedType);
        }
        else {
            throw new IllegalArgumentException("Unsupported constant: " + expression);
        }
        return value;
    }


    @NotNull
    public CompileTimeConstant<?> getIntegerValue(@NotNull String text, @NotNull JetType expectedType) {
        return getIntegerValue(parseLongValue(text), expectedType);
    }

    @NotNull
    public CompileTimeConstant<?> getIntegerValue(@Nullable Long value, @NotNull JetType expectedType) {
        if (value == null) {
            return OUT_OF_RANGE;
        }
        if (noExpectedTypeOrUnitOrError(expectedType)) {
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
                return getIntegerValue(value, intType);
            }
            else if (typeChecker.isSubtypeOf(longType, expectedType)) {
                return getIntegerValue(value, longType);
            }
            else {
                return new ErrorValue("An integer literal does not conform to the expected type " + expectedType);
            }
        }

        if (value != null && lowerBound <= value && value <= upperBound) {
            return create.apply(value);
        }
        return new ErrorValue("An integer literal does not conform to the expected type " + expectedType);
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
    public CompileTimeConstant<?> getFloatValue(@NotNull String text, @NotNull JetType expectedType) {
        if (noExpectedTypeOrUnitOrError(expectedType)
            || JetTypeChecker.INSTANCE.isSubtypeOf(builtIns.getDoubleType(), expectedType)) {
            try {
                return new DoubleValue(Double.parseDouble(text));
            }
            catch (NumberFormatException e) {
                return OUT_OF_RANGE;
            }
        }
        else if (JetTypeChecker.INSTANCE.isSubtypeOf(builtIns.getFloatType(), expectedType)) {
            try {
                return new FloatValue(Float.parseFloat(text));
            }
            catch (NumberFormatException e) {
                return OUT_OF_RANGE;
            }
        }
        else {
            return new ErrorValue("A floating-point literal does not conform to the expected type " + expectedType);
        }
    }

    @Nullable
    private CompileTimeConstant<?> checkNativeType(String text, JetType expectedType, String title, JetType nativeType) {
        if (!noExpectedTypeOrUnitOrError(expectedType)
            && !JetTypeChecker.INSTANCE.isSubtypeOf(nativeType, expectedType)) {
            return new ErrorValue("A " + title + " literal " + text + " does not conform to the expected type " + expectedType);
        }
        return null;
    }

    @NotNull
    public CompileTimeConstant<?> getBooleanValue(@NotNull String text, @NotNull JetType expectedType) {
        CompileTimeConstant<?> error = checkNativeType(text, expectedType, "boolean", builtIns.getBooleanType());
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
    public CompileTimeConstant<?> getCharValue(@NotNull String text, @NotNull JetType expectedType) {
        CompileTimeConstant<?> error = checkNativeType(text, expectedType, "character", builtIns.getCharType());
        if (error != null) {
            return error;
        }

        // Strip the quotes
        if (text.length() < 2 || text.charAt(0) != '\'' || text.charAt(text.length() - 1) != '\'') {
            return new ErrorValue("Incorrect character literal");
        }
        text = text.substring(1, text.length() - 1); // now there're no quotes

        if (text.length() == 0) {
            return new ErrorValue("Empty character literal");            
        }

        if (text.charAt(0) != '\\') {
            // No escape
            if (text.length() == 1) {
                return new CharValue(text.charAt(0));
            }
            return new ErrorValue("Too many characters in a character literal '" + text + "'");
        }
        return escapedStringToCharValue(text);
    }

    @NotNull
    public static CompileTimeConstant<?> escapedStringToCharValue(@NotNull String text) {
        assert text.length() > 0 && text.charAt(0) == '\\' : "Only escaped sequences must be passed to this routine: " + text;

        // Escape
        String escape = text.substring(1); // strip the slash
        switch (escape.length()) {
            case 0:
                // bare slash
                return illegalEscape(text);
            case 1:
                // one-char escape
                Character escaped = translateEscape(escape.charAt(0));
                if (escaped == null) {
                    return illegalEscape(text);
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
        return illegalEscape(text);
    }

    private static ErrorValue illegalEscape(String text) {
        return new ErrorValue("Illegal escape: " + text);
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
    public CompileTimeConstant<?> getNullValue(@NotNull JetType expectedType) {
        if (noExpectedTypeOrUnitOrError(expectedType) || expectedType.isNullable()) {
            return NullValue.NULL;
        }
        return new ErrorValue("Null can not be a value of a non-null type " + expectedType);
    }

    private static boolean noExpectedTypeOrUnitOrError(JetType expectedType) {
        return TypeUtils.noExpectedType(expectedType) || KotlinBuiltIns.getInstance().isUnit(expectedType) || ErrorUtils.isErrorType(expectedType);
    }
}
