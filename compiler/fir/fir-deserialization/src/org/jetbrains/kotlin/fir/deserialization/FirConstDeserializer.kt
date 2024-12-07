/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.types.ConstantValueKind

open class FirConstDeserializer(
    private val protocol: SerializerExtensionProtocol
) {
    protected val constantCache: MutableMap<CallableId, FirExpression> = mutableMapOf()

    open fun loadConstant(
        propertyProto: ProtoBuf.Property, callableId: CallableId, nameResolver: NameResolver, isUnsigned: Boolean,
    ): FirExpression? {
        if (!Flags.HAS_CONSTANT.get(propertyProto.flags)) return null
        constantCache[callableId]?.let { return it }
        val value = propertyProto.getExtensionOrNull(protocol.compileTimeValue) ?: return null
        return buildFirConstant(value, null, value.type.name, nameResolver, isUnsigned)?.also { constantCache[callableId] = it }
    }
}

fun buildFirConstant(
    protoValue: ProtoBuf.Annotation.Argument.Value?, sourceValue: Any?, constKind: String, nameResolver: NameResolver, isUnsigned: Boolean
): FirExpression? {
    return when (constKind) {
        "BYTE", "B" -> buildLiteralExpression(
            null,
            if (isUnsigned) ConstantValueKind.UnsignedByte else ConstantValueKind.Byte,
            ((protoValue?.intValue ?: sourceValue) as Number).toByte(),
            setType = true,
        )
        "SHORT", "S" -> buildLiteralExpression(
            null,
            if (isUnsigned) ConstantValueKind.UnsignedShort else ConstantValueKind.Short,
            ((protoValue?.intValue ?: sourceValue) as Number).toShort(),
            setType = true,
        )
        "INT", "I" -> buildLiteralExpression(
            null,
            if (isUnsigned) ConstantValueKind.UnsignedInt else ConstantValueKind.Int,
            protoValue?.intValue?.toInt() ?: sourceValue as Int,
            setType = true,
        )
        "LONG", "J" -> buildLiteralExpression(
            null,
            if (isUnsigned) ConstantValueKind.UnsignedLong else ConstantValueKind.Long,
            protoValue?.intValue ?: sourceValue as Long,
            setType = true,
        )
        "CHAR", "C" -> buildLiteralExpression(
            null, ConstantValueKind.Char, ((protoValue?.intValue ?: sourceValue) as Number).toInt().toChar(), setType = true
        )
        "FLOAT", "F" -> buildLiteralExpression(
            null, ConstantValueKind.Float, protoValue?.floatValue ?: sourceValue as Float, setType = true
        )
        "DOUBLE", "D" -> buildLiteralExpression(
            null, ConstantValueKind.Double, protoValue?.doubleValue ?: sourceValue as Double, setType = true
        )
        "BOOLEAN", "Z" -> buildLiteralExpression(
            null, ConstantValueKind.Boolean, (protoValue?.intValue?.toInt() ?: sourceValue) != 0, setType = true
        )
        "STRING", "Ljava/lang/String;" -> buildLiteralExpression(
            null, ConstantValueKind.String,
            protoValue?.stringValue?.let { nameResolver.getString(it) } ?: sourceValue as String, setType = true
        )
        else -> null
    }
}

fun CallableId.replaceName(newName: Name): CallableId {
    return CallableId(this.packageName, this.className, newName)
}
