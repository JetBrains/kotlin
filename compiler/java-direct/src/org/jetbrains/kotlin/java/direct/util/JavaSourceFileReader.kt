/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.util

import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Abstraction over file access for reading `.java` source files and walking source roots.
 * I/O errors (`IOException`) are **not** caught here — they propagate to the compiler's
 * top-level error handling, like the rest of the compiler's filesystem failures.
 */
interface JavaSourceFileReader {
    /**
     * Reads the full textual content of a Java source file.
     * Returns `null` if [file] is not a readable regular file (missing or a directory).
     */
    fun readFileContent(file: File): CharSequence?

    /**
     * Returns a lazy [Sequence] of `.java` (and `package-info.java`) source files found under
     * the given [roots]. Missing or non-directory roots are silently skipped.
     *
     * The returned sequence is intended to be consumed once by [org.jetbrains.kotlin.java.direct.JavaClassFinderOverAstImpl.buildIndex].
     */
    fun walkSourceRoots(roots: List<File>): Sequence<File>
}

/**
 * Default `java.io.File`-backed implementation of [JavaSourceFileReader].
 */
object DefaultJavaSourceFileReader : JavaSourceFileReader {
    override fun readFileContent(file: File): CharSequence? {
        if (!file.isFile) return null
        // `.java` sources are specified by JLS to be decoded with whatever charset the compiler
        // chooses — we follow javac's convention of UTF-8.
        return String(file.readBytes(), StandardCharsets.UTF_8)
    }

    override fun walkSourceRoots(roots: List<File>): Sequence<File> = sequence {
        for (root in roots) {
            if (!root.exists()) continue
            walk(root)
        }
    }

    private suspend fun SequenceScope<File>.walk(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            val kids = file.listFiles() ?: return
            for (child in kids) {
                walk(child)
            }
        } else if (file.name.endsWith(".java")) {
            yield(file)
        }
    }
}
