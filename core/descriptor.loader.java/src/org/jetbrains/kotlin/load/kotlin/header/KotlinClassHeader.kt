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

package org.jetbrains.kotlin.load.kotlin.header

import org.jetbrains.kotlin.load.java.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion

class KotlinClassHeader(
        val kind: KotlinClassHeader.Kind,
        val metadataVersion: JvmMetadataVersion,
        val bytecodeVersion: JvmBytecodeBinaryVersion,
        val data: Array<String>?,
        val strings: Array<String>?,
        val extraString: String?,
        val extraInt: Int
) {
    // See kotlin.Metadata
    enum class Kind(val id: Int) {
        UNKNOWN(0),
        CLASS(1),
        FILE_FACADE(2),
        SYNTHETIC_CLASS(3),
        MULTIFILE_CLASS(4),
        MULTIFILE_CLASS_PART(5);

        companion object {
            private val entryById = values().associateBy(Kind::id)

            @JvmStatic
            fun getById(id: Int) = entryById[id] ?: UNKNOWN
        }
    }

    // See kotlin.Metadata
    enum class MultifileClassKind(val id: Int) {
        DELEGATING(0),
        INHERITING(1);

        companion object {
            private val entryById = values().associateBy(MultifileClassKind::id)

            @JvmStatic
            fun getById(id: Int) = entryById[id]
        }
    }

    val multifileClassName: String?
        get() = if (kind == Kind.MULTIFILE_CLASS_PART) extraString else null

    val multifileClassKind: MultifileClassKind?
        get() = if (kind == Kind.MULTIFILE_CLASS || kind == Kind.MULTIFILE_CLASS_PART)
            MultifileClassKind.getById(extraInt)
        else
            null

    override fun toString() = "$kind version=$metadataVersion"
}
