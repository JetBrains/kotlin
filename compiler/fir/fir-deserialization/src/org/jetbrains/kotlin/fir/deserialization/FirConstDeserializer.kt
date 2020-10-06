/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirConstKind
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

class FirConstDeserializer(
    val session: FirSession,
    private val partSource: KotlinJvmBinaryClass? = null,
    private val facadeSource: KotlinJvmBinaryClass? = null
) {
    companion object {
        private val constantCache = mutableMapOf<CallableId, FirExpression>()
    }

    fun loadConstant(propertyProto: ProtoBuf.Property, callableId: CallableId, nameResolver: NameResolver): FirExpression? {
        if (!Flags.HAS_CONSTANT.get(propertyProto.flags)) return null

        constantCache[callableId]?.let { return it }

        if (facadeSource == null && partSource == null) {
            val value = propertyProto.getExtensionOrNull(BuiltInSerializerProtocol.compileTimeValue) ?: return null
            return buildFirConstant(value, null, value.type.name, nameResolver)?.apply { constantCache[callableId] = this }
        }

        (facadeSource ?: partSource)!!.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
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

    private fun buildFirConstant(
        protoValue: ProtoBuf.Annotation.Argument.Value?, sourceValue: Any?, constKind: String, nameResolver: NameResolver
    ): FirExpression? {
        return when (constKind) {
            "BYTE", "B" -> buildConstExpression(null, FirConstKind.Byte, ((protoValue?.intValue ?: sourceValue) as Number).toByte())
            "CHAR", "C" -> buildConstExpression(null, FirConstKind.Char, ((protoValue?.intValue ?: sourceValue) as Number).toChar())
            "SHORT", "S" -> buildConstExpression(null, FirConstKind.Short, ((protoValue?.intValue ?: sourceValue) as Number).toShort())
            "INT", "I" -> buildConstExpression(null, FirConstKind.Int, protoValue?.intValue?.toInt() ?: sourceValue as Int)
            "LONG", "J" -> buildConstExpression(null, FirConstKind.Long, (protoValue?.intValue ?: sourceValue) as Long)
            "FLOAT", "F" -> buildConstExpression(null, FirConstKind.Float, (protoValue?.floatValue ?: sourceValue) as Float)
            "DOUBLE", "D" -> buildConstExpression(null, FirConstKind.Double, (protoValue?.doubleValue ?: sourceValue) as Double)
            "BOOLEAN", "Z" -> buildConstExpression(null, FirConstKind.Boolean, (protoValue?.intValue?.toInt() ?: sourceValue) != 0)
            "STRING", "Ljava/lang/String" -> buildConstExpression(
                null, FirConstKind.String, protoValue?.stringValue?.let { nameResolver.getString(it) } ?: sourceValue as String
            )
            else -> null
        }
    }

    private fun CallableId.replaceName(newName: Name): CallableId {
        return CallableId(this.packageName, this.className, newName)
    }
}