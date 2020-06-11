/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class CachedLibraries(
        private val target: KonanTarget,
        allLibraries: List<KotlinLibrary>,
        explicitCaches: Map<KotlinLibrary, String>,
        implicitCacheDirectories: List<File>
) {

    class Cache(val kind: Kind, val path: String) {
        enum class Kind { DYNAMIC, STATIC }
    }

    private fun selectCache(library: KotlinLibrary, cacheDir: File): Cache? {
        // See Linker.renameOutput why is it ok to have an empty cache directory.
        if (cacheDir.listFilesOrEmpty.isEmpty()) return null
        val baseName = getCachedLibraryName(library)
        val dynamicFile = cacheDir.child(getArtifactName(baseName, CompilerOutputKind.DYNAMIC_CACHE))
        val staticFile = cacheDir.child(getArtifactName(baseName, CompilerOutputKind.STATIC_CACHE))

        if (dynamicFile.exists && staticFile.exists)
            error("Both dynamic and static caches files cannot be in the same directory." +
                    " Library: ${library.libraryName}, path to cache: ${cacheDir.absolutePath}")
        return when {
            dynamicFile.exists -> Cache(Cache.Kind.DYNAMIC, dynamicFile.absolutePath)
            staticFile.exists -> Cache(Cache.Kind.STATIC, staticFile.absolutePath)
            else -> error("No cache found for library ${library.libraryName} at ${cacheDir.absolutePath}")
        }
    }

    private val allCaches: Map<KotlinLibrary, Cache> = allLibraries.mapNotNull { library ->
        val explicitPath = explicitCaches[library]

        val cache = if (explicitPath != null) {
            selectCache(library, File(explicitPath))
                    ?: error("No cache found for library ${library.libraryName} at $explicitPath")
        } else {
            implicitCacheDirectories.firstNotNullResult { dir ->
                selectCache(library, dir.child(getCachedLibraryName(library)))
            }
        }

        cache?.let { library to it }
    }.toMap()

    private fun getArtifactName(baseName: String, kind: CompilerOutputKind) =
            "${kind.prefix(target)}$baseName${kind.suffix(target)}"

    fun isLibraryCached(library: KotlinLibrary): Boolean =
            getLibraryCache(library) != null

    fun getLibraryCache(library: KotlinLibrary): Cache? =
            allCaches[library]

    val hasStaticCaches = allCaches.values.any {
        when (it.kind) {
            Cache.Kind.STATIC -> true
            Cache.Kind.DYNAMIC -> false
        }
    }

    val hasDynamicCaches = allCaches.values.any {
        when (it.kind) {
            Cache.Kind.STATIC -> false
            Cache.Kind.DYNAMIC -> true
        }
    }

    companion object {
        fun getCachedLibraryName(library: KotlinLibrary): String = getCachedLibraryName(library.uniqueName)
        fun getCachedLibraryName(libraryName: String): String = "$libraryName-cache"
    }
}