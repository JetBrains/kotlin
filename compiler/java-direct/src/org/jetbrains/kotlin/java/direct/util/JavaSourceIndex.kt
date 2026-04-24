/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.util

import com.intellij.openapi.vfs.VirtualFile
import kotlin.text.iterator

/**
 * Lightweight (no-parse) source index helpers for Java source files.
 *
 * These utilities allow extracting the package name and top-level class names of a `.java`
 * file by scanning it line-by-line, without invoking the KMP Java parser. They are used by
 * [org.jetbrains.kotlin.java.direct.JavaClassFinderOverAstImpl] to index large files cheaply; the full parse is then deferred
 * until a class is actually looked up.
 */

internal val PACKAGE_REGEX = Regex("""\bpackage\s+([\w.]+)\s*;""")
internal val DECLARATION_REGEX = Regex("""\b(class|interface|enum|record)\s+([A-Za-z_]\w*)""")

/**
 * Result of lightweight (no-parse) file scanning.
 */
internal data class LightweightFileInfo(
    val packageName: String?,
    val topLevelClassNames: Set<String>,
)

/**
 * Strips single-line (`//`) and block (`/* */`) comments from a line.
 * Tracks block comment state across lines.
 *
 * @return pair of (effective text with comments removed, whether still inside a block comment)
 */
private fun stripLineComments(line: String, inBlockComment: Boolean): Pair<String, Boolean> {
    val sb = StringBuilder()
    var inComment = inBlockComment
    var i = 0
    while (i < line.length) {
        if (inComment) {
            val endIdx = line.indexOf("*/", i)
            if (endIdx >= 0) {
                inComment = false
                i = endIdx + 2
            } else {
                return sb.toString() to true
            }
        } else {
            if (i + 1 < line.length) {
                if (line[i] == '/' && line[i + 1] == '/') {
                    return sb.toString() to false
                }
                if (line[i] == '/' && line[i + 1] == '*') {
                    inComment = true
                    i += 2
                    continue
                }
            }
            sb.append(line[i])
            i++
        }
    }
    return sb.toString() to inComment
}

/**
 * Extracts package name and top-level class/interface/enum/record names from a Java file
 * without invoking the parser. Scans the file line by line, stripping comments and tracking
 * brace depth to distinguish top-level declarations from nested ones.
 *
 * This is much cheaper than full parsing and is used for indexing large files.
 */
internal fun extractFileInfoLightweight(file: VirtualFile, reader: JavaSourceFileReader): LightweightFileInfo? {
    var packageName: String? = null
    val classNames = mutableSetOf<String>()
    var inBlockComment = false
    var braceDepth = 0

    val lineReader = reader.openLineReader(file) ?: return null
    lineReader.use { br ->
        var rawLine = br.readLine()
        while (rawLine != null) {
            val (effective, stillInComment) = stripLineComments(rawLine, inBlockComment)
            inBlockComment = stillInComment

            if (effective.isNotBlank()) {
                val depthBeforeLine = braceDepth
                for (ch in effective) {
                    when (ch) {
                        '{' -> braceDepth++
                        '}' -> braceDepth--
                    }
                }

                if (packageName == null && depthBeforeLine == 0) {
                    PACKAGE_REGEX.find(effective)?.let {
                        packageName = it.groupValues[1]
                    }
                }

                if (depthBeforeLine == 0) {
                    for (match in DECLARATION_REGEX.findAll(effective)) {
                        classNames.add(match.groupValues[2])
                    }
                }
            }

            rawLine = br.readLine()
        }
    }

    if (classNames.isEmpty()) return null
    return LightweightFileInfo(packageName, classNames)
}
