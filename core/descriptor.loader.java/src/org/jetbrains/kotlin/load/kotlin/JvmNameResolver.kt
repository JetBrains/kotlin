/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.StringTableTypes.Record
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.StringTableTypes.Record.Operation.*
import java.util.*

class JvmNameResolver(
        private val types: JvmProtoBuf.StringTableTypes,
        private val strings: Array<String>
) : NameResolver {
    private val localNameIndices = types.localNameList.run { if (isEmpty()) emptySet() else toSet() }

    // Here we expand the 'range' field of the Record message for simplicity to a list of records
    private val records: List<Record> = ArrayList<Record>().apply {
        val records = types.recordList
        this.ensureCapacity(records.size)
        for (record in records) {
            repeat(record.range) {
                this.add(record)
            }
        }
        this.trimToSize()
    }

    override fun getString(index: Int): String {
        val record = records[index]

        var string = when {
            record.hasString() -> record.string
            record.hasPredefinedIndex() && record.predefinedIndex in PREDEFINED_STRINGS.indices ->
                PREDEFINED_STRINGS[record.predefinedIndex]
            else -> strings[index]
        }

        if (record.substringIndexCount >= 2) {
            val (begin, end) = record.substringIndexList
            if (0 <= begin && begin <= end && end <= string.length) {
                string = string.substring(begin, end)
            }
        }

        if (record.replaceCharCount >= 2) {
            val (from, to) = record.replaceCharList
            string = string.replace(from.toChar(), to.toChar())
        }

        when (record.operation ?: NONE) {
            NONE -> {
                // Do nothing
            }
            INTERNAL_TO_CLASS_ID -> {
                string = string.replace('$', '.')
            }
            DESC_TO_CLASS_ID -> {
                if (string.length >= 2) {
                    string = string.substring(1, string.length - 1)
                }
                string = string.replace('$', '.')
            }
        }

        return string
    }

    override fun getName(index: Int) = Name.guessByFirstCharacter(getString(index))

    override fun getClassId(index: Int): ClassId {
        val string = getString(index)
        val lastSlash = string.lastIndexOf('/')
        val packageName =
                if (lastSlash < 0) FqName.ROOT
                else FqName(string.substring(0, lastSlash).replace('/', '.'))
        val className = FqName(string.substring(lastSlash + 1))
        return ClassId(packageName, className, index in localNameIndices)
    }

    companion object {
        val PREDEFINED_STRINGS = listOf(
                "kotlin/Any",
                "kotlin/Nothing",
                "kotlin/Unit",
                "kotlin/Throwable",
                "kotlin/Number",

                "kotlin/Byte", "kotlin/Double", "kotlin/Float", "kotlin/Int",
                "kotlin/Long", "kotlin/Short", "kotlin/Boolean", "kotlin/Char",

                "kotlin/CharSequence",
                "kotlin/String",
                "kotlin/Comparable",
                "kotlin/Enum",

                "kotlin/Array",
                "kotlin/ByteArray", "kotlin/DoubleArray", "kotlin/FloatArray", "kotlin/IntArray",
                "kotlin/LongArray", "kotlin/ShortArray", "kotlin/BooleanArray", "kotlin/CharArray",

                "kotlin/Cloneable",
                "kotlin/Annotation",

                "kotlin/collections/Iterable", "kotlin/collections/MutableIterable",
                "kotlin/collections/Collection", "kotlin/collections/MutableCollection",
                "kotlin/collections/List", "kotlin/collections/MutableList",
                "kotlin/collections/Set", "kotlin/collections/MutableSet",
                "kotlin/collections/Map", "kotlin/collections/MutableMap",
                "kotlin/collections/Map.Entry", "kotlin/collections/MutableMap.MutableEntry",

                "kotlin/collections/Iterator", "kotlin/collections/MutableIterator",
                "kotlin/collections/ListIterator", "kotlin/collections/MutableListIterator"
        )

        private val PREDEFINED_STRINGS_MAP = PREDEFINED_STRINGS.withIndex().associateBy({ it.value }, { it.index })

        fun getPredefinedStringIndex(string: String): Int? = PREDEFINED_STRINGS_MAP[string]
    }
}
