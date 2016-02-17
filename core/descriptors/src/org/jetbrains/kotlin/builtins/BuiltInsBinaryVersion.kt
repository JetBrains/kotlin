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

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion
import java.io.DataInputStream
import java.io.InputStream

/**
 * The version of the format in which the .kotlin_builtins file is stored. This version also includes the version
 * of the core protobuf messages (descriptors.proto).
 */
class BuiltInsBinaryVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
    override fun isCompatible() = this.isCompatibleTo(INSTANCE)

    companion object {
        @JvmField
        val INSTANCE = BuiltInsBinaryVersion(1, 0, 0)

        fun readFrom(stream: InputStream): BuiltInsBinaryVersion {
            val dataInput = DataInputStream(stream)
            return BuiltInsBinaryVersion(*(1..dataInput.readInt()).map { dataInput.readInt() }.toIntArray())
        }
    }
}
