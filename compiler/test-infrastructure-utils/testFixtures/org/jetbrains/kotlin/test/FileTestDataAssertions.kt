/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.utils.rethrow
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path

const val ACTUAL_DATA_DIFFERS_FROM_FILE_CONTENT = "Actual data differs from file content"

class FileComparisonResult(
    val expectedFile: File,
    val expectedText: String,
    expectedSanitizedText: String,
    val actualSanitizedText: String,
) {
    val doesEqual: Boolean = expectedSanitizedText == actualSanitizedText
}

private fun applyDefaultAndCustomSanitizer(text: String, sanitizer: (String) -> String): String {
    val textAfterDefaultSanitizer = StringUtil.convertLineSeparators(text.trim()).trimTrailingWhitespacesAndAddNewlineAtEOF();
    return sanitizer(textAfterDefaultSanitizer);
}

private fun tryLoadExpectedFile(expectedFile: File, getSanitizedActualText: () -> String): String {
    try {
        if (!expectedFile.exists()) {
            if (IS_UNDER_TEAMCITY) {
                KtAssert.fail("Expected data file $expectedFile did not exist");
            } else {
                FileUtil.writeToFile(expectedFile, getSanitizedActualText.invoke());
                KtAssert.fail("Expected data file did not exist. Generating: $expectedFile");
            }
        }
        return FileUtil.loadFile(expectedFile, CharsetToolkit.UTF8, true);
    } catch (e: IOException) {
        throw rethrow(e);
    }
}

fun compareExpectFileWithActualText(
    expectedFile: File,
    actual: String,
    sanitizer: (String) -> String,
): FileComparisonResult {
    val getActualSanitizedText = { applyDefaultAndCustomSanitizer(actual, sanitizer) }

    val expectedText: String = tryLoadExpectedFile(expectedFile, getActualSanitizedText)
    val expectedSanitizedText: String = applyDefaultAndCustomSanitizer(expectedText, sanitizer)

    return FileComparisonResult(
        expectedFile,
        expectedText,
        expectedSanitizedText,
        getActualSanitizedText()
    )
}

private fun failIfNotEqual(message: String, fileComparisonResult: FileComparisonResult) {
    if (!fileComparisonResult.doesEqual) {
        throw AssertionFailedError(
            message + ": " + fileComparisonResult.expectedFile.getName(),
            FileInfo(
                fileComparisonResult.expectedFile.getAbsolutePath(),
                fileComparisonResult.expectedText.toByteArray(StandardCharsets.UTF_8)
            ),
            fileComparisonResult.actualSanitizedText
        )
    }
}

/**
 * Compares the content of the specified file with the string on which the method is invoked.
 * The content can optionally be sanitized before comparison.
 * Provides an comparison assertion failure handled nicely by IDEA.
 *
 * @param expectedFile the path to the file containing the expected content
 * @param sanitizer a lambda function to process the string content before comparison
 * @param message the message displayed if the content does not match the file
 */
@JvmOverloads
fun String.assertEqualsToFile(
    expectedFile: Path,
    sanitizer: (String) -> String = { it },
    message: String = ACTUAL_DATA_DIFFERS_FROM_FILE_CONTENT,
) = assertEqualsToFile(expectedFile.toFile(), sanitizer, message)

/**
 * Compares the content of the specified file with the string on which the method is invoked.
 * The content can optionally be sanitized before comparison.
 * Provides a comparison assertion failure handled nicely by IDEA.
 *
 * @param expectedFile the path to the file containing the expected content
 * @param sanitizer a lambda function to process the actual string before comparison
 * @param message the message displayed if the content does not match the file
 */
@JvmOverloads
fun String.assertEqualsToFile(
    expectedFile: File,
    sanitizer: (String) -> String = { it },
    message: String = ACTUAL_DATA_DIFFERS_FROM_FILE_CONTENT,
) {
    failIfNotEqual(message, compareExpectFileWithActualText(expectedFile, this, sanitizer));
}

/**
 * Asserts that the content of the string is value-agnostically equal to the content of the specified file.
 * The comparison is performed after sanitizing the string and expected file content, ensuring that placeholder
 * values or insignificant content differences are ignored.
 * Provides a comparison assertion failure handled nicely by IDEA.
 *
 * @param expectedFile the file containing the expected content to compare against
 */
fun String.assertValueAgnosticEqualsToFile(expectedFile: File) {
    val sanitizer = ValueAgnosticSanitizer(this);

    val expectedText = tryLoadExpectedFile(expectedFile, sanitizer::generateExpectedText);
    val expectedSanitizedText = applyDefaultAndCustomSanitizer(expectedText) { it }

    val sanitizedActualBasedOnExpectPlaceholders =
        applyDefaultAndCustomSanitizer(
            sanitizer.generateSanitizedActualTextBasedOnExpectPlaceholders(expectedSanitizedText)
        ) { it }

    val comparisonResult = FileComparisonResult(
        expectedFile,
        expectedText,
        expectedSanitizedText,
        sanitizedActualBasedOnExpectPlaceholders
    )

    failIfNotEqual(ACTUAL_DATA_DIFFERS_FROM_FILE_CONTENT, comparisonResult);
}