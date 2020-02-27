/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

sealed class ProtoContainer(
    val sourceElement: SourceElement?
) {
    class Class(
        val classProto: ProtoBuf.Class,
        val outerClass: Class?,
        val classId: ClassId,
        sourceElement: SourceElement?
    ) : ProtoContainer(sourceElement) {
        val kind: ProtoBuf.Class.Kind = Flags.CLASS_KIND.get(classProto.flags) ?: ProtoBuf.Class.Kind
            .CLASS
        val isInner: Boolean = Flags.IS_INNER.get(classProto.flags)

        override fun debugFqName(): FqName = classId.asSingleFqName()
    }

    class Package(
        val fqName: FqName,
        sourceElement: SourceElement?
    ) : ProtoContainer(sourceElement) {
        override fun debugFqName(): FqName = fqName
    }

    abstract fun debugFqName(): FqName

    override fun toString() = "${this::class.java.simpleName}: ${debugFqName()}"
}