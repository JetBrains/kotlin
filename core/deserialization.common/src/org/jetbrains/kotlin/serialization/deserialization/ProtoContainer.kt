/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

sealed class ProtoContainer(
    val nameResolver: NameResolver,
    val typeTable: TypeTable,
    val source: SourceElement?
) {
    class Class(
        val classProto: ProtoBuf.Class,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        source: SourceElement?,
        val outerClass: ProtoContainer.Class?
    ) : ProtoContainer(nameResolver, typeTable, source) {
        val classId: ClassId = nameResolver.getClassId(classProto.fqName)

        val kind: ProtoBuf.Class.Kind = Flags.CLASS_KIND.get(classProto.flags) ?: ProtoBuf.Class.Kind.CLASS
        val isInner: Boolean = Flags.IS_INNER.get(classProto.flags)

        override fun debugFqName(): FqName = classId.asSingleFqName()
    }

    class Package(
        val fqName: FqName,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        source: SourceElement?
    ) : ProtoContainer(nameResolver, typeTable, source) {
        override fun debugFqName(): FqName = fqName
    }

    abstract fun debugFqName(): FqName

    override fun toString() = "${this::class.java.simpleName}: ${debugFqName()}"
}