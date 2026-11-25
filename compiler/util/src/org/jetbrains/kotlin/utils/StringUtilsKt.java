/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.beans.Introspector;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A copy of the subset of {@code com.intellij.openapi.util.text.StringUtil} used by the compiler.
 */
public final class StringUtilsKt {
    private StringUtilsKt() {
    }

    private static final String ELLIPSIS = "…";

    private static final Pattern EOL_SPLIT_KEEP_SEPARATORS = Pattern.compile("(?<=(\r\n|\n))|(?<=\r)(?=[^\n])");

    private static final List<String> REPLACES_REFS = Arrays.asList("&lt;", "&gt;", "&amp;", "&#39;", "&quot;");
    private static final List<String> REPLACES_DISP = Arrays.asList("<", ">", "&", "'", "\"");

    //region Case conversion

    public static String toUpperCase(String s) {
        return s == null ? null : s.toUpperCase(Locale.ENGLISH);
    }

    public static String toLowerCase(String str) {
        return str == null ? null : str.toLowerCase(Locale.ENGLISH);
    }

    private static char toUpperCase(char ch) {
        if (ch <= 0x7F) {
            if (ch >= 'a' && ch <= 'z') {
                //in ASCII lower and upper case letters differ by a single bit:
                return (char) (ch & 0b1101_1111);
            }
            return ch;
        }
        return Character.toUpperCase(ch);
    }

    private static char toLowerCase(char ch) {
        if (ch <= 0x7F) {
            if (ch >= 'A' && ch <= 'Z') {
                //in ASCII lower and upper case letters differ by a single bit:
                return (char) (ch | 0b0010_0000);
            }
            return ch;
        }
        return Character.toLowerCase(ch);
    }

    private static int compare(char c1, char c2, boolean ignoreCase) {
        // duplicating String.equalsIgnoreCase logic
        int d = c1 - c2;
        if (d == 0 || !ignoreCase) {
            return d;
        }
        // If characters don't match but case may be ignored,
        // try converting both characters to uppercase.
        // If the results match, then the comparison scan should continue.
        char u1 = toUpperCase(c1);
        char u2 = toUpperCase(c2);
        d = u1 - u2;
        if (d != 0) {
            // Unfortunately, conversion to uppercase does not work properly
            // for the Georgian alphabet, which has strange rules about case
            // conversion.  So we need to make one last check before exiting.
            d = toLowerCase(u1) - toLowerCase(u2);
        }
        return d;
    }

    private static boolean charsMatch(char c1, char c2, boolean ignoreCase) {
        return compare(c1, c2, ignoreCase) == 0;
    }

