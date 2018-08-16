/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FINITE_BOUNDS_VIOLATION_IN_JAVA")

package org.jetbrains.kotlin.metadata.serialization

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite

private class TableElementWrapper<Element : GeneratedMessageLite.Builder<*, Element>>(val builder: Element) {
    // If you'll try to optimize it using structured equals/hashCode, pay attention to extensions present in proto messages
    private val bytes: ByteArray = builder.build().toByteArray()
    private val hashCode: Int = bytes.contentHashCode()

    override fun hashCode() = hashCode

    override fun equals(other: Any?) = other is TableElementWrapper<*> && bytes.contentEquals(other.bytes)
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

class MutableVersionRequirementTable :
    MutableTable<ProtoBuf.VersionRequirement.Builder, ProtoBuf.VersionRequirementTable, ProtoBuf.VersionRequirementTable.Builder>() {
    override fun createTableBuilder(): ProtoBuf.VersionRequirementTable.Builder = ProtoBuf.VersionRequirementTable.newBuilder()

    override fun addElement(builder: ProtoBuf.VersionRequirementTable.Builder, element: ProtoBuf.VersionRequirement.Builder) {
        builder.addRequirement(element)
    }
}
