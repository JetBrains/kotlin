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

import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion

/**
 * The version of the metadata serialized by the compiler and deserialized by the compiler and reflection.
 * This version includes the version of the core protobuf messages (descriptors.proto) as well as JVM extensions (jvm_descriptors.proto).
 */
class JvmMetadataVersion protected constructor(
        major: Int, minor: Int, patch: Int, rest: List<Int>
) : BinaryVersion(major, minor, patch, rest) {
    override fun isCompatible() = this.isCompatibleTo(INSTANCE)

    companion object {
        @JvmField
        val INSTANCE = create(1, 0, 2)

        @JvmField
        val INVALID_VERSION = JvmMetadataVersion.create(IntArray(0))

        @JvmStatic
        fun create(version: IntArray) = create(version, ::JvmMetadataVersion)

        @JvmStatic
        fun create(major: Int, minor: Int, patch: Int) = create(major, minor, patch, ::JvmMetadataVersion)
    }
}