    public static @NotNull String capitalize(@NotNull String s) {
        if (s.isEmpty()) return s;
        if (s.length() == 1) return toUpperCase(s);
        if (Character.isUpperCase(s.charAt(0))) return s;
        return toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static @NotNull String decapitalize(@NotNull String s) {
        return Introspector.decapitalize(s);
    }

    //endregion

    //region Line breaks and offsets

    private static boolean isLineBreak(char c) {
        return c == '\n' || c == '\r';
    }

    public static int getLineBreakCount(@NotNull CharSequence text) {
        int count = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\n') {
                count++;
            }
            else if (c == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                count++;
            }
            i++;
        }
        return count;
    }

    public static boolean containsLineBreak(@NotNull CharSequence text) {
        for (int i = 0; i < text.length(); i++) {
            if (isLineBreak(text.charAt(i))) return true;
        }
        return false;
    }

    public static @NotNull String escapeLineBreak(@NotNull String text) {
        StringBuilder buffer = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\n':
                    buffer.append("\\n");
                    break;
                case '\r':
                    buffer.append("\\r");
                    break;
                default:
                    buffer.append(c);
            }
        }
        return buffer.toString();
    }

    public static boolean endsWithLineBreak(@NotNull CharSequence text) {
        int len = text.length();
        return len > 0 && isLineBreak(text.charAt(len - 1));
    }

    public static int countNewLines(@NotNull CharSequence text) {
        return countChars(text, '\n');
    }

    private static int countChars(@NotNull CharSequence text, char c) {
        return countChars(text, c, 0, false);
    }

    private static int countChars(@NotNull CharSequence text, char c, int offset, boolean stopAtOtherChar) {
        return countChars(text, c, offset, text.length(), stopAtOtherChar);
    }

    private static int countChars(@NotNull CharSequence text, char c, int start, int end, boolean stopAtOtherChar) {
        boolean forward = start <= end;
        start = forward ? Math.max(0, start) : Math.min(text.length(), start);
        end = forward ? Math.min(text.length(), end) : Math.max(0, end);
        int count = 0;
        for (int i = forward ? start : start - 1; forward == i < end; i += forward ? 1 : -1) {
            if (text.charAt(i) == c) {
                count++;
            }
            else if (stopAtOtherChar) {
                break;
            }
        }
        return count;
    }

    public static int offsetToLineNumber(@NotNull CharSequence text, int offset) {
        LineColumn lineColumn = offsetToLineColumn(text, offset);
        return lineColumn != null ? lineColumn.line : -1;
    }

    public static LineColumn offsetToLineColumn(@NotNull CharSequence text, int offset) {
        int curLine = 0;
        int curLineStart = 0;
        int curOffset = 0;
        while (curOffset < offset) {
            if (curOffset == text.length()) return null;
            char c = text.charAt(curOffset);
            if (c == '\n') {
                curLine++;
                curLineStart = curOffset + 1;
            }
            else if (c == '\r') {
                curLine++;
                if (curOffset < text.length() - 1 && text.charAt(curOffset + 1) == '\n') {
                    curOffset++;
                }
                curLineStart = curOffset + 1;
            }
            curOffset++;
        }

        return LineColumn.of(curLine, offset - curLineStart);
    }

    public static final class LineColumn {
        public final int line;
        public final int column;

        private LineColumn(int line, int column) {
            this.line = line;
            this.column = column;
        }

        public static @NotNull LineColumn of(int line, int column) {
            return new LineColumn(line, column);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LineColumn other = (LineColumn) o;

            if (line != other.line) return false;
            if (column != other.column) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = line;
            result = 31 * result + column;
            return result;
        }

        @Override
        public String toString() {
            return "LineColumn{line=" + line + ", column=" + column + '}';
        }
    }

    //endregion

    //region Java identifiers

    private static boolean isJavaIdentifierStart(int cp) {
        return cp >= 'a' && cp <= 'z' || cp >= 'A' && cp <= 'Z' || Character.isJavaIdentifierStart(cp);
    }

    private static boolean isJavaIdentifierPart(int cp) {
        return cp >= '0' && cp <= '9' || cp >= 'a' && cp <= 'z' || cp >= 'A' && cp <= 'Z' || Character.isJavaIdentifierPart(cp);
    }

    public static boolean isJavaIdentifier(@NotNull String text) {
        int len = text.length();
        if (len == 0) return false;
        int point = text.codePointAt(0);
        if (!isJavaIdentifierStart(point)) return false;
        int i = Character.charCount(point);

        while (i < len) {
            point = text.codePointAt(i);
            if (!isJavaIdentifierPart(point)) return false;
            i += Character.charCount(point);
        }
        return true;
    }

    public static @NotNull String sanitizeJavaIdentifier(@NotNull String name) {
        StringBuilder result = new StringBuilder(name.length());

        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isJavaIdentifierPart(ch)) {
                if (result.length() == 0 && !Character.isJavaIdentifierStart(ch)) {
                    result.append("_");
                }
                result.append(ch);
            }
        }

        return result.toString();
    }

    //endregion

    //region Escaping

    private static boolean isPrintableUnicode(char c) {
        int t = Character.getType(c);
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return t != Character.UNASSIGNED && t != Character.LINE_SEPARATOR && t != Character.PARAGRAPH_SEPARATOR &&
               t != Character.CONTROL && t != Character.FORMAT && t != Character.PRIVATE_USE && t != Character.SURROGATE &&
               block != Character.UnicodeBlock.VARIATION_SELECTORS && block != Character.UnicodeBlock.VARIATION_SELECTORS_SUPPLEMENT;
    }

    public static @NotNull String escapeStringCharacters(@NotNull String s) {
        StringBuilder buffer = new StringBuilder(s.length());
        escapeStringCharacters(s.length(), s, "\"", buffer);
        return buffer.toString();
    }

    public static @NotNull String escapeCharCharacters(@NotNull String s) {
        StringBuilder buffer = new StringBuilder(s.length());
        escapeStringCharacters(s.length(), s, "'", buffer);
        return buffer.toString();
    }

    public static @NotNull StringBuilder escapeStringCharacters(
            int length,
            @NotNull String str,
            @Nullable String additionalChars,
            @NotNull StringBuilder buffer
    ) {
        return escapeStringCharacters(length, str, additionalChars, true, buffer);
    }

    private static @NotNull StringBuilder escapeStringCharacters(
            int length,
            @NotNull String str,
            @Nullable String additionalChars,
            boolean escapeSlash,
            @NotNull StringBuilder buffer
    ) {
        return escapeStringCharacters(length, str, additionalChars, escapeSlash, true, buffer);
    }

    private static @NotNull StringBuilder escapeStringCharacters(
            int length,
            @NotNull String str,
            @Nullable String additionalChars,
            boolean escapeSlash,
            boolean escapeUnicode,
            @NotNull StringBuilder buffer
    ) {
        char prev = 0;
        for (int idx = 0; idx < length; idx++) {
            char ch = str.charAt(idx);
            switch (ch) {
                case '\b':
                    buffer.append("\\b");
                    break;

                case '\t':
                    buffer.append("\\t");
                    break;

                case '\n':
                    buffer.append("\\n");
                    break;

                case '\f':
                    buffer.append("\\f");
                    break;

                case '\r':
                    buffer.append("\\r");
                    break;

                default:
                    if (escapeSlash && ch == '\\') {
                        buffer.append("\\\\");
                    }
                    else if (additionalChars != null && additionalChars.indexOf(ch) > -1 && (escapeSlash || prev != '\\')) {
                        buffer.append("\\").append(ch);
                    }
                    else if (escapeUnicode && !isPrintableUnicode(ch)) {
                        CharSequence hexCode = toUpperCase(Integer.toHexString(ch));
                        buffer.append("\\u");
                        int paddingCount = 4 - hexCode.length();
                        while (paddingCount-- > 0) {
                            buffer.append(0);
                        }
                        buffer.append(hexCode);
                    }
                    else {
                        buffer.append(ch);
                    }
            }
            prev = ch;
        }
        return buffer;
    }

    public static @NotNull String unescapeStringCharacters(@NotNull String s) {
        StringBuilder buffer = new StringBuilder(s.length());
        unescapeStringCharacters(s.length(), s, buffer);
        return buffer.toString();
    }

    private static void unescapeStringCharacters(int length, @NotNull String s, @NotNull StringBuilder buffer) {
        boolean escaped = false;
        for (int idx = 0; idx < length; idx++) {
            char ch = s.charAt(idx);
            if (!escaped) {
                if (ch == '\\') {
                    escaped = true;
                }
                else {
                    buffer.append(ch);
                }
            }
            else {
                int octalEscapeMaxLength = 2;
                switch (ch) {
                    case 'n':
                        buffer.append('\n');
                        break;

                    case 'r':
                        buffer.append('\r');
                        break;

                    case 'b':
                        buffer.append('\b');
                        break;

                    case 't':
                        buffer.append('\t');
                        break;

                    case 'f':
                        buffer.append('\f');
                        break;

                    case '\'':
                        buffer.append('\'');
                        break;

                    case '\"':
                        buffer.append('\"');
                        break;

                    case '\\':
                        buffer.append('\\');
                        break;

                    case 'u':
                        if (idx + 4 < length) {
                            try {
                                int code = Integer.parseInt(s.substring(idx + 1, idx + 5), 16);
                                //noinspection AssignmentToForLoopParameter
                                idx += 4;
                                buffer.append((char) code);
                            }
                            catch (NumberFormatException e) {
                                buffer.append("\\u");
                            }
                        }
                        else {
                            buffer.append("\\u");
                        }
                        break;

                    case '0':
                    case '1':
                    case '2':
                    case '3':
                        octalEscapeMaxLength = 3;
                        //noinspection fallthrough
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                        int escapeEnd = idx + 1;
                        while (escapeEnd < length && escapeEnd < idx + octalEscapeMaxLength && isOctalDigit(s.charAt(escapeEnd))) {
                            escapeEnd++;
                        }
                        try {
                            buffer.append((char) Integer.parseInt(s.substring(idx, escapeEnd), 8));
                        }
                        catch (NumberFormatException e) {
                            throw new RuntimeException("Couldn't parse " + s.substring(idx, escapeEnd), e); // shouldn't happen
                        }
                        //noinspection AssignmentToForLoopParameter
                        idx = escapeEnd - 1;
                        break;

                    default:
                        buffer.append(ch);
                        break;
                }
                escaped = false;
            }
        }

        if (escaped) buffer.append('\\');
    }

    private static boolean isOctalDigit(char c) {
        return '0' <= c && c <= '7';
    }

    public static @NotNull String unescapeChar(@NotNull String str, char unescapeChar) {
        StringBuilder buf = new StringBuilder(str.length());
        unescapeChar(buf, str, unescapeChar);
        return buf.toString();
    }

    private static void unescapeChar(@NotNull StringBuilder buf, @NotNull String str, char unescapeChar) {
        int length = str.length();
        int last = length - 1;
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            if (ch == '\\' && i != last) {
                //noinspection AssignmentToForLoopParameter
                i++;
                ch = str.charAt(i);
                if (ch != unescapeChar) buf.append('\\');
            }

            buf.append(ch);
        }
    }

    public static @NotNull String escapeToRegexp(@NotNull String text) {
        StringBuilder result = new StringBuilder(text.length());
        return escapeToRegexp(text, result).toString();
    }

    private static @NotNull StringBuilder escapeToRegexp(@NotNull CharSequence text, @NotNull StringBuilder builder) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                builder.append("\\n");
            }
            else if (c == '\r') {
                builder.append("\\r");
            }
            else if (".$|()[]{}^?*+\\".indexOf(c) >= 0) {
                // escaping ']' and '}' is not required for most regex dialects,
                // but we do it for maximum compatibility
                builder.append('\\').append(c);
            }
            else {
                builder.append(c);
            }
        }

        return builder;
    }

    public static @NotNull String escapeXmlEntities(@NotNull String text) {
        return replace(text, REPLACES_DISP, REPLACES_REFS);
    }

    private static @NotNull String replace(@NotNull String text, @NotNull List<String> from, @NotNull List<String> to) {
        assert from.size() == to.size();
        StringBuilder result = null;
        replace:
        for (int i = 0; i < text.length(); i++) {
            for (int j = 0; j < from.size(); j += 1) {
                String toReplace = from.get(j);
                String replaceWith = to.get(j);

                int len = toReplace.length();
                if (len == 0) continue;
                if (text.regionMatches(i, toReplace, 0, len)) {
                    if (result == null) {
                        result = new StringBuilder(text.length());
                        result.append(text, 0, i);
                    }
                    result.append(replaceWith);
                    //noinspection AssignmentToForLoopParameter
                    i += len - 1;
                    continue replace;
                }
            }

            if (result != null) {
                result.append(text.charAt(i));
            }
        }
        return result == null ? text : result.toString();
    }

    public static @NotNull String removeHtmlTags(@NotNull String htmlString) {
        return removeHtmlTags(htmlString, false);
    }

    // the `Logger.getInstance(StringUtil.class).error(e)` call is adapted to a RuntimeException
    private static @NotNull String removeHtmlTags(@NotNull String htmlString, boolean isRemoveStyleTag) {
        if (isEmpty(htmlString)) {
            return "";
        }

        MyHtml2Text parser = isRemoveStyleTag ? new MyHtml2Text(true) : new MyHtml2Text(false);
        try {
            parser.parse(new StringReader(htmlString));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return parser.getText();
    }

    private static final class MyHtml2Text extends HTMLEditorKit.ParserCallback {
        private final @NotNull StringBuilder myBuffer = new StringBuilder();
        private final boolean myIsSkipStyleTag;

        private boolean myIsStyleTagOpened;

        private MyHtml2Text(boolean isSkipStyleTag) {
            myIsSkipStyleTag = isSkipStyleTag;
        }

        void parse(@NotNull Reader in) throws IOException {
            myBuffer.setLength(0);
            new ParserDelegator().parse(in, this, Boolean.TRUE);
        }

        @Override
        public void handleText(char[] text, int pos) {
            if (!myIsStyleTagOpened) {
                myBuffer.append(text);
            }
        }

        @Override
        public void handleStartTag(@NotNull HTML.Tag tag, MutableAttributeSet set, int i) {
            if (myIsSkipStyleTag && "style".equals(tag.toString())) {
                myIsStyleTagOpened = true;
            }
            handleTag(tag);
        }

        @Override
        public void handleEndTag(@NotNull HTML.Tag tag, int pos) {
            if (myIsSkipStyleTag && "style".equals(tag.toString())) {
                myIsStyleTagOpened = false;
            }
        }

        @Override
        public void handleSimpleTag(@NotNull HTML.Tag tag, MutableAttributeSet set, int i) {
            handleTag(tag);
        }

        private void handleTag(@NotNull HTML.Tag tag) {
            if (tag.breaksFlow() && myBuffer.length() > 0) {
                myBuffer.append(System.lineSeparator());
            }
        }

        @NotNull String getText() {
            return myBuffer.toString();
        }
    }

    //endregion

    //region Quoting

    private static boolean isQuotedString(@NotNull String s) {
        int length = s.length();
        if (length <= 1) return false;
        char firstChar = s.charAt(0);
        if (firstChar != '\'' && firstChar != '\"') return false;
        return firstChar == s.charAt(length - 1);
    }

    public static @NotNull String unquoteString(@NotNull String s) {
        return isQuotedString(s) ? s.substring(1, s.length() - 1) : s;
    }

    //endregion

    //region Emptiness and nullability

    public static boolean isEmpty(@Nullable String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isEmpty(@Nullable CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isNotEmpty(@Nullable String s) {
        return !isEmpty(s);
    }

    public static @NotNull String notNullize(@Nullable String s) {
        return notNullize(s, "");
    }

    public static @NotNull String notNullize(@Nullable String s, @NotNull String defaultValue) {
        return s == null ? defaultValue : s;
    }

    public static boolean isEmptyOrSpaces(@Nullable String s) {
        return isEmptyOrSpaces((CharSequence) s);
    }

    private static boolean isEmptyOrSpaces(@Nullable CharSequence s) {
        if (isEmpty(s)) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > ' ') {
                return false;
            }
        }
        return true;
    }

    public static boolean containsChar(@NotNull String value, char ch) {
        return value.indexOf(ch) >= 0;
    }

    //endregion

    //region Trimming and shortening

    public static @NotNull String trimLeading(@NotNull String string) {
        return trimLeading((CharSequence) string).toString();
    }

    private static @NotNull CharSequence trimLeading(@NotNull CharSequence string) {
        int index = 0;
        while (index < string.length() && Character.isWhitespace(string.charAt(index))) index++;
        return string.subSequence(index, string.length());
    }

    public static @NotNull String trimTrailing(@NotNull String string) {
        return trimTrailing((CharSequence) string).toString();
    }

    private static @NotNull CharSequence trimTrailing(@NotNull CharSequence string) {
        int index = string.length() - 1;
        while (index >= 0 && Character.isWhitespace(string.charAt(index))) index--;
        return string.subSequence(0, index + 1);
    }

    public static boolean startsWithChar(@Nullable CharSequence s, char prefix) {
        return s != null && s.length() != 0 && s.charAt(0) == prefix;
    }

    public static @NotNull String trimStart(@NotNull String s, @NotNull String prefix) {
        if (s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    public static @NotNull String trimMiddle(@NotNull String text, int maxLength) {
        return shortenTextWithEllipsis(text, maxLength, maxLength >> 1, true);
    }

    private static @NotNull String shortenTextWithEllipsis(
            @NotNull String text,
            int maxLength,
            int suffixLength,
            boolean useEllipsisSymbol
    ) {
        String symbol = useEllipsisSymbol ? ELLIPSIS : "...";
        return shortenTextWithEllipsis(text, maxLength, suffixLength, symbol);
    }

    private static @NotNull String shortenTextWithEllipsis(@NotNull String text, int maxLength, int suffixLength, @NotNull String symbol) {
        int textLength = text.length();
        if (textLength > maxLength) {
            int prefixLength = maxLength - suffixLength - symbol.length();
            assert prefixLength >= 0;
            return text.substring(0, prefixLength) + symbol + text.substring(textLength - suffixLength);
        }
        else {
            return text;
        }
    }

    public static @NotNull String first(@NotNull String text, int maxLength, boolean appendEllipsis) {
        return text.length() > maxLength ? text.substring(0, maxLength) + (appendEllipsis ? "..." : "") : text;
    }

    //endregion

    //region Splitting and joining

    public static @NotNull List<String> splitHonorQuotes(@NotNull String s, char separator) {
        List<String> result = new ArrayList<>();
        StringBuilder builder = new StringBuilder(s.length());
        char quote = 0;
        boolean isEscaped = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean isSeparator = c == separator;
            boolean isQuote = c == '"' || c == '\'';
            boolean isQuoted = quote != 0;
            boolean isEscape = c == '\\';

            if (!isQuoted && isSeparator) {
                if (builder.length() > 0) {
                    result.add(builder.toString());
                    builder.setLength(0);
                }
                continue;
            }

            if (!isEscaped && isQuote && (quote == 0 || quote == c)) {
                quote = isQuoted ? 0 : c;
            }

            isEscaped = isEscape && !isEscaped;

            builder.append(c);
        }
        if (builder.length() > 0) {
            result.add(builder.toString());
        }
        return Collections.unmodifiableList(result);
    }

    public static @NotNull List<String> split(@NotNull String s, @NotNull String separator) {
        return split(s, separator, true);
    }

    public static @NotNull List<String> split(@NotNull String s, @NotNull String separator, boolean excludeSeparator) {
        return split(s, separator, excludeSeparator, true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static @NotNull List<String> split(
            @NotNull String s,
            @NotNull String separator,
            boolean excludeSeparator,
            boolean excludeEmptyStrings
    ) {
        return (List) split((CharSequence) s, separator, excludeSeparator, excludeEmptyStrings);
    }

    // FreezableArrayList is replaced by ArrayList
    private static @NotNull List<CharSequence> split(
            @NotNull CharSequence s,
            @NotNull CharSequence separator,
            boolean excludeSeparator,
            boolean excludeEmptyStrings
    ) {
        if (separator.length() == 0) {
            return Collections.singletonList(s);
        }
        List<CharSequence> result = new ArrayList<>();
        int pos = 0;
        while (true) {
            int index = indexOf(s, separator, pos);
            if (index == -1) break;
            int nextPos = index + separator.length();
            CharSequence token = s.subSequence(pos, excludeSeparator ? index : nextPos);
            if (token.length() != 0 || !excludeEmptyStrings) {
                result.add(token);
            }
            pos = nextPos;
        }
        if (pos < s.length() || !excludeEmptyStrings && pos == s.length()) {
            result.add(s.subSequence(pos, s.length()));
        }
        return result;
    }

    private static int indexOf(@NotNull CharSequence sequence, @NotNull CharSequence infix, int start) {
        return indexOf(sequence, infix, start, sequence.length());
    }

    private static int indexOf(@NotNull CharSequence sequence, @NotNull CharSequence infix, int start, int end) {
        for (int i = start; i <= end - infix.length(); i++) {
            if (startsWith(sequence, i, infix)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean startsWith(@NotNull CharSequence text, int startIndex, @NotNull CharSequence prefix) {
        int tl = text.length();
        if (startIndex < 0 || startIndex > tl) {
            throw new IllegalArgumentException("Index is out of bounds: " + startIndex + ", length: " + tl);
        }
        int l1 = tl - startIndex;
        int l2 = prefix.length();
        if (l1 < l2) return false;

        for (int i = 0; i < l2; i++) {
            if (text.charAt(i + startIndex) != prefix.charAt(i)) return false;
        }
        return true;
    }

    public static String[] splitByLinesKeepSeparators(@NotNull String string) {
        return EOL_SPLIT_KEEP_SEPARATORS.split(string);
    }

    public static @NotNull String join(@NotNull Collection<String> strings, @NotNull String separator) {
        if (strings.size() <= 1) {
            return notNullize(strings.isEmpty() ? null : strings.iterator().next());
        }
        StringBuilder result = new StringBuilder();
        join(strings, separator, result);
        return result.toString();
    }

    private static void join(@NotNull Collection<String> strings, @NotNull String separator, @NotNull StringBuilder result) {
        boolean isFirst = true;
        for (String string : strings) {
            if (string != null) {
                if (isFirst) {
                    isFirst = false;
                }
                else {
                    result.append(separator);
                }
                result.append(string);
            }
        }
    }

    public static @NotNull String join(@NotNull Iterable<?> items, @NotNull String separator) {
        StringBuilder result = new StringBuilder();
        for (Object item : items) {
            result.append(item).append(separator);
        }
        if (result.length() > 0) {
            result.setLength(result.length() - separator.length());
        }
        return result.toString();
    }

    // com.intellij.util.Function is replaced by java.util.function.Function
    public static <T> @NotNull String join(
            @NotNull Iterable<? extends T> items,
            @NotNull Function<? super T, ? extends CharSequence> f,
            @NotNull String separator
    ) {
        StringBuilder result = new StringBuilder();
        join(items, f, separator, result);
        return result.toString();
    }

    // com.intellij.util.Function is replaced by java.util.function.Function
    private static <T> void join(
            @NotNull Iterable<? extends T> items,
            @NotNull Function<? super T, ? extends CharSequence> f,
            @NotNull String separator,
            @NotNull StringBuilder result
    ) {
        boolean isFirst = true;
        for (T item : items) {
            CharSequence string = f.apply(item);
            if (!isEmpty(string)) {
                if (isFirst) {
                    isFirst = false;
                }
                else {
                    result.append(separator);
                }
                result.append(string);
            }
        }
    }

    //endregion

    //region Search and misc

    public static @NotNull List<String> findMatches(@NotNull String s, @NotNull Pattern pattern) {
        return findMatches(s, pattern, 1);
    }

    // FreezableArrayList is replaced by ArrayList
    private static @NotNull List<String> findMatches(@NotNull String s, @NotNull Pattern pattern, int groupIndex) {
        List<String> result = new ArrayList<>();
        Matcher m = pattern.matcher(s);
        while (m.find()) {
            String group = m.group(groupIndex);
            if (group != null) {
                result.add(group);
            }
        }
        return result;
    }

    public static int commonPrefixLength(@NotNull CharSequence s1, @NotNull CharSequence s2) {
        return commonPrefixLength(s1, s2, false);
    }

    private static int commonPrefixLength(@NotNull CharSequence s1, @NotNull CharSequence s2, boolean ignoreCase) {
        int i;
        int minLength = Math.min(s1.length(), s2.length());
        for (i = 0; i < minLength; i++) {
            if (!charsMatch(s1.charAt(i), s2.charAt(i), ignoreCase)) {
                break;
            }
        }
        return i;
    }

    public static int commonSuffixLength(@NotNull CharSequence s1, @NotNull CharSequence s2) {
        int s1Length = s1.length();
        int s2Length = s2.length();
        if (s1Length == 0 || s2Length == 0) return 0;
        int i;
        for (i = 0; i < s1Length && i < s2Length; i++) {
            if (s1.charAt(s1Length - i - 1) != s2.charAt(s2Length - i - 1)) {
                break;
            }
        }
        return i;
    }

    public static @Nullable String substringAfter(@NotNull String text, @NotNull String subString) {
        int i = text.indexOf(subString);
        if (i == -1) return null;
        return text.substring(i + subString.length());
    }

    public static @NotNull String replace(@NotNull String text, @NotNull String oldS, @NotNull String newS) {
        if (text.length() < oldS.length()) return text;

        StringBuilder newText = null;
        int i = 0;

        while (i < text.length()) {
            int index = text.indexOf(oldS, i);
            if (index < 0) {
                if (i == 0) {
                    return text;
                }

                newText.append(text, i, text.length());
                break;
            }
            else {
                if (newText == null) {
                    if (text.length() == oldS.length()) {
                        return newS;
                    }
                    newText = new StringBuilder(text.length() - i);
                }

                newText.append(text, i, index);
                newText.append(newS);
                i = index + oldS.length();
            }
        }
        return newText != null ? newText.toString() : "";
    }

    public static int comparePairs(@Nullable String s1, @Nullable String t1, @Nullable String s2, @Nullable String t2, boolean ignoreCase) {
        int compare = compare(s1, s2, ignoreCase);
        return compare != 0 ? compare : compare(t1, t2, ignoreCase);
    }

    private static int compare(@Nullable String s1, @Nullable String s2, boolean ignoreCase) {
        if (s1 == s2) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        return ignoreCase ? s1.compareToIgnoreCase(s2) : s1.compareTo(s2);
    }

    public static boolean equals(@Nullable CharSequence s1, @Nullable CharSequence s2) {
        return equal(s1, s2, true);
    }

    private static boolean equal(@Nullable CharSequence s1, @Nullable CharSequence s2, boolean caseSensitive) {
        if (s1 == s2) return true;
        if (s1 == null || s2 == null) return false;

        if (s1.length() != s2.length()) return false;

        if (caseSensitive) {
            for (int i = 0; i < s1.length(); i++) {
                if (s1.charAt(i) != s2.charAt(i)) {
                    return false;
                }
            }
        }
        else {
            for (int i = 0; i < s1.length(); i++) {
                if (!charsEqualIgnoreCase(s1.charAt(i), s2.charAt(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean charsEqualIgnoreCase(char a, char b) {
        return a == b || toUpperCase(a) == toUpperCase(b) || toLowerCase(a) == toLowerCase(b);
    }

    public static @NotNull String getShortName(@NotNull String fqName) {
        return getShortName(fqName, '.');
    }

    private static @NotNull String getShortName(@NotNull String fqName, char separator) {
        int lastPointIdx = fqName.lastIndexOf(separator);
        if (lastPointIdx >= 0) {
            return fqName.substring(lastPointIdx + 1);
        }
        return fqName;
    }

    public static @NotNull String getPackageName(@NotNull String fqName) {
        return getPackageName(fqName, '.');
    }

    private static @NotNull String getPackageName(@NotNull String fqName, char separator) {
        int lastPointIdx = fqName.lastIndexOf(separator);
        if (lastPointIdx >= 0) {
            return fqName.substring(0, lastPointIdx);
        }
        return "";
    }

    //endregion

    //region Line separator conversion

    public static @NotNull String convertLineSeparators(@NotNull String text) {
        return convertLineSeparators(text, "\n");
    }

    public static @NotNull String convertLineSeparators(@NotNull String text, @NotNull String newSeparator) {
        return convertLineSeparators(text, newSeparator, null, false);
    }

    @NotNull
    private static String convertLineSeparators(
            @NotNull String text,
            @NotNull String newSeparator,
            int[] offsetsToKeep,
            boolean keepCarriageReturn
    ) {
        return unifyLineSeparators(text, newSeparator, offsetsToKeep, keepCarriageReturn).toString();
    }

    @NotNull
    private static CharSequence unifyLineSeparators(
            @NotNull CharSequence text,
            @NotNull String newSeparator,
            int[] offsetsToKeep,
            boolean keepCarriageReturn
    ) {
        StringBuilder buffer = null;
        int intactLength = 0;
        boolean newSeparatorIsSlashN = "\n".equals(newSeparator);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                if (!newSeparatorIsSlashN) {
                    if (buffer == null) {
                        buffer = new StringBuilder(text.length());
                        buffer.append(text, 0, intactLength);
                    }
                    buffer.append(newSeparator);
                    shiftOffsets(offsetsToKeep, buffer.length(), 1, newSeparator.length());
                }
                else if (buffer == null) {
                    intactLength++;
                }
                else {
                    buffer.append('\n');
                }
            }
            else if (c == '\r') {
                boolean followedByLineFeed = i < text.length() - 1 && text.charAt(i + 1) == '\n';
                if (!followedByLineFeed && keepCarriageReturn) {
                    if (buffer == null) {
                        intactLength++;
                    }
                    else {
                        buffer.append('\r');
                    }
                    continue;
                }
                if (buffer == null) {
                    buffer = new StringBuilder(text.length());
                    buffer.append(text, 0, intactLength);
                }
                buffer.append(newSeparator);
                if (followedByLineFeed) {
                    //noinspection AssignmentToForLoopParameter
                    i++;
                    shiftOffsets(offsetsToKeep, buffer.length(), 2, newSeparator.length());
                }
                else {
                    shiftOffsets(offsetsToKeep, buffer.length(), 1, newSeparator.length());
                }
            }
            else if (buffer == null) {
                intactLength++;
            }
            else {
                buffer.append(c);
            }
        }
        return buffer == null ? text : buffer;
    }

    private static void shiftOffsets(int[] offsets, int changeOffset, int oldLength, int newLength) {
        if (offsets == null) return;
        int shift = newLength - oldLength;
        if (shift == 0) return;
        for (int i = 0; i < offsets.length; i++) {
            int offset = offsets[i];
            if (offset >= changeOffset + oldLength) {
                offsets[i] += shift;
            }
        }
    }

    //endregion
}
