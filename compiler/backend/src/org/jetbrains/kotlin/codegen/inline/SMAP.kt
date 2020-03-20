/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import gnu.trove.TIntIntHashMap
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.SourceInfo
import org.jetbrains.kotlin.codegen.inline.SMAP.Companion.END
import org.jetbrains.kotlin.codegen.inline.SMAP.Companion.FILE_SECTION
import org.jetbrains.kotlin.codegen.inline.SMAP.Companion.LINE_SECTION
import org.jetbrains.kotlin.codegen.inline.SMAP.Companion.STRATA_SECTION
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.util.*
import kotlin.math.max

const val KOTLIN_STRATA_NAME = "Kotlin"
const val KOTLIN_DEBUG_STRATA_NAME = "KotlinDebug"

//TODO join parameter
class SMAPBuilder(
    val source: String,
    val path: String,
    private val fileMappings: List<FileMapping>,
    private val backwardsCompatibleSyntax: Boolean
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
        if (backwardsCompatibleSyntax && defaultStrata.isNotEmpty() && debugStrata.isNotEmpty()) {
            // Old versions of kotlinc might fail if there is no END between defaultStrata and debugStrata.
            // This is not actually correct syntax according to JSR-045.
            return "$header\n$defaultStrata$END\n$debugStrata$END\n"
        }
        return "$header\n$defaultStrata$debugStrata$END\n"
    }

    private fun generateDefaultStrata(realMappings: List<FileMapping>): String {
        val fileIds = FILE_SECTION + realMappings.mapIndexed { id, file -> "\n${file.toSMAPFile(id + 1)}" }.joinToString("")
        val lineMappings = LINE_SECTION + realMappings.joinToString("") { it.toSMAPMapping() }
        return "$STRATA_SECTION $KOTLIN_STRATA_NAME\n$fileIds\n$lineMappings\n"
    }

    private fun generateDebugStrata(realMappings: List<FileMapping>): String {
        val combinedMapping = FileMapping(source, path)
        realMappings.forEach { fileMapping ->
            fileMapping.lineMappings.filter { it.callSiteMarker != null }.forEach { (_, dest, range, callSiteMarker) ->
                combinedMapping.addRangeMapping(RangeMapping(callSiteMarker!!.lineNumber, dest, range))
            }
        }

        if (combinedMapping.lineMappings.isEmpty()) return ""

        val newMappings = listOf(combinedMapping)
        val fileIds = FILE_SECTION + newMappings.mapIndexed { id, file -> "\n${file.toSMAPFile(id + 1)}" }.joinToString("")
        val lineMappings = LINE_SECTION + newMappings.joinToString("") { it.toSMAPMapping() }
        return "$STRATA_SECTION $KOTLIN_DEBUG_STRATA_NAME\n$fileIds\n$lineMappings\n"
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
    override val parent: SourceMapper, protected val smap: SMAP
) : DefaultSourceMapper(smap.sourceInfo) {

    private val visitedLines = TIntIntHashMap()

    private var lastVisitedRange: RangeMapping? = null

    override fun mapLineNumber(lineNumber: Int): Int {
        if (lineNumber in JvmAbi.SYNTHETIC_MARKER_LINE_NUMBERS) {
            return lineNumber
        }

        val mappedLineNumber = visitedLines.get(lineNumber)

        return if (mappedLineNumber > 0) {
            mappedLineNumber
        } else {
            val rangeForMapping =
                (if (lastVisitedRange?.contains(lineNumber) == true) lastVisitedRange!! else smap.findRange(lineNumber))
                    ?: error("Can't find range to map line $lineNumber in ${sourceInfo.source}: ${sourceInfo.pathOrCleanFQN}")
            val sourceLineNumber = rangeForMapping.mapDestToSource(lineNumber)
            val newLineNumber = parent.mapLineNumber(sourceLineNumber, rangeForMapping.parent!!.name, rangeForMapping.parent!!.path)
            if (newLineNumber > 0) {
                visitedLines.put(lineNumber, newLineNumber)
            }
            lastVisitedRange = rangeForMapping
            newLineNumber
        }
    }
}

open class SameFileNestedSourceMapper(parent: SourceMapper, smap: SMAP) : NestedSourceMapper(parent, smap) {
    override fun mapLineNumber(lineNumber: Int): Int {
        if (lineNumber <= smap.sourceInfo.linesInFile) {
            // assuming the parent source mapper is for the same file, this line number does not need remapping
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

    override fun mapLineNumber(source: Int, sourceName: String, sourcePath: String): Int {
        throw UnsupportedOperationException(
            "IdenticalSourceMapper#mapLineNumber($source, $sourceName, $sourcePath)\n"
                    + "This mapper should not encounter a line number out of range of the current file.\n"
                    + "This indicates that SMAP generation is missed somewhere."
        )
    }
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
                    maxUsedValue = max(it.maxDest, maxUsedValue)
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
    val sourceInfo: SourceInfo = run {
        assert(fileMappings.isNotEmpty()) { "File Mappings shouldn't be empty" }
        val defaultFile = fileMappings.first()
        val defaultRange = defaultFile.lineMappings.first()
        SourceInfo(defaultFile.name, defaultFile.path, defaultRange.source + defaultRange.range - 1)
    }

    private val intervals = fileMappings.flatMap { it.lineMappings }.sortedWith(RangeMapping.Comparator)

    fun findRange(lineNumber: Int): RangeMapping? {
        val index = intervals.binarySearch(RangeMapping(lineNumber, lineNumber, 1), Comparator { value, key ->
            if (key.dest in value) 0 else RangeMapping.Comparator.compare(value, key)
        })
        return if (index < 0) null else intervals[index]
    }

    companion object {
        const val FILE_SECTION = "*F"
        const val LINE_SECTION = "*L"
        const val STRATA_SECTION = "*S"
        const val END = "*E"
    }
}

data class SMAPAndMethodNode(val node: MethodNode, val classSMAP: SMAP)

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
        } else {
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
