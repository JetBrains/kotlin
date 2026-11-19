/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.filters

import java.nio.file.Path
import kotlin.io.path.*

/**
 * A test data path in the form `@TestMetadata` uses: relative to the repository root, with `/` separators.
 * As a [TestMetadataFilter] path, a trailing `/` limits it to the directory; without a repository root to resolve the
 * option against (a direct launch, see [unresolvedTestDataPaths]), it is the option entry as given.
 */
@JvmInline
internal value class TestMetadataPath(val path: String) {
    override fun toString(): String = path
}

/**
 * The `testDataPath` option resolved against the repository root.
 *
 * @property paths the paths for [TestMetadataFilter]
 * @property errors one per path that can't be used; the run must not start if there are any
 */
internal class ResolvedTestDataPaths(val paths: List<TestMetadataPath>, val errors: List<TestDataPathError>)

/** A `testDataPath` entry that can't be used, with [path] as given in the option. */
internal sealed interface TestDataPathError {
    val path: String

    /** The message for the console and the report, naming [repositoryRoot] the paths are relative to. */
    fun render(repositoryRoot: Path): String

    data class OutsideRepository(override val path: String) : TestDataPathError {
        override fun render(repositoryRoot: Path): String =
            "testDataPath '$path' is outside the repository root ${repositoryRoot.toAbsolutePath().normalize()}"
    }

    /**
     * @property suggestion the repository-relative form of [path] if it exists relative to the module directory
     */
    data class Missing(override val path: String, val suggestion: TestMetadataPath?) : TestDataPathError {
        override fun render(repositoryRoot: Path): String = buildString {
            append("testDataPath '$path' does not exist; paths are relative to the repository root ")
            append(repositoryRoot.toAbsolutePath().normalize())
            if (suggestion != null) {
                append(". Did you mean '$suggestion'?")
            }
        }
    }
}

/** Splits the comma-separated `testDataPath` option, dropping blank entries. */
internal fun splitTestDataPaths(option: String): List<String> =
    option.split(",").map(String::trim).filter(String::isNotEmpty)

/** The entries of the `testDataPath` [option] as they are, without resolving them like [resolveTestDataPaths]. */
internal fun unresolvedTestDataPaths(option: String): List<TestMetadataPath> =
    splitTestDataPaths(option).map(::TestMetadataPath)

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
    val paths = mutableListOf<TestMetadataPath>()
    val errors = mutableListOf<TestDataPathError>()

    for (rawPath in splitTestDataPaths(option)) {
        val path = Path(rawPath)
        val absolutePath = root.resolve(path).normalize()
        if (!absolutePath.startsWith(root)) {
            errors += TestDataPathError.OutsideRepository(rawPath)
            continue
        }

        if (!absolutePath.existsAsTestDataPath()) {
            val projectRelative = projectDir?.toAbsolutePath()?.resolve(path)?.normalize()
            val suggestion = projectRelative
                ?.takeIf { it.startsWith(root) && it.existsAsTestDataPath() }
                ?.let { TestMetadataPath(root.relativize(it).invariantSeparatorsPathString) }
            errors += TestDataPathError.Missing(rawPath, suggestion)
            continue
        }

        val relativePath = root.relativize(absolutePath).invariantSeparatorsPathString
        paths += TestMetadataPath(if (rawPath.endsWith('/') && relativePath.isNotEmpty()) "$relativePath/" else relativePath)
    }

    return ResolvedTestDataPaths(paths, errors)
}

private fun Path.existsAsTestDataPath(): Boolean {
    if (exists()) return true
    val directory = parent?.takeIf { it.isDirectory() } ?: return false
    // Not a glob: the name may contain glob syntax like `[`
    return directory.listDirectoryEntries().any { it.name.startsWith("$name.") }
}
