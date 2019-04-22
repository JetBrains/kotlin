/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.AbstractAnnotationDeserializer
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver

class JvmBinaryAnnotationDeserializer(
    session: FirSession, nameResolver: JvmNameResolver
) : AbstractAnnotationDeserializer(session, nameResolver) {
    override fun loadTypeAnnotations(typeProto: ProtoBuf.Type): List<FirAnnotationCall> {
        val annotations = typeProto.getExtension(JvmProtoBuf.typeAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it) }
    }
}