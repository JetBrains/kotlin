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

open class LineNumberToMap(val lineNumberToMap: Int, val source: Int, val name: String, val path: String)

open class NestedSourceMapper(
        override val parent: SourceMapper, val ranges: List<RangeMapping>, sourceInfo: SourceInfo
) : DefaultSourceMapper(sourceInfo) {

    val visited = TIntIntHashMap()

    override fun visitLineNumber(iv: MethodVisitor, lineNumber: Int, start: Label) {
        val mappedLineNumber = visited.get(lineNumber)

        if (mappedLineNumber > 0) {
            iv.visitLineNumber(mappedLineNumber, start)
        } else {
            val findMappingIfExists = findMappingIfExists(lineNumber)!!
            val sourceLineNumber = findMappingIfExists.mapDestToSource(lineNumber)
            val visitLineNumber = parent.visitLineNumber(iv, lineNumber, start, LineNumberToMap(lineNumber, sourceLineNumber, findMappingIfExists.parent!!.name, findMappingIfExists.parent!!.path))
            if (visitLineNumber > 0) {
                visited.put(lineNumber, visitLineNumber)
            }
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
            "Mapping ragnes should be present in inlined lambda: ${smap.node}"
        }
    }

    override fun visitLineNumber(iv: MethodVisitor, lineNumber: Int, start: Label) {
        val mapping = findMappingIfExists(lineNumber)!!
        if (mapping == ranges.firstOrNull()) { //TODO rewrite
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

    override fun visitLineNumber(iv: MethodVisitor, destLineNumber: Int, start: Label, source: LineNumberToMap): Int {
        if (source.source < 0) {
            //no source information, so just skip this linenumber
            return -1
        }
        visitSource(source.name, source.path)
        val mappedLineIndex = createMapping(source.source)
        iv.visitLineNumber(mappedLineIndex, start)
        return mappedLineIndex

    }

    protected fun createMapping(lineNumber: Int): Int {
        val fileMapping = lastVisited!!
        //val mappedLineIndex = fileMapping.mapLine(lineNumber, maxUsedValue, lastMappedWithChanges == lastVisited)
        val mappedLineIndex = fileMapping.mapNewLineNumber(lineNumber, maxUsedValue, lastMappedWithChanges == lastVisited)
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

    fun mapNewLineNumber(source: Int, currentIndex: Int, isLastMapped: Boolean): Int {
        val dest: Int
        val rangeMapping: RangeMapping
        if (rangeMappings.isNotEmpty() && isLastMapped && couldFoldInRange(lastMappedWithNewIndex, source)) {
            rangeMapping = rangeMappings.last()
            rangeMapping.range += source - lastMappedWithNewIndex
            dest = rangeMapping.mapSourceToDest(source)
        }
        else {
            dest = currentIndex + 1
            rangeMapping = RangeMapping(source, dest)
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

