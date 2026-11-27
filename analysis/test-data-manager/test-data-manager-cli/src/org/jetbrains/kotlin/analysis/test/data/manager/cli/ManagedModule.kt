/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import org.jetbrains.kotlin.analysis.test.data.manager.filters.TestMetadataPath
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

/** The Gradle task that prints the [ManagedModule]s, one line each after [MANAGED_MODULE_PREFIX]. */
internal const val MANAGED_MODULE_TASK = "testDataManagerModule"

internal const val MANAGED_MODULE_PREFIX = "Test data manager module: "

/**
 * A module with the `test-data-manager` plugin, as Gradle has it.
 *
 * @property project the project path of the module, like `:analysis:stubs`
 * @property directory the directory of the module, relative to the repository root
 * @property testData the test data directories the module declares, relative to the repository root: its tests read no
 * other test data, as `test-inputs-check` ensures
 */
internal class ManagedModule(val project: String, val directory: String, val testData: List<String>) {
    /**
     * Whether the tests of the module may have test data in the `testDataPath` entry [path], a repository-relative path
     * without a trailing `/`, only inside it if it is [directoryOnly]: the entry is in a declared directory or includes one
     * ([TestMetadataPath.includes]). The empty path is the whole repository.
     */
    fun mayHaveTestsOf(path: String, directoryOnly: Boolean): Boolean {
        if (path.isEmpty()) return true
        val entry = TestMetadataPath(if (directoryOnly) "$path/" else path)
        return testData.any { directory ->
            // The repository root, declared as the test data, holds every path
            directory.isEmpty() || path == directory || path.startsWith("$directory/") || entry.includes("$directory/")
        }
    }
}

/**
 * The managed modules printed in [output], the output of [MANAGED_MODULE_TASK], sorted by their project path, with
 * their paths relative to [root]; a declared test data directory outside the repository is left out.
 */
internal fun parseManagedModules(output: String, root: Path): List<ManagedModule> =
    output.lineSequence()
        .map { it.trimEnd('\r') }
        .filter { it.startsWith(MANAGED_MODULE_PREFIX) }
        .map { line ->
            val fields = line.removePrefix(MANAGED_MODULE_PREFIX).split('\t')
            val [project, directory] = fields
            ManagedModule(
                project = project,
                directory = root.relativize(Path(directory)).invariantSeparatorsPathString,
                testData = fields.drop(2)
                    .map(::Path)
                    .filter { it.isInside(root) }
                    .map { root.relativize(it).invariantSeparatorsPathString },
            )
        }
        .sortedBy { it.project }
        .toList()

/**
 * [module] as the project path the module reports use: `analysis:stubs` and `analysis/stubs/` are `:analysis:stubs`.
 *
 * @throws IllegalArgumentException if [module] names no module, like `:`
 */
internal fun projectPath(module: String): String {
    val names = module.trim().replace('\\', '/').replace(':', '/').split('/').filter { it != "" && it != "." }
    require(names.isNotEmpty()) { "'$module' is not a module path" }
    return names.joinToString(":", prefix = ":")
}

internal fun isModulePath(module: String): Boolean = runCatching { projectPath(module) }.isSuccess
