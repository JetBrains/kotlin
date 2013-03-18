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

package org.jetbrains.jet;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public final class InTextDirectivesUtils {

    private InTextDirectivesUtils() {
    }

    @Nullable
    public static Integer getPrefixedInt(String fileText, String prefix) {
        String[] numberStrings = findArrayWithPrefix(fileText, prefix);
        if (numberStrings.length > 0) {
            return Integer.parseInt(numberStrings[0]);
        }

        return null;
    }

    @NotNull
    public static String[] findArrayWithPrefix(String fileText, String... prefixes) {
        return ArrayUtil.toStringArray(findListWithPrefix(fileText, prefixes));
    }

    @NotNull
    public static List<String> findListWithPrefix(String fileText, String... prefixes) {
        List<String> result = new ArrayList<String>();

        for (String line : findLinesWithPrefixRemoved(fileText, prefixes)) {
            String[] variants = line.split(",");

            for (String variant : variants) {
                result.add(StringUtil.unquoteString(variant.trim()));
            }
        }

        return result;
    }

    @Nullable
    public static String findStringWithPrefix(String fileText, String... prefixes) {
        List<String> strings = findListWithPrefix(fileText, prefixes);
        if (strings.isEmpty()) {
            return null;
        }
        assert strings.size() == 1 : "There is more than one string with given prefixes " +
                                     Arrays.toString(prefixes) + ". Use findListWithPrefix() instead.";
        return strings.get(0);
    }

    @NotNull
    public static List<String> findLinesWithPrefixRemoved(String fileText, String... prefixes) {
        List<String> result = new ArrayList<String>();
        List<String> cleanedPrefixes = cleanDirectivesFromComments(Arrays.asList(prefixes));

        for (String line : fileNonEmptyCommentedLines(fileText)) {
            for (String prefix : cleanedPrefixes) {
                if (line.startsWith(prefix)) {
                    String noPrefixLine = line.substring(prefix.length());

                    if (noPrefixLine.isEmpty() ||
                            Character.isWhitespace(noPrefixLine.charAt(0)) ||
                            Character.isWhitespace(prefix.charAt(prefix.length() - 1))) {
                        result.add(noPrefixLine.trim());
                        break;
                    }
                }
            }
        }

        return result;
    }

    public static void assertHasUnknownPrefixes(String fileText, Collection<String> knownPrefixes) {
        Set<String> prefixes = Sets.newHashSet();

        for (String line : fileNonEmptyCommentedLines(fileText)) {
            String prefix = probableDirective(line);
            if (prefix != null) {
                prefixes.add(prefix);
            }
        }

        prefixes.removeAll(cleanDirectivesFromComments(knownPrefixes));

        Assert.assertTrue("File contains some unexpected directives" + prefixes, prefixes.isEmpty());
    }

    private static String probableDirective(String line) {
        String[] arr = line.split(" ", 2);
        String firstWord = arr[0];

        if (firstWord.length() > 1 && StringUtil.toUpperCase(firstWord).equals(firstWord)) {
            return firstWord;
        }

        return null;
    }

    private static List<String> cleanDirectivesFromComments(Collection<String> prefixes) {
        List<String> resultPrefixes = Lists.newArrayList();

        for (String prefix : prefixes) {
            if (prefix.startsWith("//")) {
                resultPrefixes.add(StringUtil.trimLeading(prefix.substring(2)));
            }
            else {
                resultPrefixes.add(prefix);
            }
        }

        return resultPrefixes;
    }


    @NotNull
    private static List<String> fileNonEmptyCommentedLines(String fileText) {
        List<String> result = new ArrayList<String>();

        try {
            BufferedReader reader = new BufferedReader(new StringReader(fileText));
            try {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("//")) {
                        String uncommentedLine = line.substring(2).trim();
                        if (!uncommentedLine.isEmpty()) {
                            result.add(uncommentedLine);
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch(IOException e) {
            throw new AssertionError(e);
        }

        return result;
    }
}
