/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.inline

import gnu.trove.TIntIntHashMap
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.SourceInfo
import java.util.*

val KOTLIN_STRATA_NAME = "Kotlin"
val KOTLIN_DEBUG_STRATA_NAME = "KotlinDebug"

//TODO join parameter
class SMAPBuilder(
        val source: String,
        val path: String,
        val fileMappings: List<FileMapping>
) {
    private val header = "SMAP\n$source\nKotlin"

    fun build(): String? {
        val realMappings = fileMappings.filter {
            val mappings = it.lineMappings
            mappings.isNotEmpty() && mappings.first() != RangeMapping.SKIP
        }

        if (realMappings.isEmpty()) {
            return null
        }

        val defaultStrata = generateDefaultStrata(realMappings)
        val debugStrata = generateDebugStrata(realMappings)

        return "$header\n$defaultStrata$debugStrata"
    }

    private fun generateDefaultStrata(realMappings: List<FileMapping>): String {
        val fileIds = "*F" + realMappings.mapIndexed { id, file -> "\n${file.toSMAPFile(id + 1)}" }.joinToString("")
        val lineMappings = "*L" + realMappings.joinToString("") { it.toSMAPMapping() }
        return "*S $KOTLIN_STRATA_NAME\n$fileIds\n$lineMappings\n*E\n"
    }

    private fun generateDebugStrata(realMappings: List<FileMapping>): String {
        val combinedMapping = FileMapping(source, path)
        realMappings.forEach { fileMapping ->
            fileMapping.lineMappings.filter { it.callSiteMarker != null }.forEach { (source, dest, range, callSiteMarker) ->
                combinedMapping.addRangeMapping(RangeMapping(
                        callSiteMarker!!.lineNumber, dest, range
                ))
            }
        }

        if (combinedMapping.lineMappings.isEmpty()) return ""

        val newMappings = listOf(combinedMapping)
        val fileIds = "*F" + newMappings.mapIndexed { id, file -> "\n${file.toSMAPFile(id + 1)}" }.joinToString("")
        val lineMappings = "*L" + newMappings.joinToString("") { it.toSMAPMapping() }
        return "*S $KOTLIN_DEBUG_STRATA_NAME\n$fileIds\n$lineMappings\n*E\n"
    }

    private fun RangeMapping.toSMAP(fileId: Int): String {
        return if (range == 1) "$source#$fileId:$dest" else "$source#$fileId,$range:$dest"
    }

    private fun FileMapping.toSMAPFile(id: Int): String {
        this.id = id
        return "+ $id $name\n$path"
    }

    //TODO inline
    private fun FileMapping.toSMAPMapping(): String {
        return lineMappings.joinToString("") { "\n${it.toSMAP(id)}" }
    }
}

open class NestedSourceMapper(
        override val parent: SourceMapper, val ranges: List<RangeMapping>, sourceInfo: SourceInfo
) : DefaultSourceMapper(sourceInfo) {

    val visitedLines = TIntIntHashMap()

    var lastVisitedRange: RangeMapping? = null

    override fun mapLineNumber(lineNumber: Int): Int {
        val mappedLineNumber = visitedLines.get(lineNumber)

        if (mappedLineNumber > 0) {
            return mappedLineNumber
        }
        else {
            val rangeForMapping =
                    (if (lastVisitedRange?.contains(lineNumber) ?: false) lastVisitedRange!! else findMappingIfExists(lineNumber))
                    ?: error("Can't find range to map line $lineNumber in ${sourceInfo.source}: ${sourceInfo.pathOrCleanFQN}")
            val sourceLineNumber = rangeForMapping.mapDestToSource(lineNumber)
            val newLineNumber = parent.mapLineNumber(sourceLineNumber, rangeForMapping.parent!!.name, rangeForMapping.parent!!.path)
            if (newLineNumber > 0) {
                visitedLines.put(lineNumber, newLineNumber)
            }
            lastVisitedRange = rangeForMapping
            return newLineNumber
        }
    }

    fun findMappingIfExists(lineNumber: Int): RangeMapping? {
        val index = ranges.binarySearch(RangeMapping(lineNumber, lineNumber, 1), Comparator {
            value, key ->
            if (key.dest in value) 0 else RangeMapping.Comparator.compare(value, key)
        })
        return if (index < 0) null else ranges[index]
    }
}

