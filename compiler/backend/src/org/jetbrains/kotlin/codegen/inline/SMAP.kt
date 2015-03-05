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

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Label
import java.util
import java.util.LinkedHashMap
import java.util.Collections
import java.util.ArrayList
import org.jetbrains.kotlin.codegen.SourceInfo
import org.jetbrains.kotlin.codegen.ClassBuilder

//TODO join parameter
public class SMAPBuilder(val source: String,
                         val path: String,
                         val fileMappings: List<FileMapping>) {

    val header = "SMAP\n$source\nKotlin\n*S Kotlin"

    fun build(): String? {
        if (fileMappings.isEmpty()) {
            return null;
        }

        val fileIds = "*F" + fileMappings.mapIndexed {(id, file) -> "\n${file.toSMAPFile(id + 1)}" }.join("")
        val lineMappings = "*L" + fileMappings.map { it.toSMAPMapping() }.join("")

        return "$header\n$fileIds\n$lineMappings\n*E\n"
    }


    fun RangeMapping.toSMAP(fileId: Int): String {
        return if (range == 1) "$source#$fileId:$dest" else "$source#$fileId,$range:$dest"
    }

    fun FileMapping.toSMAPFile(id: Int): String {
        this.id = id;
        return "+ $id $name\n$path"
    }

    //TODO inline
    fun FileMapping.toSMAPMapping(): String {
        return lineMappings.map {
            "\n${it.toSMAP(id)}"
        }.join("")
    }
}

public open class NestedSourceMapper(parent: SourceMapper, val ranges: List<RangeMapping>, sourceInfo: SourceInfo) : DefaultSourceMapper(sourceInfo, parent) {

    override fun visitLineNumber(iv: MethodVisitor, lineNumber: Int, start: Label) {
        val index = Collections.binarySearch(ranges, RangeMapping(lineNumber, lineNumber, 1)) {
            (value, key) ->
            if (value.contains(key.dest)) 0 else RangeMapping.Comparator.compare(value, key)
        }
        if (index < 0) {
            parent!!.visitSource(sourceInfo.source,  sourceInfo.pathOrCleanFQN)
            parent!!.visitLineNumber(iv, lineNumber, start)
        }
        else {
            val rangeMapping = ranges[index]
            parent!!.visitSource(rangeMapping.parent!!.name, rangeMapping.parent!!.path)
            parent!!.visitLineNumber(iv, rangeMapping.map(lineNumber), start)
        }
    }
}

public open class InlineLambdaSourceMapper(parent: SourceMapper, smap: SMAPAndMethodNode) : NestedSourceMapper(parent, smap.ranges, smap.classSMAP.sourceInfo) {

    override fun visitSource(name: String, path: String) {
        super.visitSource(name, path)
        if (isOriginalVisited()) {
            parent!!.visitOrigin()
        }
    }

    override fun visitOrigin() {
        super.visitOrigin()
        parent!!.visitOrigin()
    }

    private fun isOriginalVisited(): Boolean {
        return lastVisited == origin
    }

    override fun visitLineNumber(iv: MethodVisitor, lineNumber: Int, start: Label) {
        val index = Collections.binarySearch(ranges, RangeMapping(lineNumber, lineNumber, 1)) {
            (value, key) ->
            if (value.contains(key.dest)) 0 else RangeMapping.Comparator.compare(value, key)
        }
        if (index >= 0) {
            val fmapping = ranges[index].parent!!
            if (fmapping.path == origin.path && fmapping.name == origin.name) {
                parent!!.visitOrigin()
                parent!!.visitLineNumber(iv, lineNumber, start)
                return
            }
        }
        super.visitLineNumber(iv, lineNumber, start)
    }
}

trait SourceMapper {

    val resultMappings: List<FileMapping>
    val parent: SourceMapper?

    open fun visitSource(name: String, path: String) {
        throw UnsupportedOperationException("fail")
    }

    open fun visitOrigin() {
        throw UnsupportedOperationException("fail")
    }

    open fun visitLineNumber(iv: MethodVisitor, lineNumber: Int, start: Label) {
        throw UnsupportedOperationException("fail")
    }

    open fun endMapping() {
        parent?.visitOrigin()
    }

    default object {

        fun flushToClassBuilder(mapper: SourceMapper, v: ClassBuilder) {
            for (fileMapping in mapper.resultMappings) {
                v.addSMAP(fileMapping)
            }
        }

        fun createFromSmap(smap: SMAP): DefaultSourceMapper {
            val sourceMapper = DefaultSourceMapper(smap.sourceInfo, null)
            smap.fileMappings.stream()
                    //default one mapped through sourceInfo
                    .filterNot { it == smap.default }
                    .forEach { fileMapping ->
                        sourceMapper.visitSource(fileMapping.name, fileMapping.path)
                        fileMapping.lineMappings.forEach {
                            sourceMapper.lastVisited!!.mapNewInterval(it.source, it.dest, it.range)
                        }
                    }

            return sourceMapper
        }
    }
}

public object IdenticalSourceMapper : SourceMapper {
    override val resultMappings: List<FileMapping>
        get() = emptyList()

    override val parent: SourceMapper?
        get() = null

