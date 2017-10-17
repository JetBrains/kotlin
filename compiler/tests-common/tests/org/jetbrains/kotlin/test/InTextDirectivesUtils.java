/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public final class InTextDirectivesUtils {

    private static final String DIRECTIVES_FILE_NAME = "directives.txt";

    private InTextDirectivesUtils() {
    }

    @Nullable
    public static Integer getPrefixedInt(String fileText, String prefix) {
        String[] strings = findArrayWithPrefixes(fileText, prefix);
        if (strings.length > 0) {
            assert strings.length == 1;
            return Integer.parseInt(strings[0]);
        }

        return null;
    }

    @Nullable
    public static Boolean getPrefixedBoolean(String fileText, String prefix) {
        String[] strings = findArrayWithPrefixes(fileText, prefix);
        if (strings.length > 0) {
            assert strings.length == 1;
            return Boolean.parseBoolean(strings[0]);
        }

        return null;
    }

    @NotNull
    public static String[] findArrayWithPrefixes(@NotNull String fileText, @NotNull String... prefixes) {
        return ArrayUtil.toStringArray(findListWithPrefixes(fileText, prefixes));
    }

    @NotNull
    public static List<String> findListWithPrefixes(@NotNull String fileText, @NotNull String... prefixes) {
        List<String> result = new ArrayList<>();

        for (String line : findLinesWithPrefixesRemoved(fileText, prefixes)) {
            String unquoted = StringUtil.unquoteString(line);
            if (!unquoted.equals(line)) {
                result.add(unquoted);
            }
            else{
                String[] variants = line.split(",");
                for (String variant : variants) {
                    result.add(variant.trim());
                }
            }
        }

        return result;
    }

    public static boolean isDirectiveDefined(String fileText, String directive) {
        return !findListWithPrefixes(fileText, directive).isEmpty();
    }

    @Nullable
    public static String findStringWithPrefixes(String fileText, String... prefixes) {
        List<String> strings = findListWithPrefixes(fileText, prefixes);
        if (strings.isEmpty()) {
            return null;
        }

        if (strings.size() != 1) {
            throw new IllegalStateException("There is more than one string with given prefixes " +
                                            Arrays.toString(prefixes) + ":\n" +
                                            StringUtil.join(strings, "\n") + "\n" +
                                            "Use findListWithPrefixes() instead.");
        }

        return strings.get(0);
    }

    @NotNull
    public static List<String> findLinesWithPrefixesRemoved(String fileText, String... prefixes) {
        return findLinesWithPrefixesRemoved(fileText, true, prefixes);
    }

    @NotNull
    public static List<String> findLinesWithPrefixesRemoved(String fileText, boolean trim, String... prefixes) {
        List<String> result = new ArrayList<>();
        List<String> cleanedPrefixes = cleanDirectivesFromComments(Arrays.asList(prefixes));

        for (String line : fileNonEmptyCommentedLines(fileText)) {
            for (String prefix : cleanedPrefixes) {
                if (line.startsWith(prefix)) {
                    String noPrefixLine = line.substring(prefix.length());

                    if (noPrefixLine.isEmpty() ||
                            Character.isWhitespace(noPrefixLine.charAt(0)) ||
                            Character.isWhitespace(prefix.charAt(prefix.length() - 1))) {
                        result.add(trim ? noPrefixLine.trim() : StringUtil.trimTrailing(StringsKt.drop(noPrefixLine, 1)));
                        break;
                    } else {
                        throw new AssertionError(
                                "Line starts with prefix \"" + prefix + "\", but doesn't have space symbol after it: " + line);
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
            if (prefix.startsWith("//") || prefix.startsWith("##")) {
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
        List<String> result = new ArrayList<>();

        try {
            try (BufferedReader reader = new BufferedReader(new StringReader(fileText))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("//") || line.startsWith("##")) {
                        String uncommentedLine = line.substring(2).trim();
                        if (!uncommentedLine.isEmpty()) {
                            result.add(uncommentedLine);
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }

        return result;
    }

    private static String textWithDirectives(File file) {
        try {
            String fileText;
            if (file.isDirectory()) {
                File directivesFile = new File(file, DIRECTIVES_FILE_NAME);
                if (!directivesFile.exists()) return "";

                fileText = FileUtil.loadFile(directivesFile);
            }
            else {
                fileText = FileUtil.loadFile(file);
            }
            return fileText;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isCompatibleTarget(TargetBackend targetBackend, File file) {
        if (targetBackend == TargetBackend.ANY) return true;

        List<String> backends = findLinesWithPrefixesRemoved(textWithDirectives(file), "// TARGET_BACKEND: ");
        return backends.isEmpty() || backends.contains(targetBackend.name());
    }

    private static boolean isIgnoredTargetByPrefix(TargetBackend targetBackend, File file, String prefix) {
        if (targetBackend == TargetBackend.ANY) return false;

        List<String> ignoredBackends = findListWithPrefixes(textWithDirectives(file), prefix);
        return ignoredBackends.contains(targetBackend.name());
    }

    public static boolean isIgnoredTarget(TargetBackend targetBackend, File file) {
        return isIgnoredTargetByPrefix(targetBackend, file, "// IGNORE_BACKEND: ");
    }

    public static boolean isIgnoredTargetWithoutCheck(TargetBackend targetBackend, File file) {
        return isIgnoredTargetByPrefix(targetBackend, file, "// IGNORE_BACKEND_WITHOUT_CHECK: ");
    }

    // Whether the target test is supposed to pass successfully on targetBackend
    public static boolean isPassingTarget(TargetBackend targetBackend, File file) {
        return isCompatibleTarget(targetBackend, file) && !isIgnoredTarget(targetBackend, file) && !isIgnoredTargetWithoutCheck(targetBackend, file);
    }
}
