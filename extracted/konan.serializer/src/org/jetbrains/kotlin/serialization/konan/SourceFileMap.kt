/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.KonanResolvedLibrary
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragment

private class DeserializedSourceFile(
        val name_: String, val index: Int, val library: KonanLibrary) : SourceFile {
    override fun getName(): String? = name_

    override fun equals(other: Any?): Boolean {
        return other is DeserializedSourceFile && library == other.library && index == other.index
    }

    override fun hashCode(): Int {
        return library.hashCode() xor index
    }
}

class SourceFileMap {
    private val sourceToIndex = mutableMapOf<SourceFile, Int>()
    private val indexToSource = mutableMapOf<Int, SourceFile>()

    fun assign(file: SourceFile): Int {
        return sourceToIndex.getOrPut(file) {
            sourceToIndex.size
        }
    }

    fun provide(fileName: String, index: Int, library: KonanLibrary) {
        assert(indexToSource[index] == null)
        indexToSource[index] = DeserializedSourceFile(fileName, index, library)
    }

    fun sourceFile(index: Int): SourceFile =
            indexToSource[index] ?: throw Error("Unknown file for $index")

    fun filesAndClear() =
        sourceToIndex.keys.sortedBy {
            sourceToIndex[it]
        }.also{
            clear()
        }


    fun clear() {
        sourceToIndex.clear()
        indexToSource.clear()
    }
}