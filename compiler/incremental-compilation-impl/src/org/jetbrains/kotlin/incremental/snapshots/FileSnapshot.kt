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

/**
 * Represents a snapshot of a file, capturing its length and hash.
 *
 * Does not take into account the path of the file as its intended usage is within [FileSnapshotMap] which has file path as the key.
 */
internal class FileSnapshot(
    val length: Long,
    val hash: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileSnapshot

        if (length != other.length) return false
        if (!hash.contentEquals(other.hash)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = length.hashCode()
        result = 31 * result + hash.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "FileSnapshot(length=$length, hash=${hash.contentToString()})"
    }
}
