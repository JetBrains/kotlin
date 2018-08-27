/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan

import com.intellij.openapi.project.Project
import org.jetbrains.konan.settings.KonanPaths
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget

class KonanPluginSearchPathResolver(val project: Project) : SearchPathResolverWithTarget {

    private val paths by lazy { KonanPaths(project) }

    override val searchRoots: List<File> by lazy {
        paths.konanDist()?.let { distPath ->
            listOf(KONAN_COMMON_LIBS_PATH, konanSpecificPlatformLibrariesPath(target.toString())).mapNotNull { relativePath ->
                distPath.resolve(relativePath).File().takeIf { it.exists }
            }
        } ?: emptyList()
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

    override fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean): List<File> {

        val result = mutableListOf<File>()

        if (!noStdLib) {
            result.add(resolve(KONAN_STDLIB_NAME))
        }

        if (!noDefaultLibs) {
            val defaultLibs = searchRoots.flatMap { it.listFiles }
                .filterNot { it.name.removeSuffix(KLIB_FILE_EXTENSION_WITH_DOT) == KONAN_STDLIB_NAME }
                .map { File(it.absolutePath) }
            result.addAll(defaultLibs)
        }

        return result
    }

    override val target: KonanTarget
        get() = paths.target()

    private fun found(candidate: File): File? {

        fun check(file: File): Boolean =
            file.exists && (file.isFile || File(file, "manifest").exists)

        val noSuffix = File(candidate.path.removeSuffix(KLIB_FILE_EXTENSION_WITH_DOT))
        val withSuffix = File(candidate.path.suffixIfNot(KLIB_FILE_EXTENSION_WITH_DOT))

        return when {
            check(withSuffix) -> withSuffix
            check(noSuffix) -> noSuffix
            else -> null
        }
    }
}

private fun String.suffixIfNot(suffix: String) = if (this.endsWith(suffix)) this else "$this$suffix"
