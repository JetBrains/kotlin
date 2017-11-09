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
class JvmMetadataVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
    // NOTE: 1.1 is incompatible with 1.0 and hence with any other version except 1.1.*
    override fun isCompatible() =
            this.major == 1 && this.minor == 1

    companion object {
        @JvmField
        val INSTANCE = JvmMetadataVersion(1, 1, 9)

        @JvmField
        val INVALID_VERSION = JvmMetadataVersion()
    }
}
