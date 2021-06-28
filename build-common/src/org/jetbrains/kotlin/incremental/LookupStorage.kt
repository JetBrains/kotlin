/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.containers.MultiMap
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.createStringInterner
import org.jetbrains.kotlin.utils.keysToMap
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.system.measureTimeMillis

open class LookupStorage(
    targetDataDir: File,
    pathConverter: FileToPathConverter
) : BasicMapsOwner(targetDataDir) {
    val LOG = Logger.getInstance("#org.jetbrains.kotlin.jps.build.KotlinBuilder")

    companion object {
        private val DELETED_TO_SIZE_TRESHOLD = 0.5
        private val MINIMUM_GARBAGE_COLLECTIBLE_SIZE = 10000
    }

    private val countersFile = "counters".storageFile
    private val idToFile = registerMap(IdToFileMap("id-to-file".storageFile, pathConverter))
    private val fileToId = registerMap(FileToIdMap("file-to-id".storageFile, pathConverter))
    val lookupMap = registerMap(LookupMap("lookups".storageFile))

    @Volatile
    private var size: Int = 0

    @Volatile
    private var deletedCount: Int = 0

    init {
        try {
            if (countersFile.exists()) {
                val lines = countersFile.readLines()
                size = lines[0].toInt()
                deletedCount = lines[1].toInt()
            }
        } catch (e: Exception) {
            throw IOException("Could not read $countersFile", e)
        }
    }

    @Synchronized
    fun get(lookupSymbol: LookupSymbol): Collection<String> {
        val key = LookupSymbolKey(lookupSymbol.name, lookupSymbol.scope)
        val fileIds = lookupMap[key] ?: return emptySet()
        val paths = mutableSetOf<String>()
        val filtered = mutableSetOf<Int>()

        for (fileId in fileIds) {
            val path = idToFile[fileId]?.path

            if (path != null) {
                paths.add(path)
                filtered.add(fileId)
            }

        }

        if (size > MINIMUM_GARBAGE_COLLECTIBLE_SIZE && filtered.size.toDouble() / fileIds.size.toDouble() < DELETED_TO_SIZE_TRESHOLD) {
            lookupMap[key] = filtered
        }

        return paths
    }

    @Synchronized
    fun addAll(lookups: MultiMap<LookupSymbol, String>, allPaths: Set<String>) {
        val pathToId = allPaths.sorted().keysToMap { addFileIfNeeded(File(it)) }

        for (lookupSymbol in lookups.keySet().sorted()) {
            val key = LookupSymbolKey(lookupSymbol.name, lookupSymbol.scope)
            val paths = lookups[lookupSymbol]
            val fileIds = paths.mapTo(TreeSet()) { pathToId[it]!! }

            lookupMap.append(key, fileIds)
        }
    }

    @Synchronized
    fun removeLookupsFrom(files: Sequence<File>) {
        for (file in files) {
            val id = fileToId[file] ?: continue
            idToFile.remove(id)
            fileToId.remove(file)
            deletedCount++
        }
    }

    @Synchronized
    override fun clean() {
        if (countersFile.exists()) {
            countersFile.delete()
        }

        size = 0
        deletedCount = 0

        super.clean()
    }

    @Synchronized
    override fun flush(memoryCachesOnly: Boolean) {
        try {
            removeGarbageIfNeeded()

            if (size > 0) {
                if (!countersFile.exists()) {
                    countersFile.parentFile.mkdirs()
                    countersFile.createNewFile()
                }

                countersFile.writeText("$size\n$deletedCount")
            }
        } finally {
            super.flush(memoryCachesOnly)
        }
    }

    private fun addFileIfNeeded(file: File): Int {
        val existing = fileToId[file]
        if (existing != null) return existing

        val id = size++
        fileToId[file] = id
        idToFile[id] = file
        return id
    }

    private fun removeGarbageIfNeeded(force: Boolean = false) {
        if (force && (size > MINIMUM_GARBAGE_COLLECTIBLE_SIZE && deletedCount.toDouble() / size > DELETED_TO_SIZE_TRESHOLD)) {
            doRemoveGarbage()
        }
    }

    private fun doRemoveGarbage() {
        val timeInMillis = measureTimeMillis {
            for (hash in lookupMap.keys) {
                val dirtyFileIds = lookupMap[hash]!!
                val filteredFileIds = dirtyFileIds.filter { it in idToFile }.toSet()
                if (dirtyFileIds != filteredFileIds) lookupMap[hash] = filteredFileIds
            }

            deletedCount = 0
        }
        LOG.debug(">>Garbage removed in $timeInMillis ms")
    }

    @TestOnly
    fun forceGC() {
        removeGarbageIfNeeded(force = true)
        flush(false)
    }

    @TestOnly
    fun dump(lookupSymbols: Set<LookupSymbol>): String {
        flush(false)

        val sb = StringBuilder()
        val p = Printer(sb)

        p.println("====== File to id map")
        p.println(fileToId.dump())

        p.println("====== Id to file map")
        p.println(idToFile.dump())

        val lookupsStrings = lookupSymbols.groupBy { LookupSymbolKey(it.name, it.scope) }

        for (lookup in lookupMap.keys.sorted()) {
            val fileIds = lookupMap[lookup]!!

            val key = if (lookup in lookupsStrings) {
                lookupsStrings[lookup]!!.map { "${it.scope}#${it.name}" }.sorted().joinToString(", ")
            } else {
                lookup.toString()
            }

            val value = fileIds.map { it.toString() }.sorted().joinToString(", ")
            p.println("$key -> $value")
        }

        return sb.toString()
    }
}

class LookupTrackerImpl(private val delegate: LookupTracker) : LookupTracker {
    val lookups = MultiMap.createSet<LookupSymbol, String>()
    val pathInterner = createStringInterner()
    private val interner = createStringInterner()

    override val requiresPosition: Boolean
        get() = delegate.requiresPosition

    override fun record(filePath: String, position: Position, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        val internedScopeFqName = interner.intern(scopeFqName)
        val internedName = interner.intern(name)
        val internedFilePath = pathInterner.intern(filePath)

        lookups.putValue(LookupSymbol(internedName, internedScopeFqName), internedFilePath)
        delegate.record(internedFilePath, position, internedScopeFqName, scopeKind, internedName)
    }
}

data class LookupSymbol(val name: String, val scope: String) : Comparable<LookupSymbol> {
    override fun compareTo(other: LookupSymbol): Int {
        val scopeCompare = scope.compareTo(other.scope)
        if (scopeCompare != 0) return scopeCompare

        return name.compareTo(other.name)
    }
}
