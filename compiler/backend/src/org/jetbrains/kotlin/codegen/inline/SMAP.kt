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

import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Label
import java.util
import java.util.HashSet
import java.util.Arrays

public class SMAPBuilder(val source: String,
                         val path: String,
                         val fileMappings: List<FileMapping>,
                         val lineCountInOriginalFile: Int) {

    val header = "SMAP\n$source\nKotlin\n*S Kotlin"

    fun build(): String? {
        if (fileMappings.isEmpty()) {
            return null;
        }


        val defaultSourceMapping = RawFileMapping(source, path)
        for(i in 1..lineCountInOriginalFile) {
            defaultSourceMapping.mapLine(i, i - 1, true)
        }
        val allMappings = arrayListOf(defaultSourceMapping.toFileMapping())
        allMappings.addAll(fileMappings)

        var id = 1;

        val fileIds = "*F" +
                      allMappings.fold("") {(a, e) ->
                          a + "\n${e.toSMAPFile(id++)}"
                      }

        val fileMappings = "*L" +
                      allMappings.fold("") {(a, e) ->
                          a + "${e.toSMAPMapping()}"
                      }

        return header + "\n" + fileIds +"\n" + fileMappings + "\n*E\n"
    }

    fun RangeMapping.toSMAP(fileId: Int): String {
        return if (range == 1) "$source#$fileId:$dest" else "$source#$fileId,$range:$dest"
    }

    fun FileMapping.toSMAPFile(id: Int): String {
        this.id = id;
        return "+ $id $name\n$path"
    }

    fun FileMapping.toSMAPMapping(): String {
        return lineMappings.fold("") {
            (a, e) ->
            "$a\n${e.toSMAP(id)}"
        }
    }
}

public object IdenticalSourceMapper: SourceMapper(-1) {

    override fun visitSource(name: String, path: String) {

    }

    override fun visitLineNumber(iv: MethodVisitor, lineNumber: Int, start: Label) {
        iv.visitLineNumber(lineNumber, start)
    }
}

public open class SourceMapper(val lineNumbers: Int) {

    private var maxUsedValue = lineNumbers;

    private var lastMappedWithChanges: RawFileMapping? = null;

    var fileMapping: MutableList<RawFileMapping> = arrayListOf();

    open fun visitSource(name: String, path: String) {
        fileMapping.add(RawFileMapping(name, path))
    }

    open fun visitLineNumber(iv: MethodVisitor, lineNumber: Int, start: Label) {
        val fileMapping = fileMapping.last()
        val mappedLineIndex = fileMapping.mapLine(lineNumber, maxUsedValue, true)
        iv.visitLineNumber(mappedLineIndex, start)
        if (mappedLineIndex > maxUsedValue) {
            lastMappedWithChanges = fileMapping
            maxUsedValue = mappedLineIndex
        }
    }

}
/*Source Mapping*/
class SMAP(fileMappings: List<FileMapping>) {
    var fileMappings: List<FileMapping> = fileMappings

    val intervals = fileMappings.flatMap { it -> it.lineMappings }.sortBy(RangeMapping.Comparator)

    class object {
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
        var dest = lineMappings.get(source);
        if (dest == null) {
            val rangeMapping: RangeMapping
            if (rangeMappings.isNotEmpty() && isLastMapped && couldFoldInRange(lastMappedWithNewIndex, source)) {
                rangeMapping = rangeMappings.last()
                rangeMapping.range += source - lastMappedWithNewIndex;
                dest = lineMappings.get(lastMappedWithNewIndex) + source - lastMappedWithNewIndex;
            } else {
                dest = currentIndex + 1;
                rangeMapping = RangeMapping(source, dest)
                rangeMappings.add(rangeMapping)
            }

            lineMappings.put(source, dest)
            lastMappedWithNewIndex = source;
        }

        return dest
    }

    private fun couldFoldInRange(first: Int, second: Int): Boolean {
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

public class RangeMapping(val source: Int, val dest: Int, var range: Int = 1) {

    var parent: FileMapping? = null;

    fun contains(destLine: Int): Boolean {
        return dest <= destLine && destLine < dest + range
    }

    fun map(destLine: Int): Int {
        return source + destLine - dest
    }

    object Comparator : util.Comparator<RangeMapping> {
        override fun compare(o1: RangeMapping, o2: RangeMapping): Int {
            if (o1 == o2) return 0

            val res = o1.dest - o2.dest
            if (res == 0) {
                return o1.range - o2.range
            } else {
                return res
            }
        }
    }
}