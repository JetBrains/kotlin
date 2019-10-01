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

internal class CachedLibraries(
        private val target: KonanTarget,
        allLibraries: List<KotlinLibrary>,
        explicitCaches: Map<KotlinLibrary, String>,
        implicitCacheDirectories: List<File>
) {

    class Cache(val kind: Kind, val path: String) {
        enum class Kind { DYNAMIC, STATIC }
    }

    private val allCaches: Map<KotlinLibrary, Cache> = allLibraries.mapNotNull { library ->
        val explicitPath = explicitCaches[library]

        val cache = if (explicitPath != null) {
            val kind = when {
                explicitPath.endsWith(target.family.dynamicSuffix) -> Cache.Kind.DYNAMIC
                explicitPath.endsWith(target.family.staticSuffix) -> Cache.Kind.STATIC
                else -> error("unexpected cache: $explicitPath")
            }
            Cache(kind, explicitPath)
        } else {
            implicitCacheDirectories.firstNotNullResult { dir ->
                val baseName = "${library.uniqueName}-cache"
                val dynamicFile = dir.child(getArtifactName(baseName, CompilerOutputKind.DYNAMIC_CACHE))
                val staticFile = dir.child(getArtifactName(baseName, CompilerOutputKind.STATIC_CACHE))

                when {
                    dynamicFile.exists -> Cache(Cache.Kind.DYNAMIC, dynamicFile.absolutePath)
                    staticFile.exists -> Cache(Cache.Kind.STATIC, staticFile.absolutePath)
                    else -> null
                }
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
}