open class InlineLambdaSourceMapper(
        parent: SourceMapper, smap: SMAPAndMethodNode
) : NestedSourceMapper(parent, smap.sortedRanges, smap.classSMAP.sourceInfo) {

    init {
        assert(ranges.isNotEmpty()) {
            "Mapping ranges should be presented in inline lambda: ${smap.node}"
        }
    }

    override fun mapLineNumber(lineNumber: Int): Int {
        if (ranges.firstOrNull()?.contains(lineNumber) ?: false) {
            //don't remap origin lambda line numbers
            return lineNumber
        }
        return super.mapLineNumber(lineNumber)
    }
}

interface SourceMapper {
    val resultMappings: List<FileMapping>
    val parent: SourceMapper?
        get() = null

    fun mapLineNumber(lineNumber: Int): Int {
        throw UnsupportedOperationException("fail")
    }

    fun mapLineNumber(source: Int, sourceName: String, sourcePath: String): Int {
        throw UnsupportedOperationException("fail")
    }

    fun endMapping() {
    }

    companion object {
        fun flushToClassBuilder(mapper: SourceMapper, v: ClassBuilder) {
            mapper.resultMappings.forEach { fileMapping -> v.addSMAP(fileMapping) }
        }

        fun createFromSmap(smap: SMAP): SourceMapper {
            return DefaultSourceMapper(smap.sourceInfo, smap.fileMappings)
        }
    }
}

object IdenticalSourceMapper : SourceMapper {
    override val resultMappings: List<FileMapping>
        get() = emptyList()

    override val parent: SourceMapper?
        get() = null

    override fun mapLineNumber(lineNumber: Int) = lineNumber
}

class CallSiteMarker(val lineNumber: Int)

open class DefaultSourceMapper(val sourceInfo: SourceInfo) : SourceMapper {
    private var maxUsedValue: Int = sourceInfo.linesInFile
    private var lastMappedWithChanges: RawFileMapping? = null
    private var fileMappings: LinkedHashMap<String, RawFileMapping> = linkedMapOf()

    protected val origin: RawFileMapping

    var callSiteMarker: CallSiteMarker? = null
        set(value) {
            lastMappedWithChanges = null
            field = value
        }

    override val resultMappings: List<FileMapping>
        get() = fileMappings.values.map { it.toFileMapping() }

    init {
        val name = sourceInfo.source
        val path = sourceInfo.pathOrCleanFQN
        origin = RawFileMapping(name, path)
        origin.initRange(1, sourceInfo.linesInFile)
        fileMappings.put(createKey(name, path), origin)
    }

    constructor(sourceInfo: SourceInfo, fileMappings: List<FileMapping>) : this(sourceInfo) {
        fileMappings.asSequence().drop(1)
                //default one mapped through sourceInfo
                .forEach { fileMapping ->
                    val newFileMapping = getOrRegisterNewSource(fileMapping.name, fileMapping.path)
                    fileMapping.lineMappings.forEach {
                        newFileMapping.mapNewInterval(it.source, it.dest, it.range)
                        maxUsedValue = Math.max(it.maxDest, maxUsedValue)
                    }
                }
    }

    private fun createKey(name: String, path: String) = "$name#$path"

    private fun getOrRegisterNewSource(name: String, path: String): RawFileMapping {
        return fileMappings.getOrPut(createKey(name, path)) { RawFileMapping(name, path) }
    }

    override fun mapLineNumber(lineNumber: Int): Int {
        if (lineNumber < 0) {
            //no source information, so just skip this linenumber
            return -1
        }
        //TODO maybe add assertion that linenumber contained in fileMappings
        return lineNumber
    }

    override fun mapLineNumber(source: Int, sourceName: String, sourcePath: String): Int {
        if (source < 0) {
            //no source information, so just skip this linenumber
            return -1
        }
        return createMapping(getOrRegisterNewSource(sourceName, sourcePath), source)
    }

    private fun createMapping(fileMapping: RawFileMapping, lineNumber: Int): Int {
        val mappedLineIndex = fileMapping.mapNewLineNumber(lineNumber, maxUsedValue, lastMappedWithChanges == fileMapping, callSiteMarker)
        if (mappedLineIndex > maxUsedValue) {
            lastMappedWithChanges = fileMapping
            maxUsedValue = mappedLineIndex
        }
        return mappedLineIndex
    }
}

