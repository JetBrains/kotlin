/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.testFramework;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.types.AbstractTypeChecker;
import org.jetbrains.kotlin.types.FlexibleTypeImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInfo;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.FileInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public final class KtUsefulTestCase {

    static {
        // -- KOTLIN ADDITIONAL START --

        FlexibleTypeImpl.RUN_SLOW_ASSERTIONS = true;
        AbstractTypeChecker.RUN_SLOW_ASSERTIONS = true;

        // -- KOTLIN ADDITIONAL END --
    }

    private KtUsefulTestCase() {}


    /**
     * Checks {@code actual} contains same elements (in {@link #equals(Object)} meaning) as {@code expected} irrespective of their order
     */
    @SafeVarargs
    public static <T> void assertSameElements(@NotNull Collection<? extends T> actual, @NotNull T... expected) {
        assertSameElements("", actual, Arrays.asList(expected));
    }

    /**
     * Checks {@code actual} contains same elements (in {@link #equals(Object)} meaning) as {@code expected} irrespective of their order
     */
    public static <T> void assertSameElements(@NotNull String message, @NotNull Collection<? extends T> actual, @NotNull Collection<? extends T> expected) {
        if (actual.size() != expected.size() || !new HashSet<>(expected).equals(new HashSet<T>(actual))) {
            Assertions.assertEquals(new HashSet<>(expected), new HashSet<T>(actual), message);
        }
    }

    public static void assertExists(@NotNull File file){
        Assertions.assertTrue(file.exists(), "File should exist " + file);
    }

    @NotNull
    public static String getTestName(@Nullable String name, boolean lowercaseFirstLetter) {
        if (name == null) return "";
        name = StringUtil.trimStart(name, "test");
        return StringUtil.isEmpty(name) ? "" : lowercaseFirstLetter(name, lowercaseFirstLetter);
    }

    @NotNull
    public static String getTestName(@Nullable TestInfo testInfo) {
        return getTestName(testInfo.getTestMethod().get().getName(), true);
    }

    public static @NotNull String lowercaseFirstLetter(@NotNull String name, boolean lowercaseFirstLetter) {
        if (lowercaseFirstLetter && !isAllUppercaseName(name)) {
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    private static boolean isAllUppercaseName(@NotNull String name) {
        int uppercaseChars = 0;
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLowerCase(name.charAt(i))) {
                return false;
            }
            if (Character.isUpperCase(name.charAt(i))) {
                uppercaseChars++;
            }
        }
        return uppercaseChars >= 3;
    }

    public static void assertSameLinesWithFile(@NotNull String filePath, @NotNull String actualText) {
        assertSameLinesWithFile(filePath, actualText, true);
    }

    public static void assertSameLinesWithFile(@NotNull String filePath, @NotNull String actualText, boolean trimBeforeComparing) {
        String fileText;
        try {
            fileText = FileUtil.loadFile(new File(filePath), StandardCharsets.UTF_8);
        }
        catch (FileNotFoundException e) {
            try {
                FileUtil.writeToFile(new File(filePath), actualText);
            }
            catch (IOException exception) {
                throw new AssertionError(exception);
            }
            throw new AssertionFailedError("No output text found. File " + filePath + " created.");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        String expected = StringUtil.convertLineSeparators(trimBeforeComparing ? fileText.trim() : fileText);
        String actual = StringUtil.convertLineSeparators(trimBeforeComparing ? actualText.trim() : actualText);
        if (!Objects.equals(expected, actual)) {
            throw new AssertionFailedError(
                    null,
                    new FileInfo(filePath, expected.getBytes(StandardCharsets.UTF_8)),
                    actual
            );
        }
    }
}
