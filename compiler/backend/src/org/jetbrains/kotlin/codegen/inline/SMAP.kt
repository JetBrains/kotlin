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

class SourceMapCopier(val parent: SourceMapper, private val smap: SMAP, private val keepCallSites: Boolean = false) {
    private val visitedLines = TIntIntHashMap()
    private var lastVisitedRange: RangeMapping? = null

    fun mapLineNumber(lineNumber: Int): Int {
        if (lineNumber in JvmAbi.SYNTHETIC_MARKER_LINE_NUMBERS) {
            return lineNumber
        }

        val mappedLineNumber = visitedLines.get(lineNumber)
        if (mappedLineNumber > 0) {
            return mappedLineNumber
        }

        val range = lastVisitedRange?.takeIf { lineNumber in it }
            ?: smap.findRange(lineNumber)
            ?: error("Can't find range to map line $lineNumber in ${smap.sourceInfo.source}: ${smap.sourceInfo.pathOrCleanFQN}")
        val sourceLineNumber = range.mapDestToSource(lineNumber)
        if (sourceLineNumber < 0) {
            return -1
        }
        val callSiteMarker = if (keepCallSites) range.callSiteMarker else parent.callSiteMarker
        val newLineNumber = parent.mapLineNumber(sourceLineNumber, range.parent.name, range.parent.path, callSiteMarker)
        visitedLines.put(lineNumber, newLineNumber)
        lastVisitedRange = range
        return newLineNumber
    }
}

data class CallSiteMarker(val lineNumber: Int)

class SourceMapper(val sourceInfo: SourceInfo?) {
    private var maxUsedValue: Int = sourceInfo?.linesInFile ?: 0
    private var fileMappings: LinkedHashMap<Pair<String, String>, FileMapping> = linkedMapOf()

    var callSiteMarker: CallSiteMarker? = null

    val resultMappings: List<FileMapping>
        get() = fileMappings.values.toList()

    init {
        sourceInfo?.let {
            // Explicitly map the file to itself -- we'll probably need a lot of lines from it, so this will produce less ranges.
            getOrRegisterNewSource(it.source, it.pathOrCleanFQN).mapNewInterval(1, 1, it.linesInFile)
        }
    }

    private fun getOrRegisterNewSource(name: String, path: String): FileMapping =
        fileMappings.getOrPut(name to path) { FileMapping(name, path) }

    fun mapLineNumber(source: Int, sourceName: String, sourcePath: String, callSiteMarker: CallSiteMarker?): Int {
        val fileMapping = getOrRegisterNewSource(sourceName, sourcePath)
        val mappedLineIndex = fileMapping.mapNewLineNumber(source, maxUsedValue, callSiteMarker)
        maxUsedValue = max(maxUsedValue, mappedLineIndex)
        return mappedLineIndex
    }
}

private fun FileMapping.toSourceInfo(): SourceInfo =
    SourceInfo(name, path, lineMappings.fold(0) { result, mapping -> max(result, mapping.source + mapping.range - 1) })

class SMAP(val fileMappings: List<FileMapping>) {
    val sourceInfo: SourceInfo
        get() = fileMappings.firstOrNull()?.toSourceInfo() ?: throw AssertionError("no files mapped")

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
        // Save some space in the SMAP by reusing (or extending if it's the last one) the existing range.
        // TODO some *other* range may already cover `source`; probably too slow to check them all though.
        //   Maybe keep the list ordered by `source` and use binary search to locate the closest range on the left?
        val mapping = lineMappings.lastOrNull()?.takeIf { it.canReuseFor(source, currentIndex, callSiteMarker) }
            ?: lineMappings.firstOrNull()?.takeIf { it.canReuseFor(source, currentIndex, callSiteMarker) }
            ?: mapNewInterval(source, currentIndex + 1, 1, callSiteMarker)
        mapping.range = max(mapping.range, source - mapping.source + 1)
        return mapping.mapSourceToDest(source)
    }

    private fun RangeMapping.canReuseFor(newSource: Int, globalMaxDest: Int, newCallSite: CallSiteMarker?): Boolean =
        callSiteMarker == newCallSite && (newSource - source) in 0 until range + (if (maxDest == globalMaxDest) 10 else 0)

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