class SMAP(val fileMappings: List<FileMapping>) {
    init {
        assert(fileMappings.isNotEmpty()) { "File Mappings shouldn't be empty" }
    }

    val default: FileMapping
        get() = fileMappings.first()

    val intervals = fileMappings.flatMap { it.lineMappings }.sortedWith(RangeMapping.Comparator)

    val sourceInfo: SourceInfo

    init {
        val defaultMapping = default.lineMappings.first()
        sourceInfo = SourceInfo(default.name, default.path, defaultMapping.source + defaultMapping.range - 1)
    }

    companion object {
        const val FILE_SECTION = "*F"
        const val LINE_SECTION = "*L"
        const val END = "*E"
    }
}


class RawFileMapping(val name: String, val path: String) {
    private val rangeMappings = arrayListOf<RangeMapping>()

    private var lastMappedWithNewIndex = -1000

    fun toFileMapping() =
            FileMapping(name, path).apply {
                for (range in rangeMappings) {
                    addRangeMapping(range)
                }
            }

    fun initRange(start: Int, end: Int) {
        assert(rangeMappings.isEmpty()) { "initRange should only be called for empty mapping" }
        rangeMappings.add(RangeMapping(start, start, end - start + 1))
        lastMappedWithNewIndex = end
    }

    fun mapNewLineNumber(source: Int, currentIndex: Int, isLastMapped: Boolean, callSiteMarker: CallSiteMarker?): Int {
        val dest: Int
        val rangeMapping: RangeMapping
        if (rangeMappings.isNotEmpty() && isLastMapped && couldFoldInRange(lastMappedWithNewIndex, source)) {
            rangeMapping = rangeMappings.last()
            rangeMapping.range += source - lastMappedWithNewIndex
            dest = rangeMapping.mapSourceToDest(source)
        }
        else {
            dest = currentIndex + 1
            rangeMapping = RangeMapping(source, dest, callSiteMarker = callSiteMarker)
            rangeMappings.add(rangeMapping)
        }

        lastMappedWithNewIndex = source
        return dest
    }

    fun mapNewInterval(source: Int, dest: Int, range: Int) {
        val rangeMapping = RangeMapping(source, dest, range)
        rangeMappings.add(rangeMapping)
    }

    private fun couldFoldInRange(first: Int, second: Int): Boolean {
        //TODO
        val delta = second - first
        return delta > 0 && delta <= 10
    }
}

open class FileMapping(val name: String, val path: String) {
    val lineMappings = arrayListOf<RangeMapping>()
    var id = -1

    fun addRangeMapping(lineMapping: RangeMapping) {
        lineMappings.add(lineMapping)
        lineMapping.parent = this
    }

    object SKIP : FileMapping("no-source-info", "no-source-info") {
        init {
            addRangeMapping(RangeMapping.SKIP)
        }
    }
}

//TODO comparable
data class RangeMapping(val source: Int, val dest: Int, var range: Int = 1, var callSiteMarker: CallSiteMarker? = null) {
    var parent: FileMapping? = null
    private val skip = source == -1 && dest == -1

    val maxDest: Int
        get() = dest + range - 1

    operator fun contains(destLine: Int): Boolean {
        return skip || (dest <= destLine && destLine < dest + range)
    }

    fun hasMappingForSource(sourceLine: Int): Boolean {
        return skip || (source <= sourceLine && sourceLine < source + range)
    }

    fun mapDestToSource(destLine: Int): Int {
        return if (skip) -1 else source + (destLine - dest)
    }

    fun mapSourceToDest(sourceLine: Int): Int {
        return if (skip) -1 else dest + (sourceLine - source)
    }

    object Comparator : java.util.Comparator<RangeMapping> {
        override fun compare(o1: RangeMapping, o2: RangeMapping): Int {
            if (o1 == o2) return 0

            val res = o1.dest - o2.dest
            return if (res == 0) o1.range - o2.range else res
        }
    }

    companion object {
        val SKIP = RangeMapping(-1, -1, 1)
    }
}

val RangeMapping.toRange: IntRange
    get() = this.dest..this.maxDest
