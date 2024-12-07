/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.file

import org.jetbrains.kotlin.analysis.decompiler.stub.AnnotationWithArgs
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.psi.stubs.impl.createConstantValue
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.*

class AnnotationLoaderForStubBuilderImpl(
    protocol: SerializerExtensionProtocol,
) : AbstractAnnotationLoader<AnnotationWithArgs>(protocol) {
    override fun loadAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): AnnotationWithArgs {
        val valueMap = proto.argumentList.associate { nameResolver.getName(it.nameId) to createConstantValue(it.value, nameResolver) }
        return AnnotationWithArgs(nameResolver.getClassId(proto.id), valueMap)
    }
}
