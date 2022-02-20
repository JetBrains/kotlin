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

open class LookupStorage(
    targetDataDir: File,
    pathConverter: FileToPathConverter,
    storeFullFqNames: Boolean = false,
    private val trackChanges: Boolean = false
) : BasicMapsOwner(targetDataDir) {
    val LOG = Logger.getInstance("#org.jetbrains.kotlin.jps.build.KotlinBuilder")

    companion object {
        private val DELETED_TO_SIZE_TRESHOLD = 0.5
        private val MINIMUM_GARBAGE_COLLECTIBLE_SIZE = 10000
    }

    private val countersFile = "counters".storageFile
    private val idToFile = registerMap(IdToFileMap("id-to-file".storageFile, pathConverter))
    private val fileToId = registerMap(FileToIdMap("file-to-id".storageFile, pathConverter))
    private val lookupMap = TrackedLookupMap(registerMap(LookupMap("lookups".storageFile, storeFullFqNames)), trackChanges)

    @Volatile
    private var size: Int = 0
    private var oldSize: Int = 0

    init {
        try {
            if (countersFile.exists()) {
                val lines = countersFile.readLines()
                size = lines.firstOrNull()?.toIntOrNull() ?: throw IOException("$countersFile exists, but it is empty. " +
                                                                                       "Counters file is corrupted"
                )
                oldSize = size
            }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Could not read $countersFile", e)
        }
    }

    /** Set of [LookupSymbol]s that have been added after the initialization of this [LookupStorage] instance. */
    val addedLookupSymbols: Set<LookupSymbolKey>
        get() = run {
            check(trackChanges) { "trackChanges is not enabled" }
            lookupMap.addedKeys!!
        }

    /** Set of [LookupSymbol]s that have been removed after the initialization of this [LookupStorage] instance. */
    val removedLookupSymbols: Set<LookupSymbolKey>
        get() = run {
            check(trackChanges) { "trackChanges is not enabled" }
            lookupMap.removedKeys!!
        }

    /** Returns all [LookupSymbol]s in this storage. Note that this call takes a bit of time to run. */
    val lookupSymbols: Collection<LookupSymbolKey>
        get() = lookupMap.keys

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
        }
    }

    @Synchronized
    override fun clean() {
        if (countersFile.exists()) {
            countersFile.delete()
        }

        size = 0

        super.clean()
    }

    @Synchronized
    override fun flush(memoryCachesOnly: Boolean) {
        try {
            if (size != oldSize) {
                if (size > 0) {
                    if (!countersFile.exists()) {
                        countersFile.parentFile.mkdirs()
                        countersFile.createNewFile()
                    }

                    countersFile.writeText("$size\n0")
                }
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

    private fun removeGarbageForTests() {
        for (hash in lookupMap.keys) {
            lookupMap[hash] = lookupMap[hash]!!.filter { it in idToFile }.toSet()
        }

        val oldFileToId = fileToId.toMap()
        val oldIdToNewId = HashMap<Int, Int>(oldFileToId.size)
        idToFile.clean()
        fileToId.clean()
        size = 0

        for ((file, oldId) in oldFileToId.entries.sortedBy { it.key.path }) {
            val newId = addFileIfNeeded(file)
            oldIdToNewId[oldId] = newId
        }

        for (lookup in lookupMap.keys) {
            val fileIds = lookupMap[lookup]!!.mapNotNull { oldIdToNewId[it] }.toSet()

            if (fileIds.isEmpty()) {
                lookupMap.remove(lookup)
            } else {
                lookupMap[lookup] = fileIds
            }
        }
    }


    @TestOnly
    fun forceGC() {
        removeGarbageForTests()
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

    var prevFilePath: String = ""
    var prevPosition: Position? = null
    var prevScopeFqName: String = ""
    var prevScopeKind: ScopeKind? = null
    var prevName: String = ""

    // This method is very hot and sequential invocations usually have the same parameters. Thus we cache previous parameters
    override fun record(filePath: String, position: Position, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        val nameChanged = if (name != prevName) {
            prevName = interner.intern(name)
            true
        } else false
        val fqNameChanged = if (scopeFqName != prevScopeFqName) {
            prevScopeFqName = interner.intern(scopeFqName)
            true
        } else false
        val filePathChanged = if (filePath != prevFilePath) {
            prevFilePath = pathInterner.intern(filePath)
            true
        } else false

        val lookupChanged = nameChanged || fqNameChanged || filePathChanged
        if (lookupChanged) {
            lookups.putValue(LookupSymbol(prevName, prevScopeFqName), prevFilePath)
        }
        if (lookupChanged || prevPosition != position || prevScopeKind != scopeKind) {
            prevPosition = position
            prevScopeKind = scopeKind
            delegate.record(prevFilePath, position, prevScopeFqName, scopeKind, prevName)
        }
    }
}

data class LookupSymbol(val name: String, val scope: String) : Comparable<LookupSymbol> {
    override fun compareTo(other: LookupSymbol): Int {
        val scopeCompare = scope.compareTo(other.scope)
        if (scopeCompare != 0) return scopeCompare

        return name.compareTo(other.name)
    }
}

/**
 * Wrapper of a [LookupMap] which tracks changes to the map after the initialization of this [TrackedLookupMap] instance (unless
 * [trackChanges] is set to `false`).
 */
private class TrackedLookupMap(private val lookupMap: LookupMap, private val trackChanges: Boolean) {

    // Note that there may be multiple operations on the same key, and the following sets contain the *aggregated* differences with the
    // original set of keys in the map. For example, if a key is added then removed, or vice versa, it will not be present in either set.
    val addedKeys = if (trackChanges) mutableSetOf<LookupSymbolKey>() else null
    val removedKeys = if (trackChanges) mutableSetOf<LookupSymbolKey>() else null

    val keys: Collection<LookupSymbolKey>
        get() = lookupMap.keys

    operator fun get(key: LookupSymbolKey): Collection<Int>? = lookupMap[key]

    operator fun set(key: LookupSymbolKey, fileIds: Set<Int>) {
        recordSet(key)
        lookupMap[key] = fileIds
    }

    fun append(key: LookupSymbolKey, fileIds: Collection<Int>) {
        recordSet(key)
        lookupMap.append(key, fileIds)
    }

    fun remove(key: LookupSymbolKey) {
        recordRemove(key)
        lookupMap.remove(key)
    }

    private fun recordSet(key: LookupSymbolKey) {
        if (!trackChanges) return
        if (lookupMap[key] == null) {
            if (key in removedKeys!!) {
                removedKeys.remove(key)
            } else {
                addedKeys!!.add(key)
            }
        }
    }

    private fun recordRemove(key: LookupSymbolKey) {
        if (!trackChanges) return
        if (lookupMap[key] != null) {
            if (key in addedKeys!!) {
                addedKeys.remove(key)
            } else {
                removedKeys!!.add(key)
            }
        }
    }
}
