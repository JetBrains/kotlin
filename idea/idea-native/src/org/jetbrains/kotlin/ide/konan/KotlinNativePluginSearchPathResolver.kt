/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PredefinedKonanTargets
import java.nio.file.Path
import java.nio.file.Paths

internal class KotlinNativePluginSearchPathResolver(bundledLibraryPaths: Iterable<String>) : SearchPathResolverWithTarget {

    override val target: KonanTarget
    override val searchRoots: List<File>

    init {
        val commonLibsRoot = bundledLibraryPaths.firstParentPath { it.endsWith(KONAN_COMMON_LIBS_PATH) }
        val platformLibsRoot = bundledLibraryPaths.firstParentPath { it.parent.endsWith(KONAN_ALL_PLATFORM_LIBS_PATH) }
        val platformName = platformLibsRoot?.fileName?.toString()

        target = if (platformName != null) {
            PredefinedKonanTargets.getByName(platformName) ?: error("Unexpected Kotlin/Native target platform name: $platformName")
        } else {
            // If not possible to determine platform by platform libs root, then fallback to the host platform:
            HostManager.host
        }

        searchRoots = listOfNotNull(commonLibsRoot, platformLibsRoot).map { File(it) }
    }

    override fun resolve(givenPath: String): File {
        val given = File(givenPath)

        if (given.isAbsolute) {
            found(given)?.apply { return this }
        } else {
            searchRoots.forEach {
                found(File(it, givenPath))?.apply { return this }
            }
        }

        error("Could not find \"$givenPath\" in ${searchRoots.map { it.absolutePath }}.")
    }

    private fun found(candidate: File): File? {

        fun checkAsFile(file: File): Boolean = file.isFile

        fun checkAsDirectory(dir: File): Boolean =
            dir.isDirectory && File(dir, "linkdata").listFilesOrEmpty.any { it.extension == KLIB_METADATA_FILE_EXTENSION }

        val candidatePath = candidate.path
        File(candidatePath.removeSuffix(KLIB_FILE_EXTENSION_WITH_DOT)).let { if (checkAsDirectory(it)) return it }
        File(candidatePath.suffixIfNot(KLIB_FILE_EXTENSION_WITH_DOT)).let { if (checkAsFile(it)) return it }

        return null
    }

    // For IDEA plugin all dependencies (KLIBs) should be explicitly provided by underlying layer (Gradle plugin).
    // Therefore, no default libraries.
    override fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean) = emptyList<File>()
}

private fun Iterable<String>.firstParentPath(predicate: (Path) -> Boolean) =
    asSequence().mapNotNull { Paths.get(it).parent }.firstOrNull(predicate)

private fun String.suffixIfNot(suffix: String) = if (this.endsWith(suffix)) this else "$this$suffix"
