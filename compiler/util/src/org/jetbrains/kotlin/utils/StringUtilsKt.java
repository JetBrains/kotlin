/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils;

import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * A copy of the subset of {@code com.intellij.openapi.util.text.StringUtil} used by the compiler.
 */
public final class StringUtilsKt {
    private StringUtilsKt() {
    }

    private static final String ELLIPSIS = "…";

    private static final List<String> REPLACES_REFS = Arrays.asList("&lt;", "&gt;", "&amp;", "&#39;", "&quot;");
    private static final List<String> REPLACES_DISP = Arrays.asList("<", ">", "&", "'", "\"");

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

    public static boolean endsWithLineBreak(@NotNull CharSequence text) {
        int len = text.length();
        return len > 0 && isLineBreak(text.charAt(len - 1));
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
                    if (ch == '\\') {
                        buffer.append("\\\\");
                    }
                    else if (additionalChars != null && additionalChars.indexOf(ch) > -1) {
                        buffer.append("\\").append(ch);
                    }
                    else if (!isPrintableUnicode(ch)) {
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
        }
        return buffer;
    }

    private static String toUpperCase(String s) {
        return s == null ? null : s.toUpperCase(Locale.ENGLISH);
    }

    public static @NotNull String unescapeStringCharacters(@NotNull String s) {
        StringBuilder buffer = new StringBuilder(s.length());
        int length = s.length();
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
        return buffer.toString();
    }

    private static boolean isOctalDigit(char c) {
        return '0' <= c && c <= '7';
    }

    public static @NotNull String unescapeChar(@NotNull String str, char unescapeChar) {
        StringBuilder buf = new StringBuilder(str.length());
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
        return buf.toString();
    }

    public static @NotNull String escapeToRegexp(@NotNull String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                result.append("\\n");
            }
            else if (c == '\r') {
                result.append("\\r");
            }
            else if (".$|()[]{}^?*+\\".indexOf(c) >= 0) {
                // escaping ']' and '}' is not required for most regex dialects,
                // but we do it for maximum compatibility
                result.append('\\').append(c);
            }
            else {
                result.append(c);
            }
        }

        return result.toString();
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

    private static boolean isEmpty(@Nullable CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    private static @NotNull String notNullize(@Nullable String s) {
        return notNullize(s, "");
    }

    public static @NotNull String notNullize(@Nullable String s, @NotNull String defaultValue) {
        return s == null ? defaultValue : s;
    }

    public static boolean isEmptyOrSpaces(@Nullable String s) {
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

    public static @NotNull String trimMiddle(@NotNull String text, int maxLength) {
        String symbol = ELLIPSIS;
        int textLength = text.length();
        if (textLength > maxLength) {
            int prefixLength = maxLength - (maxLength >> 1) - symbol.length();
            assert prefixLength >= 0;
            return text.substring(0, prefixLength) + symbol + text.substring(textLength - (maxLength >> 1));
        }
        else {
            return text;
        }
    }

    public static @NotNull String first(@NotNull String text, int maxLength, boolean appendEllipsis) {
        return text.length() > maxLength ? text.substring(0, maxLength) + (appendEllipsis ? "..." : "") : text;
    }

    public static @NotNull List<String> split(@NotNull String s, @NotNull String separator) {
        String[] separators = new String[1];
        separators[0] = separator;
        return StringsKt.split(s, separators, false, 0);
    }

    public static @NotNull String join(@NotNull Collection<String> strings, @NotNull String separator) {
        if (strings.size() <= 1) {
            return notNullize(strings.isEmpty() ? null : strings.iterator().next());
        }
        StringBuilder result = new StringBuilder();
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
        return result.toString();
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

    public static @NotNull String getShortName(@NotNull String fqName) {
        int lastPointIdx = fqName.lastIndexOf('.');
        if (lastPointIdx >= 0) {
            return fqName.substring(lastPointIdx + 1);
        }
        return fqName;
    }

    public static @NotNull String getPackageName(@NotNull String fqName) {
        int lastPointIdx = fqName.lastIndexOf('.');
        if (lastPointIdx >= 0) {
            return fqName.substring(0, lastPointIdx);
        }
        return "";
    }

    public static @NotNull String convertLineSeparators(@NotNull String text) {
        return convertLineSeparators(text, "\n");
    }

    public static @NotNull String convertLineSeparators(@NotNull String text, @NotNull String newSeparator) {
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
                if (buffer == null) {
                    buffer = new StringBuilder(text.length());
                    buffer.append(text, 0, intactLength);
                }
                buffer.append(newSeparator);
                if (followedByLineFeed) {
                    //noinspection AssignmentToForLoopParameter
                    i++;
                }
            }
            else if (buffer == null) {
                intactLength++;
            }
            else {
                buffer.append(c);
            }
        }
        return ((CharSequence) (buffer == null ? text : buffer)).toString();
    }
}
