/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.AbstractAnnotationDeserializer
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

class KlibBasedAnnotationDeserializer(
    session: FirSession
) : AbstractAnnotationDeserializer(session) {
    override val protocol: SerializerExtensionProtocol
        get() = KlibMetadataSerializerProtocol

    override fun loadTypeAnnotations(typeProto: ProtoBuf.Type, nameResolver: NameResolver): List<FirAnnotation> {
        val annotations = typeProto.getExtension(KlibMetadataProtoBuf.typeAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }
}