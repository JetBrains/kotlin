/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet
import org.jetbrains.kotlin.name.Name
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl

/**
 * The interface provide an API for interning [String], [Name] and [NaiveSourceBasedFileEntryImpl] values
 * to save memory by eliminating duplicates of instances of those classes
 */
class IrInterningService {
    /**
     * We use here an open-addressing map, because it consumes at least twice lesser memory than with bucket-based implementation:
     * - Open-addressing (cost per entry): ref to key + ref to value
     * - Bucket-based (cost per entry): hash code + ref to key + ref to value + ref to the next node + class header with memory alignment
     */
    private val strings by lazy { ObjectOpenHashSet<String>() }
    private val names by lazy { ObjectOpenHashSet<Name>() }
    private val fileEntries by lazy { ObjectOpenCustomHashSet(object : Hash.Strategy<IrFileEntry> {
            override fun hashCode(o: IrFileEntry?): Int {
                if (o !is NaiveSourceBasedFileEntryImpl) return o.hashCode()

                var result = super.hashCode()
                result = 31 * result + o.maxOffset
                result = 31 * result + o.firstRelevantLineIndex
                result = 31 * result + o.name.hashCode()
                result = 31 * result + o.lineStartOffsets.contentHashCode()
                return result
            }

            override fun equals(a: IrFileEntry?, b: IrFileEntry?): Boolean {
                if (a === b) return true
                if (a == null || b == null) return false
                if (a !is NaiveSourceBasedFileEntryImpl || b !is NaiveSourceBasedFileEntryImpl) return a == b

                if (a.maxOffset != b.maxOffset) return false
                if (a.firstRelevantLineIndex != b.firstRelevantLineIndex) return false
                if (a.name != b.name) return false
                if (!a.lineStartOffsets.contentEquals(b.lineStartOffsets)) return false

                return true
            }
        })
    }

    fun string(string: String): String {
        return strings.addOrGet(string)
    }

    fun name(name: Name): Name {
        return names.addOrGet(name)
    }

    fun fileEntry(file: IrFileEntry): IrFileEntry {
        return fileEntries.addOrGet(file)
    }

    /**
     * Clean up internal interner caches. After calling this function the interner will 'forget' all previously
     * seen instances and will start the deduplication from scratch.
     */
    fun reset() {
        strings.clear()
        names.clear()
        fileEntries.clear()
    }
}