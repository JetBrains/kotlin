/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.test.util

import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.openapi.util.io.FileUtil
import java.io.File

public fun String.removeTrailingWhitespacesFromEachLine(): String = split('\n') map { it.trimTrailing() } makeString ("\n")

public fun CodeInsightTestFixture.configureWithExtraFile(path: String, extraNamePart: String = ".Data") {
    val noExtensionPath = FileUtil.getNameWithoutExtension(path)
    val extraPath = array("kt", "java").stream()
            .map { ext -> "$noExtensionPath$extraNamePart.$ext" }
            .firstOrNull { extraPath -> File(extraPath).exists() }

    if (extraPath != null) {
        configureByFiles(path, extraPath)
    }
    else {
        configureByFile(path)
    }
}

public fun String.trimIndent(): String {
    val lines = split('\n')

    val firstNonEmpty = lines.firstOrNull { !it.trim().isEmpty() }
    if (firstNonEmpty == null) {
        return this
    }

    val trimmedPrefix = firstNonEmpty.takeWhile { ch -> ch.isWhitespace() }
    if (trimmedPrefix.isEmpty()) {
        return this
    }

    return lines.map { line ->
        if (line.trim().isEmpty()) {
            ""
        }
        else {
            if (!line.startsWith(trimmedPrefix)) {
                throw IllegalArgumentException(
                        """Invalid line "$line", ${trimmedPrefix.size} whitespace character are expected""")
            }

            line.substring(trimmedPrefix.length)
        }
    }.joinToString(separator = "\n")
}
