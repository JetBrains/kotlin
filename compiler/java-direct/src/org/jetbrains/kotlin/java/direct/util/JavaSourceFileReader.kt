/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.util

import com.intellij.openapi.vfs.VirtualFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Abstraction over file access used by [org.jetbrains.kotlin.java.direct.JavaClassFinderOverAstImpl] for reading `.java`
 * source files and walking source roots.
 *
 * The input is always a [VirtualFile] — the class finder receives roots as virtual files
 * (via `VfsBasedProjectEnvironment.getFirJavaFacade` → `localFs`) so all I/O goes through the
 * IntelliJ VFS caching layer. Extracting this interface serves two purposes:
 *  1. The class finder no longer performs direct I/O — it delegates to a collaborator that can
 *     be swapped in tests or alternative environments (e.g. a fake in-memory VFS).
 *  2. "Not a regular file / directory / invalid" is explicitly distinguished from a real read
 *     at the API boundary so callers can reason about missing vs. broken inputs.
 *
 * I/O errors (`IOException` from the VFS) are **not** caught here. They propagate to the
 * compiler's top-level error handling, consistent with how the rest of the compiler handles
 * filesystem failures.
 */
interface JavaSourceFileReader {
    /**
     * Reads the full textual content of a Java source file.
     * Returns `null` if [file] is not a readable regular file (invalid or a directory).
     */
    fun readFileContent(file: VirtualFile): CharSequence?

    /**
     * Returns a lazy [Sequence] of `.java` (and `package-info.java`) source files found under
     * the given [roots]. Invalid or non-directory roots are silently skipped.
     *
     * The returned sequence is intended to be consumed once by [org.jetbrains.kotlin.java.direct.JavaClassFinderOverAstImpl.buildIndex].
     */
    fun walkSourceRoots(roots: List<VirtualFile>): Sequence<VirtualFile>

    /**
     * Opens a line-by-line reader over the file for the lightweight (no-parse) pre-scan.
     * Returns `null` if [file] is not a readable regular file. The caller must close the
     * returned [BufferedReader].
     */
    fun openLineReader(file: VirtualFile): BufferedReader?
}

/**
 * Default VFS-backed implementation of [JavaSourceFileReader].
 */
object DefaultJavaSourceFileReader : JavaSourceFileReader {
    override fun readFileContent(file: VirtualFile): CharSequence? {
        if (!file.isValid || file.isDirectory) return null
        // `VirtualFile.charset` requires an IDE `Application` (EncodingManager); the
        // java-direct module also runs in unit-test contexts without one. `.java` sources
        // are specified by JLS to be decoded with whatever charset the compiler chooses —
        // we follow javac's convention of UTF-8, matching the legacy lightweight scanner.
        return String(file.contentsToByteArray(), StandardCharsets.UTF_8)
    }

    override fun openLineReader(file: VirtualFile): BufferedReader? {
        if (!file.isValid || file.isDirectory) return null
        return BufferedReader(InputStreamReader(file.inputStream, StandardCharsets.UTF_8))
    }

    override fun walkSourceRoots(roots: List<VirtualFile>): Sequence<VirtualFile> = sequence {
        for (root in roots) {
            if (!root.isValid) continue
            walk(root)
        }
    }

    private suspend fun SequenceScope<VirtualFile>.walk(file: VirtualFile) {
        if (!file.isValid) return
        if (file.isDirectory) {
            // `children` on CoreLocalVirtualFile lazily delegates to File.listFiles() and caches
            // the result, so repeat traversals benefit from VFS caching.
            val kids = file.children ?: return
            for (child in kids) {
                walk(child)
            }
        } else if (file.name.endsWith(".java")) {
            yield(file)
        }
    }
}
