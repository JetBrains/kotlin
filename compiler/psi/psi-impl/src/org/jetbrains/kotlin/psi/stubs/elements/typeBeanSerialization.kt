/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.psi.KtProjectionKind
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.psi.stubs.impl.*

internal enum class KotlinTypeBeanKind {
    // The order of entries is important, as an entry's ordinal is used to serialize/deserialize it.
    CLASS,
    TYPE_PARAMETER,
    FLEXIBLE,
    NONE;

    companion object {
        fun fromBean(typeBean: KotlinTypeBean?): KotlinTypeBeanKind = when (typeBean) {
            null -> NONE
            is KotlinTypeParameterTypeBean -> TYPE_PARAMETER
            is KotlinClassTypeBean -> CLASS
            else -> FLEXIBLE
        }
    }
}

internal fun serializeTypeBean(dataStream: StubOutputStream, type: KotlinTypeBean?) {
    dataStream.writeVarInt(KotlinTypeBeanKind.fromBean(type).ordinal)

    when (type) {
        null -> {}

        is KotlinClassTypeBean -> {
            StubUtils.serializeClassId(dataStream, type.classId)
            dataStream.writeBoolean(type.nullable)
            dataStream.writeVarInt(type.arguments.size)
            for (argument in type.arguments) {
                dataStream.writeVarInt(argument.projectionKind.ordinal)
                if (argument.projectionKind != KtProjectionKind.STAR) {
                    serializeTypeBean(dataStream, argument.type)
                }
            }
            serializeTypeBean(dataStream, type.abbreviatedType)
        }

        is KotlinTypeParameterTypeBean -> {
            dataStream.writeName(type.typeParameterName)
            dataStream.writeBoolean(type.nullable)
            dataStream.writeBoolean(type.definitelyNotNull)
        }

        is KotlinFlexibleTypeBean -> {
            serializeTypeBean(dataStream, type.lowerBound)
            serializeTypeBean(dataStream, type.upperBound)
        }
    }
}

internal fun deserializeClassTypeBean(dataStream: StubInputStream): KotlinClassTypeBean? {
    // Ignore non-class types defensively to avoid throwing an exception from stub deserialization.
    return deserializeTypeBean(dataStream) as? KotlinClassTypeBean
}

internal fun deserializeTypeBean(dataStream: StubInputStream): KotlinTypeBean? {
    val typeKind = KotlinTypeBeanKind.entries[dataStream.readVarInt()]

    return when (typeKind) {
        KotlinTypeBeanKind.CLASS -> {
            val classId = requireNotNull(StubUtils.deserializeClassId(dataStream))
            val isNullable = dataStream.readBoolean()
            val count = dataStream.readVarInt()
            val arguments = buildList {
                repeat(count) {
                    add(deserializeTypeArgumentBean(dataStream))
                }
            }
            val abbreviatedType = deserializeClassTypeBean(dataStream)

            KotlinClassTypeBean(classId, arguments, isNullable, abbreviatedType)
        }

        KotlinTypeBeanKind.TYPE_PARAMETER -> {
            val typeParameterName = requireNotNull(dataStream.readNameString())
            val nullable = dataStream.readBoolean()
            val definitelyNotNull = dataStream.readBoolean()

            KotlinTypeParameterTypeBean(typeParameterName, nullable, definitelyNotNull)
        }

        KotlinTypeBeanKind.FLEXIBLE -> KotlinFlexibleTypeBean(
            requireNotNull(deserializeTypeBean(dataStream)),
            requireNotNull(deserializeTypeBean(dataStream)),
        )

        KotlinTypeBeanKind.NONE -> null
    }
}

private fun deserializeTypeArgumentBean(dataStream: StubInputStream): KotlinTypeArgumentBean {
    val projectionKind = KtProjectionKind.entries[dataStream.readVarInt()]
    val type = if (projectionKind != KtProjectionKind.STAR) deserializeTypeBean(dataStream) else null
    return KotlinTypeArgumentBean(projectionKind, type)
}
