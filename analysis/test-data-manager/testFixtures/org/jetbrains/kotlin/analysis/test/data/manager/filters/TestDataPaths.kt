/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.filters

import java.nio.file.Path
import kotlin.io.path.*

/**
 * The `testDataPath` option resolved against the repository root.
 *
 * @property paths repository-relative paths in the form `@TestMetadata` uses, for [TestMetadataFilter]
 * @property errors one message per path that doesn't exist; the run must not start if there are any
 */
internal class ResolvedTestDataPaths(val paths: List<String>, val errors: List<String>)

/** Splits the comma-separated `testDataPath` option, dropping blank entries. */
internal fun splitTestDataPaths(option: String): List<String> =
    option.split(",").map(String::trim).filter(String::isNotEmpty)

/**
 * Resolves the comma-separated `testDataPath` [option] against [repositoryRoot], the directory `@TestMetadata` paths
 * are relative to.
 *
 * An absolute path inside the repository is made relative to it. A path that doesn't exist, as a directory, a file,
 * or a file named without its extension (`dir/file` for `dir/file.kt`), is an error: such a filter silently matches
 * nothing. When the path exists relative to [projectDir] instead, the error suggests the repository-relative form.
 *
 * A trailing `/` is kept: it limits the filter to the directory, without the files named like it (`dir.kt`).
 */
internal fun resolveTestDataPaths(option: String, repositoryRoot: Path, projectDir: Path?): ResolvedTestDataPaths {
    val root = repositoryRoot.toAbsolutePath().normalize()
    val paths = mutableListOf<String>()
    val errors = mutableListOf<String>()

    for (rawPath in splitTestDataPaths(option)) {
        val path = Path(rawPath)
        val absolutePath = root.resolve(path).normalize()
        if (!absolutePath.startsWith(root)) {
            errors += "testDataPath '$rawPath' is outside the repository root $root"
            continue
        }

        if (!absolutePath.existsAsTestDataPath()) {
            errors += buildString {
                append("testDataPath '$rawPath' does not exist; paths are relative to the repository root $root")
                val projectRelative = projectDir?.toAbsolutePath()?.resolve(path)?.normalize()
                if (projectRelative != null && projectRelative.startsWith(root) && projectRelative.existsAsTestDataPath()) {
                    append(". Did you mean '${root.relativize(projectRelative).invariantSeparatorsPathString}'?")
                }
            }
            continue
        }

        val relativePath = root.relativize(absolutePath).invariantSeparatorsPathString
        paths += if (rawPath.endsWith('/') && relativePath.isNotEmpty()) "$relativePath/" else relativePath
    }

    return ResolvedTestDataPaths(paths, errors)
}

private fun Path.existsAsTestDataPath(): Boolean {
    if (exists()) return true
    val directory = parent?.takeIf { it.isDirectory() } ?: return false
    // Not a glob: the name may contain glob syntax like `[`
    return directory.listDirectoryEntries().any { it.name.startsWith("$name.") }
}
