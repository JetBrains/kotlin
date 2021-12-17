/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.Name

class FirJvmConstDeserializer(
    session: FirSession,
    private val binaryClass: KotlinJvmBinaryClass,
) : FirConstDeserializer(session) {
    override fun loadConstant(propertyProto: ProtoBuf.Property, callableId: CallableId, nameResolver: NameResolver): FirExpression? {
        if (!Flags.HAS_CONSTANT.get(propertyProto.flags)) return null
        constantCache[callableId]?.let { return it }

        binaryClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
            override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor? = null

            override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor? {
                if (initializer != null) {
                    val constant = buildFirConstant(null, initializer, desc, nameResolver)
                    constant?.let { constantCache[callableId.replaceName(name)] = it }
                }
                return null
            }
        }, null)

        return constantCache[callableId]
    }
}