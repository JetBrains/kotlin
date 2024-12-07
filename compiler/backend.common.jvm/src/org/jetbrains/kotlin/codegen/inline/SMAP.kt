/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.SourceInfo
import org.jetbrains.kotlin.codegen.asSequence
import org.jetbrains.org.objectweb.asm.tree.LineNumberNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import kotlin.math.max
import kotlin.math.min

const val KOTLIN_STRATA_NAME = "Kotlin"
const val KOTLIN_DEBUG_STRATA_NAME = "KotlinDebug"

/**
 * Represents SMAP as a structure that is contained in `SourceDebugExtension` attribute of a class.
 * This structure is immutable, we can only query for a result.
 */
class SMAP(val fileMappings: List<FileMapping>) {
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

        // Create a mapping that simply maps a range of a file to itself, which is equivalent to having no mapping at all.
        // The contract is: if `smap` is the return value of this method, then `SourceMapCopier(SourceMapper(name, smap), smap)`
        // will not change any line numbers in any of the methods passed as an argument.
        fun identityMapping(name: String?, path: String, methods: Collection<MethodNode>): SMAP {
            if (name.isNullOrEmpty()) return SMAP(emptyList())
            var start = 0
            var end = 0
            for (node in methods) {
                for (insn in node.instructions.asSequence()) {
                    if (insn !is LineNumberNode) continue
                    start = min(start, insn.line)
                    end = max(end, insn.line + 1)
                }
            }
            if (start >= end) return SMAP(emptyList())
            return SMAP(listOf(FileMapping(name, path).apply { mapNewInterval(start, start, end - start) }))
        }
    }
}

class FileMapping(val name: String, val path: String) {
    val lineMappings = arrayListOf<RangeMapping>()

    fun toSourceInfo(): SourceInfo =
        SourceInfo(name, path, lineMappings.fold(0) { result, mapping -> max(result, mapping.source + mapping.range - 1) })

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
}

data class RangeMapping(val source: Int, val dest: Int, var range: Int, val callSite: SourcePosition?, val parent: FileMapping) {
    operator fun contains(destLine: Int): Boolean =
        dest <= destLine && destLine < dest + range

    fun hasMappingForSource(sourceLine: Int): Boolean =
        source <= sourceLine && sourceLine < source + range

    fun mapDestToSource(destLine: Int): SourcePosition =
        SourcePosition(source + (destLine - dest), parent.name, parent.path)

    fun mapSourceToDest(sourceLine: Int): Int =
        dest + (sourceLine - source)
}

val RangeMapping.toRange: IntRange
    get() = dest until dest + range

data class SourcePosition(val line: Int, val file: String, val path: String)
