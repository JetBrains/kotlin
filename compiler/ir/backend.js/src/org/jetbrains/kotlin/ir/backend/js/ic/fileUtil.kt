/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.toByteArray
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.javaFile
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.name.FqName
import java.io.File
import java.io.PrintWriter
import java.security.MessageDigest
import kotlin.random.Random
import kotlin.random.nextULong

// TODO: Proper version of the compiler (should take changes to lowerings into account)
private val compilerVersion = Random.nextULong()

// TODO more parameters for lowerings
fun buildCache(
    cachePath: String,
    project: Project,
    mainModule: MainModule.Klib,
    analyzer: AbstractAnalyzerWithCompilerReport,
    configuration: CompilerConfiguration,
    allDependencies: KotlinLibraryResolveResult,
    friendDependencies: List<KotlinLibrary>,
    exportedDeclarations: Set<FqName> = emptySet(),
    forceClean: Boolean = false,
    icCache: IcCacheInfo = IcCacheInfo.EMPTY,
) {
    val dependencyHashes = allDependencies.getFullList(TopologicalLibraryOrder).mapNotNull {
        val path = it.libraryFile.absolutePath
        icCache.md5[path]
    } + compilerVersion

    val md5 = mainModule.lib.libraryFile.javaFile().md5(dependencyHashes)

    if (!forceClean) {
        val oldCacheInfo = CacheInfo.load(cachePath)
        if (oldCacheInfo != null && md5 == oldCacheInfo.md5) return
    }

    val icDir = File(cachePath)
    icDir.deleteRecursively()
    icDir.mkdirs()

    val icData = prepareSingleLibraryIcCache(project, analyzer, configuration, mainModule.lib, allDependencies, friendDependencies, exportedDeclarations, icCache.data)

    icData.writeTo(File(cachePath))

    CacheInfo(cachePath, mainModule.lib.libraryFile.absolutePath, md5).save()
}

private fun File.md5(additional: Iterable<ULong> = emptyList()): ULong {
    val md5 = MessageDigest.getInstance("MD5")

    for (ul in additional) {
        md5.update(ul.toLong().toByteArray())
    }

    fun File.process(prefix: String = "") {
        if (isDirectory) {
            this.listFiles()!!.sortedBy { it.name }.forEach {
                md5.update((prefix + it.name).toByteArray())
                it.process(prefix + it.name + "/")
            }
        } else {
            md5.update(readBytes())
        }
    }

    this.process()

    val d = md5.digest()

    return ((d[0].toULong() and 0xFFUL)
            or ((d[1].toULong() and 0xFFUL) shl 8)
            or ((d[2].toULong() and 0xFFUL) shl 16)
            or ((d[3].toULong() and 0xFFUL) shl 24)
            or ((d[4].toULong() and 0xFFUL) shl 32)
            or ((d[5].toULong() and 0xFFUL) shl 40)
            or ((d[6].toULong() and 0xFFUL) shl 48)
            or ((d[7].toULong() and 0xFFUL) shl 56)
            )
}

fun checkCaches(
    allDependencies: KotlinLibraryResolveResult,
    cachePaths: List<String>,
    skipLib: String? = null,
): IcCacheInfo {
    val allLibs = allDependencies.getFullList().map { it.libraryFile.absolutePath }.toSet() - skipLib

    val caches = cachePaths.map { CacheInfo.load(it) ?: error("Cannot load IC cache from ${it}") }

    val missedLibs = allLibs - caches.map { it.libPath }
    if (!missedLibs.isEmpty()) {
        error("Missing caches for libraries: ${missedLibs}")
    }

    val result = mutableMapOf<String, SerializedIcData>()
    val md5 = mutableMapOf<String, ULong>()

    for (c in caches) {
        if (c.libPath !in allLibs) error("Missing library: ${c.libPath}")

        result[c.libPath] = File(c.path).readIcData()
        md5[c.libPath] = c.md5
    }

    return IcCacheInfo(result, md5)
}

// TODO md5 hash
data class CacheInfo(val path: String, val libPath: String, val md5: ULong) {
    fun save() {
        PrintWriter(File(File(path), "info")).use {
            it.println(libPath)
            it.println(md5.toString(16))
        }
    }

    companion object {
        fun load(path: String): CacheInfo? {
            val info = File(File(path), "info")

            if (!info.exists()) return null

            val (libPath, md5) = info.readLines()

            return CacheInfo(path, libPath, md5.toULong(16))
        }
    }
}


class IcCacheInfo(
    val data: Map<String, SerializedIcData>,
    val md5: Map<String, ULong>,
) {
    companion object {
        val EMPTY = IcCacheInfo(emptyMap(), emptyMap())
    }
}