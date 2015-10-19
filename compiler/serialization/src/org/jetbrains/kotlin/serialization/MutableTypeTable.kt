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

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.utils.Interner
import java.util.*

class MutableTypeTable {
    class TypeWrapper(val type: ProtoBuf.Type.Builder) {
        // If you'll try to optimize it using structured equals/hashCode, pay attention to extensions present in Type messages
        private val bytes: ByteArray = type.build().toByteArray()
        private val hashCode: Int = Arrays.hashCode(bytes)

        override fun hashCode() = hashCode

        override fun equals(other: Any?) = other is TypeWrapper && Arrays.equals(bytes, other.bytes)
    }

    val interner = Interner<TypeWrapper>()

    operator fun get(type: ProtoBuf.Type.Builder): Int =
            interner.intern(TypeWrapper(type))

    fun serialize(): ProtoBuf.TypeTable? =
            if (interner.isEmpty) null
            else ProtoBuf.TypeTable.newBuilder().apply {
                for (type in interner.allInternedObjects) {
                    addType(type.type)
                }
            }.build()
}
