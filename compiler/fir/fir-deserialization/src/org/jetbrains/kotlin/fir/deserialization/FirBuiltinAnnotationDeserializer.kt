/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

class FirBuiltinAnnotationDeserializer(
    private val session: FirSession
) : AnnotationDeserializerWithProtocol(session, BuiltInSerializerProtocol) {
    override fun loadTypeAnnotations(typeProto: ProtoBuf.Type, nameResolver: NameResolver): List<FirAnnotation> =
        typeProto.loadAnnotationsFromProtocol(session, protocol.typeAnnotation, nameResolver)

    override fun loadTypeParameterAnnotations(typeParameterProto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<FirAnnotation> =
        emptyList()
}
