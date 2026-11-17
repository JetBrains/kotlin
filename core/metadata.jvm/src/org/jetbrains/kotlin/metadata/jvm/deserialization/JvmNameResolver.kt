/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.deserialization

import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf.StringTableTypes.Record

class JvmNameResolver(
    val types: JvmProtoBuf.StringTableTypes,
    strings: Array<String>
) : JvmNameResolverBase(
    strings,
    types.localNameList.run { if (isEmpty()) emptySet() else toSet() },
    types.recordList.toExpandedRecordsList()
)

// Here we expand the 'range' field of the Record message for simplicity to a list of records
// Note that as an optimization, range of each expanded record is equal to the original range, not 1. If correct ranges are needed,
// please use the original record representation in [types.recordList].
fun List<Record>.toExpandedRecordsList(): List<Record> =
    ArrayList<Record>().also { list ->
        list.ensureCapacity(size)
        for (record in this) {
            val range = record.range
            if (range > MAX_EXPANDED_RECORDS - list.size) {
                error("String table is too large: more than $MAX_EXPANDED_RECORDS records")
            }
            repeat(range) {
                list.add(record)
            }
        }
        list.trimToSize()
    }

// The compiler writes one string to `kotlin.Metadata.d2` for each expanded record, and an array in an annotation cannot have more elements.
private const val MAX_EXPANDED_RECORDS = 65535