    override fun visitSource(name: String, path: String) {}

    override fun visitOrigin() {}

    override fun visitLineNumber(iv: MethodVisitor, lineNumber: Int, start: Label) {
        iv.visitLineNumber(lineNumber, start)
    }
}

public open class DefaultSourceMapper(val sourceInfo: SourceInfo, override val parent: SourceMapper?): SourceMapper {

    protected var maxUsedValue: Int = sourceInfo.linesInFile

    var lastVisited: RawFileMapping? = null

    private var lastMappedWithChanges: RawFileMapping? = null

    var fileMappings: LinkedHashMap<String, RawFileMapping> = linkedMapOf();

    protected val origin: RawFileMapping
    {
        visitSource(sourceInfo.source, sourceInfo.pathOrCleanFQN)
        origin = lastVisited!!
        //map interval
        (1..maxUsedValue).forEach {origin.mapLine(it, it - 1, true) }
    }

    override val resultMappings: List<FileMapping>
        get() = fileMappings.values().map { it.toFileMapping() }

    override fun visitSource(name: String, path: String) {
        lastVisited = fileMappings.getOrPut("$name#$path", { RawFileMapping(name, path) })
    }

    override fun visitOrigin() {
        lastVisited = origin
    }

    override fun visitLineNumber(iv: MethodVisitor, lineNumber: Int, start: Label) {
        val mappedLineIndex = createMapping(lineNumber)
        iv.visitLineNumber(mappedLineIndex, start)
    }

    protected fun createMapping(lineNumber: Int): Int {
        val fileMapping = lastVisited!!
        val mappedLineIndex = fileMapping.mapLine(lineNumber, maxUsedValue, lastMappedWithChanges == lastVisited)
        if (mappedLineIndex > maxUsedValue) {
            lastMappedWithChanges = fileMapping
            maxUsedValue = mappedLineIndex
        }
        return mappedLineIndex
    }
}

/*Source Mapping*/
class SMAP(val fileMappings: List<FileMapping>) {
    init {
        assert(fileMappings.isNotEmpty(), "File Mappings shouldn't be empty")
    }

    val default: FileMapping
        get() = fileMappings.first()

    val intervals = fileMappings.flatMap { it.lineMappings }.sortBy(RangeMapping.Comparator)

    val sourceInfo: SourceInfo
    init {
        val defaultMapping = default.lineMappings.single()
        sourceInfo = SourceInfo(default.name, default.path, defaultMapping.source + defaultMapping.range - 1)
    }

    default object {
        val FILE_SECTION = "*F"

        val LINE_SECTION = "*L"

        val END = "*E"
    }
}

class RawFileMapping(val name: String, val path: String) {
    private val lineMappings = linkedMapOf<Int, Int>()
    private val rangeMappings = arrayListOf<RangeMapping>()

    private var lastMappedWithNewIndex = -1000;

    fun toFileMapping(): FileMapping {
        val fileMapping = FileMapping(name, path)
        for (range in rangeMappings) {
            fileMapping.addRangeMapping(range)
        }
        return fileMapping
    }

    fun mapLine(source: Int, currentIndex: Int, isLastMapped: Boolean): Int {
        var dest = lineMappings[source]
        if (dest == null) {
            val rangeMapping: RangeMapping
            if (rangeMappings.isNotEmpty() && isLastMapped && couldFoldInRange(lastMappedWithNewIndex, source)) {
                rangeMapping = rangeMappings.last()
                rangeMapping.range += source - lastMappedWithNewIndex
                dest = lineMappings[lastMappedWithNewIndex] + source - lastMappedWithNewIndex
            }
            else {
                dest = currentIndex + 1
                rangeMapping = RangeMapping(source, dest)
                rangeMappings.add(rangeMapping)
            }

            lineMappings.put(source, dest)
            lastMappedWithNewIndex = source
        }

        return dest
    }

    fun mapNewInterval(source: Int, dest: Int, range: Int) {
        val rangeMapping = RangeMapping(source, dest, range)
        rangeMappings.add(rangeMapping)

        (source..(source + range - 1)).forEach {
            lineMappings.put(source, dest)
        }
    }

    private fun couldFoldInRange(first: Int, second: Int): Boolean {
        //TODO
        val delta = second - first
        return delta > 0 && delta <= 10;
    }
}

public class FileMapping(val name: String, val path: String) {
    val lineMappings = arrayListOf<RangeMapping>()

    var id = -1;

    fun addRangeMapping(lineMapping: RangeMapping) {
        lineMappings.add(lineMapping)
        lineMapping.parent = this
    }
}

//TODO comparable
data public class RangeMapping(val source: Int, val dest: Int, var range: Int = 1) {

    var parent: FileMapping? = null;

    fun contains(destLine: Int): Boolean {
        return dest <= destLine && destLine < dest + range
    }

    fun map(destLine: Int): Int {
        return source + (destLine - dest)
    }

    object Comparator : util.Comparator<RangeMapping> {
        override fun compare(o1: RangeMapping, o2: RangeMapping): Int {
            if (o1 == o2) return 0

            val res = o1.dest - o2.dest
            if (res == 0) {
                return o1.range - o2.range
            }
            else {
                return res
            }
        }
    }
}