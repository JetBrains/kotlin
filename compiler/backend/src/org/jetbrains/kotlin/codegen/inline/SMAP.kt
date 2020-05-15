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

        val debugMappings = linkedMapOf<Pair<String, String>, FileMapping>()
        for (fileMapping in realMappings) {
            for ((_, dest, range, callSite) in fileMapping.lineMappings) {
                callSite?.let { (line, file, path) ->
                    debugMappings.getOrPut(file to path) { FileMapping(file, path) }.mapNewInterval(line, dest, range)
                }
            }
        }

        // Old versions of kotlinc and the IDEA plugin have incorrect implementations of SMAPParser:
        //   1. they require *E between strata, which is not correct syntax according to JSR-045;
        //   2. in KotlinDebug, they use `1#2,3:4` to mean "map lines 4..6 to line 1 of #2", when in reality (and in
        //      the non-debug stratum) this maps lines 4..6 to lines 1..3. The correct syntax is `1#2:4,3`.
        val defaultStrata = realMappings.toSMAP(KOTLIN_STRATA_NAME, mapToFirstLine = false)
        val debugStrata = debugMappings.values.toSMAP(KOTLIN_DEBUG_STRATA_NAME, mapToFirstLine = !backwardsCompatibleSyntax)
        if (backwardsCompatibleSyntax && defaultStrata.isNotEmpty() && debugStrata.isNotEmpty()) {
            return "SMAP\n${fileMappings[0].name}\n$KOTLIN_STRATA_NAME\n$defaultStrata${SMAP.END}\n$debugStrata${SMAP.END}\n"
        }
        return "SMAP\n${fileMappings[0].name}\n$KOTLIN_STRATA_NAME\n$defaultStrata$debugStrata${SMAP.END}\n"
    }

    private fun Collection<FileMapping>.toSMAP(stratumName: String, mapToFirstLine: Boolean): String = if (isEmpty()) "" else
        "${SMAP.STRATA_SECTION} $stratumName\n" +
                "${SMAP.FILE_SECTION}\n${mapIndexed { id, file -> file.toSMAPFile(id + 1) }.joinToString("")}" +
                "${SMAP.LINE_SECTION}\n${mapIndexed { id, file -> file.toSMAPMapping(id + 1, mapToFirstLine) }.joinToString("")}"

    private fun RangeMapping.toSMAP(fileId: Int, oneLine: Boolean): String =
        if (range == 1) "$source#$fileId:$dest\n" else if (oneLine) "$source#$fileId:$dest,$range\n" else "$source#$fileId,$range:$dest\n"

    private fun FileMapping.toSMAPFile(id: Int): String =
        "+ $id $name\n$path\n"

    private fun FileMapping.toSMAPMapping(id: Int, mapToFirstLine: Boolean): String =
        lineMappings.joinToString("") { it.toSMAP(id, mapToFirstLine) }
}

class SourceMapCopier(val parent: SourceMapper, private val smap: SMAP, val callSite: SourcePosition? = null) {
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
        val inlineSource = range.mapDestToSource(lineNumber)
        if (inlineSource.line < 0) {
            return -1
        }
        val newLineNumber = parent.mapLineNumber(inlineSource, callSite ?: range.callSite)
        visitedLines.put(lineNumber, newLineNumber)
        lastVisitedRange = range
        return newLineNumber
    }
}

data class SourcePosition(val line: Int, val file: String, val path: String)

class SourceMapper(val sourceInfo: SourceInfo?) {
    private var maxUsedValue: Int = sourceInfo?.linesInFile ?: 0
    private var fileMappings: LinkedHashMap<Pair<String, String>, FileMapping> = linkedMapOf()

    val resultMappings: List<FileMapping>
        get() = fileMappings.values.toList()

    init {
        sourceInfo?.let {
            // Explicitly map the file to itself -- we'll probably need a lot of lines from it, so this will produce fewer ranges.
            getOrRegisterNewSource(it.source, it.pathOrCleanFQN).mapNewInterval(1, 1, it.linesInFile)
        }
    }

    private fun getOrRegisterNewSource(name: String, path: String): FileMapping =
        fileMappings.getOrPut(name to path) { FileMapping(name, path) }

    fun mapLineNumber(inlineSource: SourcePosition, inlineCallSite: SourcePosition?): Int {
        val fileMapping = getOrRegisterNewSource(inlineSource.file, inlineSource.path)
        val mappedLineIndex = fileMapping.mapNewLineNumber(inlineSource.line, maxUsedValue, inlineCallSite)
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

    fun mapNewLineNumber(source: Int, currentIndex: Int, callSite: SourcePosition?): Int {
        // Save some space in the SMAP by reusing (or extending if it's the last one) the existing range.
        // TODO some *other* range may already cover `source`; probably too slow to check them all though.
        //   Maybe keep the list ordered by `source` and use binary search to locate the closest range on the left?
        val mapping = lineMappings.lastOrNull()?.takeIf { it.canReuseFor(source, currentIndex, callSite) }
            ?: lineMappings.firstOrNull()?.takeIf { it.canReuseFor(source, currentIndex, callSite) }
            ?: mapNewInterval(source, currentIndex + 1, 1, callSite)
        mapping.range = max(mapping.range, source - mapping.source + 1)
        return mapping.mapSourceToDest(source)
    }

    private fun RangeMapping.canReuseFor(newSource: Int, globalMaxDest: Int, newCallSite: SourcePosition?): Boolean =
        callSite == newCallSite && (newSource - source) in 0 until range + (if (globalMaxDest in this) 10 else 0)

    fun mapNewInterval(source: Int, dest: Int, range: Int, callSite: SourcePosition? = null): RangeMapping =
        RangeMapping(source, dest, range, callSite, parent = this).also { lineMappings.add(it) }

    companion object {
        val SKIP = FileMapping("no-source-info", "no-source-info").apply {
            mapNewInterval(-1, -1, 1)
        }
    }
}

data class RangeMapping(val source: Int, val dest: Int, var range: Int, val callSite: SourcePosition?, val parent: FileMapping) {
    private val skip = source == -1 && dest == -1

    operator fun contains(destLine: Int): Boolean =
        skip || (dest <= destLine && destLine < dest + range)

    fun hasMappingForSource(sourceLine: Int): Boolean =
        skip || (source <= sourceLine && sourceLine < source + range)

    fun mapDestToSource(destLine: Int): SourcePosition =
        SourcePosition(if (skip) -1 else source + (destLine - dest), parent.name, parent.path)

    fun mapSourceToDest(sourceLine: Int): Int =
        if (skip) -1 else dest + (sourceLine - source)
}

val RangeMapping.toRange: IntRange
    get() = dest until dest + range
