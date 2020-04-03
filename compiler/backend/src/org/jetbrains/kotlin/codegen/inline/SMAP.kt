/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import gnu.trove.TIntIntHashMap
import org.jetbrains.kotlin.codegen.SourceInfo
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.util.*
import kotlin.math.max

const val KOTLIN_STRATA_NAME = "Kotlin"
const val KOTLIN_DEBUG_STRATA_NAME = "KotlinDebug"

object SMAPBuilder {
    fun build(fileMappings: List<FileMapping>, backwardsCompatibleSyntax: Boolean): String? {
        val realMappings = fileMappings.filter { it.lineMappings.isNotEmpty() && it != FileMapping.SKIP }
        if (realMappings.isEmpty()) {
            return null
        }
        val defaultStrata = generateDefaultStrata(realMappings)
        val debugStrata = generateDebugStrata(realMappings)
        if (backwardsCompatibleSyntax && defaultStrata.isNotEmpty() && debugStrata.isNotEmpty()) {
            // Old versions of kotlinc might fail if there is no END between defaultStrata and debugStrata.
            // This is not actually correct syntax according to JSR-045.
            return "SMAP\n${fileMappings[0].name}\n$KOTLIN_STRATA_NAME\n$defaultStrata${SMAP.END}\n$debugStrata${SMAP.END}\n"
        }
        return "SMAP\n${fileMappings[0].name}\n$KOTLIN_STRATA_NAME\n$defaultStrata$debugStrata${SMAP.END}\n"
    }

    private fun generateDefaultStrata(realMappings: List<FileMapping>): String {
        val fileData = realMappings.mapIndexed { id, file -> file.toSMAPFile(id + 1) }.joinToString("")
        val lineData = realMappings.mapIndexed { id, file -> file.toSMAPMapping(id + 1) }.joinToString("")
        return "${SMAP.STRATA_SECTION} $KOTLIN_STRATA_NAME\n${SMAP.FILE_SECTION}\n$fileData${SMAP.LINE_SECTION}\n$lineData"
    }

    private fun generateDebugStrata(realMappings: List<FileMapping>): String {
        val combinedMapping = FileMapping(realMappings[0].name, realMappings[0].path)
        for (fileMapping in realMappings) {
            for ((_, dest, range, callSiteMarker) in fileMapping.lineMappings) {
                callSiteMarker?.let { combinedMapping.mapNewInterval(it.lineNumber, dest, range) }
            }
        }

        if (combinedMapping.lineMappings.isEmpty()) return ""
        val fileData = combinedMapping.toSMAPFile(1)
        // TODO: this generates entries like `1#2,3:4` which means "map lines 4..6 to lines 1..3 of file #2".
        //   What we want is `1#2:4,3`, i.e. "map lines 4..6 to line 1 of #2", but currently IDEA cannot handle that.
        val lineData = combinedMapping.toSMAPMapping(1)
        return "${SMAP.STRATA_SECTION} $KOTLIN_DEBUG_STRATA_NAME\n${SMAP.FILE_SECTION}\n$fileData${SMAP.LINE_SECTION}\n$lineData"
    }

    private fun RangeMapping.toSMAP(fileId: Int): String =
        if (range == 1) "$source#$fileId:$dest\n" else "$source#$fileId,$range:$dest\n"

    private fun FileMapping.toSMAPFile(id: Int): String =
        "+ $id $name\n$path\n"

    private fun FileMapping.toSMAPMapping(id: Int): String =
        lineMappings.joinToString("") { it.toSMAP(id) }
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
            parent.mapLineNumber(sourceLineNumber, range.parent.name, range.parent.path, range.callSiteMarker)
        else
            parent.mapLineNumber(sourceLineNumber, range.parent.name, range.parent.path)
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
    private var fileMappings: LinkedHashMap<Pair<String, String>, FileMapping> = linkedMapOf()

    var callSiteMarker: CallSiteMarker? = null

    override val resultMappings: List<FileMapping>
        get() = fileMappings.values.toList()

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

    private fun getOrRegisterNewSource(name: String, path: String): FileMapping {
        return fileMappings.getOrPut(name to path) { FileMapping(name, path) }
    }

    //TODO maybe add assertion that linenumber contained in fileMappings
    override fun mapLineNumber(lineNumber: Int): Int = lineNumber

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

    // assuming disjoint line mappings (otherwise binary search can't be used anyway)
    private val intervals = fileMappings.flatMap { it.lineMappings }.sortedBy { it.dest }

    fun findRange(lineNumber: Int): RangeMapping? {
        val index = intervals.binarySearch { if (lineNumber in it) 0 else it.dest - lineNumber }
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

class FileMapping(val name: String, val path: String) {
    val lineMappings = arrayListOf<RangeMapping>()

    fun mapNewLineNumber(source: Int, currentIndex: Int, callSiteMarker: CallSiteMarker?): Int {
        var mapping = lineMappings.lastOrNull()
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
        RangeMapping(source, dest, range, callSiteMarker, parent = this).also { lineMappings.add(it) }

    companion object {
        val SKIP = FileMapping("no-source-info", "no-source-info").apply {
            mapNewInterval(-1, -1, 1)
        }
    }
}

data class RangeMapping(
    val source: Int, val dest: Int, var range: Int, val callSiteMarker: CallSiteMarker?,
    val parent: FileMapping
) {
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
}

val RangeMapping.toRange: IntRange
    get() = this.dest..this.maxDest
