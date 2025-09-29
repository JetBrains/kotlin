/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.util.StringUtilsKt;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.FileInfo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import static org.jetbrains.kotlin.test.AssertionsKt.isTeamCityBuild;
import static org.jetbrains.kotlin.test.KtAssert.fail;

public class TestDataAssertions {
    public static final String ACTUAL_DATA_DIFFERS_FROM_FILE_CONTENT = "Actual data differs from file content";

    public static void assertEqualsToFile(@NotNull Path expectedFile, @NotNull String actual) {
        assertEqualsToFile(expectedFile.toFile(), actual);
    }

    public static void assertEqualsToFile(@NotNull File expectedFile, @NotNull String actual) {
        assertEqualsToFile(expectedFile, actual, s -> s);
    }

    public static void assertEqualsToFile(@NotNull String message, @NotNull File expectedFile, @NotNull String actual) {
        assertEqualsToFile(message, expectedFile, actual, s -> s);
    }

    public static void assertEqualsToFile(@NotNull File expectedFile, @NotNull String actual, @NotNull Function1<String, String> sanitizer) {
        assertEqualsToFile(ACTUAL_DATA_DIFFERS_FROM_FILE_CONTENT, expectedFile, actual, sanitizer);
    }

    public static void assertValueAgnosticEqualsToFile(File expectedFile, @NotNull String actual) {
        ValueAgnosticSanitizer sanitizer = new ValueAgnosticSanitizer(actual);

        String expectedText = tryLoadExpectedFile(expectedFile, sanitizer::generateExpectedText);
        String expectedSanitizedText = applyDefaultAndCustomSanitizer(expectedText, s -> s);

        String sanitizedActualBasedOnExpectPlaceholders =
                applyDefaultAndCustomSanitizer(
                        sanitizer.generateSanitizedActualTextBasedOnExpectPlaceholders(expectedSanitizedText), s -> s);

        FileComparisonResult comparisonResult = new FileComparisonResult(
                expectedFile,
                expectedText,
                expectedSanitizedText,
                sanitizedActualBasedOnExpectPlaceholders
        );

        failIfNotEqual(ACTUAL_DATA_DIFFERS_FROM_FILE_CONTENT, comparisonResult);
    }

    public static FileComparisonResult compareExpectFileWithActualText(@NotNull File expectedFile, @NotNull String actual, @NotNull Function1<String, String> sanitizer) {
        Function0<String> getActualSanitizedText = () -> applyDefaultAndCustomSanitizer(actual, sanitizer);

        String expectedText = tryLoadExpectedFile(expectedFile, getActualSanitizedText);
        String expectedSanitizedText = applyDefaultAndCustomSanitizer(expectedText, sanitizer);

        return new FileComparisonResult(expectedFile, expectedText, expectedSanitizedText, getActualSanitizedText.invoke());
    }

    public static String tryLoadExpectedFile(@NotNull File expectedFile, @NotNull Function0<String> getSanitizedActualText) {
        try {
            if (!expectedFile.exists()) {
                if (isTeamCityBuild()) {
                    fail("Expected data file " + expectedFile + " did not exist");
                } else {
                    FileUtil.writeToFile(expectedFile, getSanitizedActualText.invoke());
                    fail("Expected data file did not exist. Generating: " + expectedFile);
                }
            }
            return FileUtil.loadFile(expectedFile, CharsetToolkit.UTF8, true);
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    public static class FileComparisonResult {
        public final @NotNull File expectedFile;
        public final @NotNull String expectedText;
        public final @NotNull String expectedSanitizedText;
        public final @NotNull String actualSanitizedText;
        public final boolean doesEqual;

        public FileComparisonResult(
                @NotNull File expectedFile,
                @NotNull String expectedText,
                @NotNull String expectedSanitizedText,
                @NotNull String actualSanitizedText
        ) {
            this.expectedFile = expectedFile;
            this.expectedText = expectedText;
            this.expectedSanitizedText = expectedSanitizedText;
            this.actualSanitizedText = actualSanitizedText;
            this.doesEqual = Objects.equals(expectedSanitizedText, actualSanitizedText);
        }
    }

    public static String applyDefaultAndCustomSanitizer(String text, @NotNull Function1<String, String> sanitizer) {
        String textAfterDefaultSanitizer = StringUtilsKt.trimTrailingWhitespacesAndAddNewlineAtEOF(StringUtil.convertLineSeparators(text.trim()));
        return sanitizer.invoke(textAfterDefaultSanitizer);
    }

    public static void assertEqualsToFile(@NotNull String message, @NotNull File expectedFile, @NotNull String actual, @NotNull Function1<String, String> sanitizer) {
        failIfNotEqual(message, compareExpectFileWithActualText(expectedFile, actual, sanitizer));
    }

    public static void failIfNotEqual(@NotNull String message, FileComparisonResult fileComparisonResult) {
        if (!fileComparisonResult.doesEqual) {
            throw new AssertionFailedError(
                    message + ": " + fileComparisonResult.expectedFile.getName(),
                    new FileInfo(fileComparisonResult.expectedFile.getAbsolutePath(), fileComparisonResult.expectedText.getBytes(
                            StandardCharsets.UTF_8)),
                    fileComparisonResult.actualSanitizedText
            );
        }
    }
}
