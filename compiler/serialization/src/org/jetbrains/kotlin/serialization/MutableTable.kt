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

@file:Suppress("FINITE_BOUNDS_VIOLATION_IN_JAVA")
package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.utils.Interner
import java.util.*

private class TableElementWrapper<Element : GeneratedMessageLite.Builder<*, Element>>(val builder: Element) {
    // If you'll try to optimize it using structured equals/hashCode, pay attention to extensions present in proto messages
    private val bytes: ByteArray = builder.build().toByteArray()
    private val hashCode: Int = Arrays.hashCode(bytes)

    override fun hashCode() = hashCode

    override fun equals(other: Any?) = other is TableElementWrapper<*> && Arrays.equals(bytes, other.bytes)
}

abstract class MutableTable<Element, Table, TableBuilder>
    where Element : GeneratedMessageLite.Builder<*, Element>,
          Table : GeneratedMessageLite,
          TableBuilder : GeneratedMessageLite.Builder<Table, TableBuilder> {

    private val interner = Interner<TableElementWrapper<Element>>()

    protected abstract fun createTableBuilder(): TableBuilder

    protected abstract fun addElement(builder: TableBuilder, element: Element)

    operator fun get(type: Element): Int =
            interner.intern(TableElementWrapper(type))

    @Suppress("UNCHECKED_CAST")
    fun serialize(): Table? =
            if (interner.isEmpty) null
            else createTableBuilder().apply {
                for (obj in interner.allInternedObjects) {
                    addElement(this, obj.builder)
                }
            }.build() as Table
}

class MutableTypeTable : MutableTable<ProtoBuf.Type.Builder, ProtoBuf.TypeTable, ProtoBuf.TypeTable.Builder>() {
    override fun createTableBuilder(): ProtoBuf.TypeTable.Builder = ProtoBuf.TypeTable.newBuilder()

    override fun addElement(builder: ProtoBuf.TypeTable.Builder, element: ProtoBuf.Type.Builder) {
        builder.addType(element)
    }
}

class MutableSinceKotlinInfoTable : MutableTable<ProtoBuf.SinceKotlinInfo.Builder, ProtoBuf.SinceKotlinInfoTable, ProtoBuf.SinceKotlinInfoTable.Builder>() {
    override fun createTableBuilder(): ProtoBuf.SinceKotlinInfoTable.Builder = ProtoBuf.SinceKotlinInfoTable.newBuilder()

    override fun addElement(builder: ProtoBuf.SinceKotlinInfoTable.Builder, element: ProtoBuf.SinceKotlinInfo.Builder) {
        builder.addInfo(element)
    }
}
