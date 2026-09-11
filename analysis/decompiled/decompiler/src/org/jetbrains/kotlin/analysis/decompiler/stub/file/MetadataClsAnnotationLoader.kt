/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.file

import org.jetbrains.kotlin.analysis.decompiler.stub.AnnotationWithArgs
import org.jetbrains.kotlin.analysis.decompiler.stub.ClsAnnotationLoader
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.psi.stubs.impl.createConstantValue
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.AbstractAnnotationLoader
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName

internal class MetadataClsAnnotationLoader(
    protocol: SerializerExtensionProtocol,
) : AbstractAnnotationLoader<AnnotationWithArgs>(protocol), ClsAnnotationLoader {
    override fun loadAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): AnnotationWithArgs {
        val valueMap = proto.argumentList.associate { nameResolver.getName(it.nameId) to createConstantValue(it.value, nameResolver) }
        return AnnotationWithArgs(nameResolver.getClassId(proto.id), valueMap)
    }

    override fun loadPropertyInitializer(container: ProtoContainer, proto: ProtoBuf.Property): ConstantValue<*>? {
        val value = proto.getExtensionOrNull(protocol.compileTimeValue) ?: return null
        return createConstantValue(value, container.nameResolver)
    }
}
