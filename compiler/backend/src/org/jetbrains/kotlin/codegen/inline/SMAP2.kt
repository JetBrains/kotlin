/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.inline2

import gnu.trove.TIntIntHashMap
import org.jetbrains.kotlin.codegen.SourceInfo
import org.jetbrains.kotlin.codegen.inline.FileMapping
import org.jetbrains.kotlin.codegen.inline.RangeMapping
import org.jetbrains.kotlin.codegen.inline.SMAPAndMethodNode
import org.jetbrains.kotlin.codegen.inline.SourceMapper
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import java.util.*


class CallSiteMarker(val lineNumber: Int)

open class NestedSourceMapper(
        override val parent: SourceMapper, val ranges: List<RangeMapping>, sourceInfo: SourceInfo
) : DefaultSourceMapper(sourceInfo) {

    val visitedLines = TIntIntHashMap()

    var lastVisitedRange: RangeMapping? = null

    override fun visitLineNumber(iv: MethodVisitor, lineNumber: Int, start: Label) {
        val mappedLineNumber = visitedLines.get(lineNumber)

        if (mappedLineNumber > 0) {
            iv.visitLineNumber(mappedLineNumber, start)
        } else {
            val rangeForMapping = if (lastVisitedRange?.contains(lineNumber) ?: false) lastVisitedRange!! else findMappingIfExists(lineNumber)!!
            val sourceLineNumber = rangeForMapping.mapDestToSource(lineNumber)
            val visitLineNumber = parent.visitLineNumber(iv, start, sourceLineNumber, rangeForMapping.parent!!.name, rangeForMapping.parent!!.path)
            if (visitLineNumber > 0) {
                visitedLines.put(lineNumber, visitLineNumber)
            }
            lastVisitedRange = rangeForMapping
        }
    }

    fun findMappingIfExists(lineNumber: Int): RangeMapping? {
        val index = ranges.binarySearch(RangeMapping(lineNumber, lineNumber, 1), Comparator {
            value, key ->
            if (key.dest in value) 0 else RangeMapping.Comparator.compare(value, key)
        })
        return if (index < 0) null else ranges[index];
    }
}

open class InlineLambdaSourceMapper(
        parent: SourceMapper, smap: SMAPAndMethodNode
) : NestedSourceMapper(parent, smap.ranges, smap.classSMAP.sourceInfo) {

    init {
        assert(smap.ranges.isNotEmpty()) {
            "Mapping ranges should be presented in inline lambda: ${smap.node}"
        }
    }

    override fun visitLineNumber(iv: MethodVisitor, lineNumber: Int, start: Label) {
        if (ranges.firstOrNull()?.contains(lineNumber) ?: false) {
            //don't remap origin lambda line numbers
            iv.visitLineNumber(lineNumber, start)
        }
        else {
            super.visitLineNumber(iv, lineNumber, start)
        }
    }
}


open class DefaultSourceMapper @JvmOverloads constructor(
        val sourceInfo: SourceInfo,
        protected var maxUsedValue: Int = sourceInfo.linesInFile
) : SourceMapper {

    var callSiteMarker: CallSiteMarker? = null;
        set(value) {
            lastMappedWithChanges = null
            field = value
        }

    var lastVisited: RawFileMapping? = null
    private var lastMappedWithChanges: RawFileMapping? = null
    private var fileMappings: LinkedHashMap<String, RawFileMapping> = linkedMapOf()
    protected val origin: RawFileMapping

    init {
        val name = sourceInfo.source
        val path = sourceInfo.pathOrCleanFQN
        origin = RawFileMapping(name, path)
        origin.initRange(1, sourceInfo.linesInFile)
        fileMappings.put(createKey(name, path), origin)
        lastVisited = origin
    }

    private fun createKey(name: String, path: String) = "$name#$path"

    override val resultMappings: List<FileMapping>
        get() = fileMappings.values.map { it.toFileMapping() }

    override fun visitSource(name: String, path: String) {
        lastVisited = fileMappings.getOrPut(createKey(name, path)) { RawFileMapping(name, path) }
    }

    override fun visitOrigin() {
        lastVisited = origin
    }

    override fun visitLineNumber(iv: MethodVisitor, lineNumber: Int, start: Label) {
        if (lineNumber < 0) {
            //no source information, so just skip this linenumber
            return
        }
        //TODO add assertion that mapping exists
        //val sourceLineNumber = createMapping(lineNumberToMap)
        val sourceLineNumber = lineNumber
        assert(lineNumber == sourceLineNumber)
        iv.visitLineNumber(lineNumber, start)
    }

    override fun visitLineNumber(iv: MethodVisitor, start: Label, source: Int, sourceName: String, sourcePath: String): Int {
        if (source < 0) {
            //no source information, so just skip this linenumber
            return -1
        }
        visitSource(sourceName, sourcePath)
        val mappedLineIndex = createMapping(source)
        iv.visitLineNumber(mappedLineIndex, start)
        return mappedLineIndex

    }

    protected fun createMapping(lineNumber: Int): Int {
        val fileMapping = lastVisited!!
        val mappedLineIndex = fileMapping.mapNewLineNumber(lineNumber, maxUsedValue, lastMappedWithChanges == lastVisited, callSiteMarker)
        if (mappedLineIndex > maxUsedValue) {
            lastMappedWithChanges = fileMapping
            maxUsedValue = mappedLineIndex
        }
        return mappedLineIndex
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

