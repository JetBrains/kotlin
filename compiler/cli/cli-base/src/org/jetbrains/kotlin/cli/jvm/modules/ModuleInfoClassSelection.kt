/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.modules

import java.util.jar.Attributes

/**
 * The rule by which the compiler picks the `module-info.class` of a binary root (a jar or a directory on the classpath/module path):
 *   - the descriptor in the root of the entry, if present;
 *   - otherwise, for a jar whose manifest declares `Multi-Release: true`, the descriptor in the lowest `META-INF/versions/<n>/` directory
 *     with `n >= `[MIN_VERSIONED_RELEASE];
 *   - otherwise none (the entry is then treated as an automatic module).
 *
 * The rule is independent of the file representation so that it can be shared by the compiler (`ClasspathRootsResolver`, working on
 * `VirtualFile`s) and incremental compilation's classpath snapshotting (working on zip entries and `java.io.File`s): both must agree on
 * which descriptor is in effect for a given entry.
 */
object ModuleInfoClassSelection {
    const val MODULE_INFO_CLASS: String = "module-info.class"
    const val MULTI_RELEASE_ATTRIBUTE: String = "Multi-Release"
    const val MIN_VERSIONED_RELEASE: Int = 9

    fun isMultiReleaseJar(manifestAttributes: Attributes?): Boolean =
        manifestAttributes?.getValue(MULTI_RELEASE_ATTRIBUTE)?.equals("true", ignoreCase = true) == true

    /**
     * Selects the descriptor of a binary root.
     *
     * @param rootModuleInfo the `module-info.class` located directly in the root, or `null` if there is none.
     * @param isJar whether the root is a jar file (versioned descriptors are only considered for jars).
     * @param isMultiRelease whether the jar manifest declares `Multi-Release: true` (only queried when needed).
     * @param versionedModuleInfos the `module-info.class` files located in `META-INF/versions/<n>/`, keyed by the name of the version
     * directory (`<n>`); only queried when needed. Directories whose name is not a number `>= `[MIN_VERSIONED_RELEASE] are ignored.
     */
    fun <T : Any> select(
        rootModuleInfo: T?,
        isJar: Boolean,
        isMultiRelease: () -> Boolean,
        versionedModuleInfos: () -> Map<String, T>,
    ): T? {
        if (rootModuleInfo != null) return rootModuleInfo
        if (!isJar || !isMultiRelease()) return null
        return versionedModuleInfos().entries
            .mapNotNull { entry ->
                val version = entry.key.toIntOrNull()?.takeIf { it >= MIN_VERSIONED_RELEASE } ?: return@mapNotNull null
                version to entry.value
            }
            .minByOrNull { it.first }
            ?.second
    }
}
