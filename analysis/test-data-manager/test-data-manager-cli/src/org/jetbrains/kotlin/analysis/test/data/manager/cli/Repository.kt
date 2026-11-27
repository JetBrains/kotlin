/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import org.jetbrains.kotlin.analysis.test.data.manager.filters.TestDataPathError
import org.jetbrains.kotlin.analysis.test.data.manager.filters.resolveTestDataPaths
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString

/**
 * The repository the wrapper runs in, at [root], a real path, with its [modules] with managed test data, sorted by their
 * project path.
 */
internal class Repository(val root: Path, val modules: List<ManagedModule>) {
    private val modulesByProject: Map<String, ManagedModule> = modules.associateBy { it.project }
    private val modulesByDirectory: Map<String, ManagedModule> = modules.associateBy { it.directory }

    /**
     * The project path of [module] given as `--module`: a project path (`:analysis:stubs`) as is, a directory
     * (`analysis/stubs/`, `plugins/allopen/../lombok`, an absolute one, also through a symbolic link) as the managed
     * module in it, which may have another project path, or as the default project path of the directory.
     */
    fun requestedProject(module: String): String {
        if (!module.startsWith(":")) {
            // `analysis:stubs` is no path on Windows
            val resolved = try {
                root.resolve(module.replace('\\', '/')).normalize()
            } catch (_: InvalidPathException) {
                null
            }

            val directory = resolved?.let { followLinkIntoRepository(module, it) }
            if (directory != null && directory.isInside(root) && directory != root) {
                val relative = root.relativize(directory).invariantSeparatorsPathString
                return modulesByDirectory[relative]?.project ?: projectPath(relative)
            }
        }

        return projectPath(module)
    }

    /**
     * The repository-relative build file of [module], a project path: in the directory Gradle gave, or in the default one of
     * an unknown module.
     */
    fun buildFile(module: String): String {
        val directory = modulesByProject[module]?.directory ?: module.removePrefix(":").replace(':', '/')
        return if (directory.isEmpty()) "build.gradle.kts" else "$directory/build.gradle.kts"
    }

    /**
     * The `testDataPath` entry [given] on the command line, in the repository. For an entry that exists relative to one
     * of [suggestionBases] instead, the error suggests its repository-relative form, as the runner does for its module.
     * An absolute path into the repository through a symbolic link counts as inside it.
     */
    fun testDataEntry(given: String, suggestionBases: List<Path>): TestDataEntry {
        val resolvedPath = try {
            root.resolve(given).normalize()
        } catch (_: InvalidPathException) {
            // Like `*` on Windows: no file can have such a path
            return TestDataEntry.Invalid(given, TestDataPathError.Missing(given, emptyList()).render(root))
        }

        val path = followLinkIntoRepository(given, resolvedPath)
            .let { if (it.isInside(root) && it.isAbsolute && Path(given).isAbsolute) it.toString() else given }
        val resolved = resolveTestDataPaths(path, root, suggestionBases)
        resolved.errors.firstOrNull()?.let { error ->
            // The error names the entry as given, not the real path of a link
            return TestDataEntry.Invalid(given, error.render(root).replaceFirst("'$path'", "'$given'"))
        }

        val relativePath = resolved.paths.single().path.trimEnd('/')
        val directoryOnly = given.endsWith('/')
        return TestDataEntry.Resolved(
            forRunner = when {
                // The repository root has no relative form
                relativePath.isEmpty() -> given
                directoryOnly -> "$relativePath/"
                else -> relativePath
            },
            path = relativePath,
            directoryOnly = directoryOnly,
        )
    }

    /**
     * [resolved], the path [given] resolves to in the repository; for a [given] absolute path outside the repository that
     * leads into it through a symbolic link, the real path. The path itself may not exist, like a test data file named
     * without its extension: the links are resolved in its nearest existing ancestor.
     */
    private fun followLinkIntoRepository(given: String, resolved: Path): Path {
        if (resolved.isInside(root) || !Path(given).isAbsolute) return resolved
        // Not normalized: `..` after a link leaves the target of the link
        val path = Path(given)
        val ancestor = generateSequence(path) { it.parent }.firstOrNull { it.exists() } ?: return resolved
        val real = try {
            ancestor.toRealPath().resolve(ancestor.relativize(path).invariantSeparatorsPathString).normalize()
        } catch (_: IOException) {
            return resolved
        }

        return if (real.isInside(root)) real else resolved
    }
}

/** A `testDataPath` entry of the command line, in the repository. */
internal sealed interface TestDataEntry {
    /**
     * The entry as the runner gets it: repository-relative when it resolves, so that one through a symbolic link to the
     * checkout works there too, with its trailing `/` kept; as given otherwise, for the runner to report.
     */
    val forRunner: String

    /**
     * An entry that exists.
     *
     * @property path the entry relative to the repository root, as `@TestMetadata` paths are, without a trailing `/`
     * @property directoryOnly whether a trailing `/` limits the entry to the directory
     */
    class Resolved(override val forRunner: String, val path: String, val directoryOnly: Boolean) : TestDataEntry

    /** An entry that can't be used, with the [error] the runner would report for it. */
    class Invalid(override val forRunner: String, val error: String) : TestDataEntry
}

internal fun Path.isInside(root: Path): Boolean = this == root || startsWith(root)
