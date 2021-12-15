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
    ArrayList<Record>().apply {
        val records = types.recordList
        this.ensureCapacity(records.size)
        for (record in records) {
            repeat(record.range) {
                this.add(record)
            }
        }
        this.trimToSize()
    }
)
