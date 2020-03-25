/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import gnu.trove.TIntIntHashMap
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

class NestedSourceMapper(
    override val parent: SourceMapper, private val smap: SMAP, private val sameFile: Boolean = false
) : DefaultSourceMapper(smap.sourceInfo) {

    private val visitedLines = TIntIntHashMap()

    private var lastVisitedRange: RangeMapping? = null

    override fun mapLineNumber(lineNumber: Int): Int {
        if (lineNumber in JvmAbi.SYNTHETIC_MARKER_LINE_NUMBERS) {
            return lineNumber
        }

        if (sameFile && lineNumber <= smap.sourceInfo.linesInFile) {
            // assuming the parent source mapper is for the same file, this line number does not need remapping
            return lineNumber
        }

        val mappedLineNumber = visitedLines.get(lineNumber)
        if (mappedLineNumber > 0) {
            return mappedLineNumber
        }

        val range = lastVisitedRange?.takeIf { lineNumber in it }
            ?: smap.findRange(lineNumber)
            ?: error("Can't find range to map line $lineNumber in ${sourceInfo.source}: ${sourceInfo.pathOrCleanFQN}")
        val sourceLineNumber = range.mapDestToSource(lineNumber)
        val newLineNumber = if (sameFile)
            parent.mapLineNumber(sourceLineNumber, range.parent!!.name, range.parent!!.path, range.callSiteMarker)
        else
            parent.mapLineNumber(sourceLineNumber, range.parent!!.name, range.parent!!.path)
        if (newLineNumber > 0) {
            visitedLines.put(lineNumber, newLineNumber)
        }
        lastVisitedRange = range
        return newLineNumber
    }
}

interface SourceMapper {
    val resultMappings: List<FileMapping>
    val parent: SourceMapper?
        get() = null

    fun mapLineNumber(lineNumber: Int): Int

    fun mapLineNumber(source: Int, sourceName: String, sourcePath: String): Int =
        mapLineNumber(source, sourceName, sourcePath, null)

    fun mapLineNumber(source: Int, sourceName: String, sourcePath: String, callSiteMarker: CallSiteMarker?): Int

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

    override fun mapLineNumber(source: Int, sourceName: String, sourcePath: String, callSiteMarker: CallSiteMarker?): Int =
        throw UnsupportedOperationException(
            "IdenticalSourceMapper#mapLineNumber($source, $sourceName, $sourcePath)\n"
                    + "This mapper should not encounter a line number out of range of the current file.\n"
                    + "This indicates that SMAP generation is missed somewhere."
        )
}

data class CallSiteMarker(val lineNumber: Int)

open class DefaultSourceMapper(val sourceInfo: SourceInfo) : SourceMapper {
    private var maxUsedValue: Int = sourceInfo.linesInFile
    private var fileMappings: LinkedHashMap<Pair<String, String>, RawFileMapping> = linkedMapOf()

    var callSiteMarker: CallSiteMarker? = null

    override val resultMappings: List<FileMapping>
        get() = fileMappings.values.map { it.toFileMapping() }

    init {
        // Explicitly map the file to itself.
        getOrRegisterNewSource(sourceInfo.source, sourceInfo.pathOrCleanFQN).mapNewInterval(1, 1, sourceInfo.linesInFile)
    }

    constructor(sourceInfo: SourceInfo, fileMappings: List<FileMapping>) : this(sourceInfo) {
        // The first mapping is already created in the `init` block above.
        fileMappings.asSequence().drop(1)
            .forEach { fileMapping ->
                val newFileMapping = getOrRegisterNewSource(fileMapping.name, fileMapping.path)
                fileMapping.lineMappings.forEach {
                    newFileMapping.mapNewInterval(it.source, it.dest, it.range)
                    maxUsedValue = max(it.maxDest, maxUsedValue)
                }
            }
    }

    private fun getOrRegisterNewSource(name: String, path: String): RawFileMapping {
        return fileMappings.getOrPut(name to path) { RawFileMapping(name, path) }
    }

    override fun mapLineNumber(lineNumber: Int): Int {
        if (lineNumber < 0) {
            //no source information, so just skip this linenumber
            return -1
        }
        //TODO maybe add assertion that linenumber contained in fileMappings
        return lineNumber
    }

    override fun mapLineNumber(source: Int, sourceName: String, sourcePath: String): Int =
        mapLineNumber(source, sourceName, sourcePath, callSiteMarker)

    override fun mapLineNumber(source: Int, sourceName: String, sourcePath: String, callSiteMarker: CallSiteMarker?): Int {
        if (source < 0) {
            //no source information, so just skip this linenumber
            return -1
        }
        val fileMapping = getOrRegisterNewSource(sourceName, sourcePath)
        val mappedLineIndex = fileMapping.mapNewLineNumber(source, maxUsedValue, callSiteMarker)
        maxUsedValue = max(maxUsedValue, mappedLineIndex)
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

    fun toFileMapping() =
        FileMapping(name, path).apply {
            for (range in rangeMappings) {
                addRangeMapping(range)
            }
        }

    fun mapNewLineNumber(source: Int, currentIndex: Int, callSiteMarker: CallSiteMarker?): Int {
        var mapping = rangeMappings.lastOrNull()
        if (mapping != null && mapping.callSiteMarker == callSiteMarker &&
            (source - mapping.source) in 0 until mapping.range + (if (mapping.maxDest == currentIndex) 10 else 0)
        ) {
            // Save some space in the SMAP by reusing (or extending if it's the last one) the existing range.
            mapping.range = max(mapping.range, source - mapping.source + 1)
        } else {
            mapping = mapNewInterval(source, currentIndex + 1, 1, callSiteMarker)
        }
        return mapping.mapSourceToDest(source)
    }

    fun mapNewInterval(source: Int, dest: Int, range: Int, callSiteMarker: CallSiteMarker? = null): RangeMapping =
        RangeMapping(source, dest, range, callSiteMarker).also { rangeMappings.add(it) }
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
