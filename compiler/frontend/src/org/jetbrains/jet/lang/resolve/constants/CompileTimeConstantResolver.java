package org.jetbrains.jet.lang.resolve.constants;

import com.google.common.base.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.JetEscapeStringTemplateEntry;
import org.jetbrains.jet.lang.psi.JetLiteralStringTemplateEntry;
import org.jetbrains.jet.lang.psi.JetStringTemplateEntry;
import org.jetbrains.jet.lang.psi.JetVisitorVoid;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.types.*;

import java.util.List;

/**
 * @author abreslav
 */
public class CompileTimeConstantResolver {
    public static final ErrorValue OUT_OF_RANGE = new ErrorValue("The value is out of range");

    private final JetSemanticServices semanticServices;
    private final BindingTrace trace;

    public CompileTimeConstantResolver(@NotNull JetSemanticServices semanticServices, @NotNull BindingTrace trace) {
        this.semanticServices = semanticServices;
        this.trace = trace;
    }

    @NotNull
    public CompileTimeConstant<?> getIntegerValue(@NotNull String text, @NotNull JetType expectedType) {
        JetStandardLibrary standardLibrary = semanticServices.getStandardLibrary();
        if (noExpectedType(expectedType)) {
            Long value = parseLongValue(text);
            if (value == null) {
                return OUT_OF_RANGE;
            }

            if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
                return new IntValue(value.intValue());
            }
            return new LongValue(value);
        }
        Function<Long, ? extends CompileTimeConstant<?>> create;
        long lowerBound;
        long upperBound;
        TypeConstructor constructor = expectedType.getConstructor();
        if (constructor == standardLibrary.getInt().getTypeConstructor()) {
            create = IntValue.CREATE;
            lowerBound = Integer.MIN_VALUE;
            upperBound = Integer.MAX_VALUE;
        }
        else if (constructor == standardLibrary.getLong().getTypeConstructor()) {
            create = LongValue.CREATE;
            lowerBound = Long.MIN_VALUE;
            upperBound = Long.MAX_VALUE;
        }
        else if (constructor == standardLibrary.getShort().getTypeConstructor()) {
            create = ShortValue.CREATE;
            lowerBound = Short.MIN_VALUE;
            upperBound = Short.MAX_VALUE;
        }
        else if (constructor == standardLibrary.getByte().getTypeConstructor()) {
            create = ByteValue.CREATE;
            lowerBound = Byte.MIN_VALUE;
            upperBound = Byte.MAX_VALUE;
        }
        else  {
            JetTypeChecker typeChecker = semanticServices.getTypeChecker();
            JetType intType = standardLibrary.getIntType();
            JetType longType = standardLibrary.getLongType();
            if (typeChecker.isSubtypeOf(intType, expectedType)) {
                return getIntegerValue(text, intType);
            }
            else if (typeChecker.isSubtypeOf(longType, expectedType)) {
                return getIntegerValue(text, longType);
            }
            else {
                return new ErrorValue("An integer literal does not conform to the expected type " + expectedType);
            }
        }
        Long value = parseLongValue(text);

        if (value != null && lowerBound <= value && value <= upperBound) {
            return create.apply(value);
        }
        return new ErrorValue("An integer literal does not conform to the expected type " + expectedType);
    }

    @Nullable
    private static Long parseLongValue(String text) {
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

    @NotNull
    public CompileTimeConstant<?> getFloatValue(@NotNull String text, @NotNull JetType expectedType) {
        JetStandardLibrary standardLibrary = semanticServices.getStandardLibrary();
        if (noExpectedType(expectedType)
            || semanticServices.getTypeChecker().isSubtypeOf(standardLibrary.getDoubleType(), expectedType)) {
            try {
                return new DoubleValue(Double.parseDouble(text));
            }
            catch (NumberFormatException e) {
                return OUT_OF_RANGE;
            }
        }
        else if (semanticServices.getTypeChecker().isSubtypeOf(standardLibrary.getFloatType(), expectedType)) {
            try {
                return new DoubleValue(Float.parseFloat(text));
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
        if (!noExpectedType(expectedType)
            && !semanticServices.getTypeChecker().isSubtypeOf(nativeType, expectedType)) {
            return new ErrorValue("A " + title + " literal " + text + " does not conform to the expected type " + expectedType);
        }
        return null;
    }

    @NotNull
    public CompileTimeConstant<?> getBooleanValue(@NotNull String text, @NotNull JetType expectedType) {
        CompileTimeConstant<?> error = checkNativeType(text, expectedType, "boolean", semanticServices.getStandardLibrary().getBooleanType());
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
        CompileTimeConstant<?> error = checkNativeType(text, expectedType, "character", semanticServices.getStandardLibrary().getCharType());
        if (error != null) {
            return error;
        }

        // Strip the quotes
        if (text.charAt(0) != '\'' || text.charAt(text.length() - 1) != '\'') {
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
            return new ErrorValue("Too many characters in a character literal" + text);
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
    public CompileTimeConstant<?> getRawStringValue(@NotNull String unescapedText, @NotNull JetType expectedType) {
        CompileTimeConstant<?> error = checkNativeType("\"\"\"...\"\"\"", expectedType, "string", semanticServices.getStandardLibrary().getStringType());
        if (error != null) {
            return error;
        }
        return new StringValue(unescapedText);
    }

    @NotNull
    public CompileTimeConstant<?> getEscapedStringValue(@NotNull List<JetStringTemplateEntry> entries, @NotNull JetType expectedType) {
        CompileTimeConstant<?> error = checkNativeType("\"...\"", expectedType, "string", semanticServices.getStandardLibrary().getStringType());
        if (error != null) {
            return error;
        }
        final StringBuilder builder = new StringBuilder();
        final CompileTimeConstant<?>[] result = new CompileTimeConstant<?>[1];
        for (JetStringTemplateEntry entry : entries) {
            entry.accept(new JetVisitorVoid() {
                @Override
                public void visitStringTemplateEntry(JetStringTemplateEntry entry) {
                    result[0] =  new ErrorValue("String templates are not allowed in compile-time constants");
                }

                @Override
                public void visitLiteralStringTemplateEntry(JetLiteralStringTemplateEntry entry) {
                    builder.append(entry.getText());
                }

                @Override
                public void visitEscapeStringTemplateEntry(JetEscapeStringTemplateEntry entry) {
                    String text = entry.getText();
                    assert text.length() == 2 && text.charAt(0) == '\\';
                    Character character = translateEscape(text.charAt(1));
                    if (character != null) {
                        builder.append(character);
                    }
                }
            });
            if (result[0] != null) {
                return result[0];
            }
        }
        return new StringValue(builder.toString());
    }

    @NotNull
    public CompileTimeConstant<?> getNullValue(@NotNull JetType expectedType) {
        if (noExpectedType(expectedType) || expectedType.isNullable()) {
            return NullValue.NULL;
        }
        return new ErrorValue("Null can not be a value of a non-null type " + expectedType);
    }

    private boolean noExpectedType(JetType expectedType) {
        return expectedType == TypeUtils.NO_EXPECTED_TYPE || JetStandardClasses.isUnit(expectedType) || ErrorUtils.isErrorType(expectedType);
    }

}
