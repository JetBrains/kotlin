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

package org.jetbrains.kotlin.incremental.snapshots

import java.io.File
import java.util.*

class FileSnapshot(
    val file: File,
    val length: Long,
    val hash: ByteArray
) {
    init {
        assert(!file.isDirectory)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false

        other as FileSnapshot

        if (file != other.file) return false
        if (length != other.length) return false
        if (!Arrays.equals(hash, other.hash)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + length.hashCode()
        result = 31 * result + Arrays.hashCode(hash)
        return result
    }

    override fun toString(): String {
        return "FileSnapshot(file=$file, length=$length, hash=${Arrays.toString(hash)})"
    }
}
