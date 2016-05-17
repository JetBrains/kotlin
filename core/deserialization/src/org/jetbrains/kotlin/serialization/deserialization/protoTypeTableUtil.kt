/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.utils.ifEmpty

fun ProtoBuf.Class.supertypes(typeTable: TypeTable): List<ProtoBuf.Type> {
    return supertypeList.ifEmpty { supertypeIdList.map { typeTable[it] } }
}

fun ProtoBuf.Type.Argument.type(typeTable: TypeTable): ProtoBuf.Type? {
    return when {
        hasType() -> type
        hasTypeId() -> typeTable[typeId]
        else -> null
    }
}

fun ProtoBuf.Type.flexibleUpperBound(typeTable: TypeTable): ProtoBuf.Type? {
    return when {
        hasFlexibleUpperBound() -> flexibleUpperBound
        hasFlexibleUpperBoundId() -> typeTable[flexibleUpperBoundId]
        else -> null
    }
}

fun ProtoBuf.TypeParameter.upperBounds(typeTable: TypeTable): List<ProtoBuf.Type> {
    return upperBoundList.ifEmpty { upperBoundIdList.map { typeTable[it] } }
}

fun ProtoBuf.Function.returnType(typeTable: TypeTable): ProtoBuf.Type {
    return if (hasReturnType()) returnType else typeTable[returnTypeId]
}

fun ProtoBuf.Function.hasReceiver(): Boolean = hasReceiverType() || hasReceiverTypeId()

fun ProtoBuf.Function.receiverType(typeTable: TypeTable): ProtoBuf.Type? {
    return when {
        hasReceiverType() -> receiverType
        hasReceiverTypeId() -> typeTable[receiverTypeId]
        else -> null
    }
}

fun ProtoBuf.Property.returnType(typeTable: TypeTable): ProtoBuf.Type {
    return if (hasReturnType()) returnType else typeTable[returnTypeId]
}

fun ProtoBuf.Property.hasReceiver(): Boolean = hasReceiverType() || hasReceiverTypeId()

fun ProtoBuf.Property.receiverType(typeTable: TypeTable): ProtoBuf.Type? {
    return when {
        hasReceiverType() -> receiverType
        hasReceiverTypeId() -> typeTable[receiverTypeId]
        else -> null
    }
}

fun ProtoBuf.ValueParameter.type(typeTable: TypeTable): ProtoBuf.Type {
    return if (hasType()) type else typeTable[typeId]
}

fun ProtoBuf.ValueParameter.varargElementType(typeTable: TypeTable): ProtoBuf.Type? {
    return when {
        hasVarargElementType() -> varargElementType
        hasVarargElementTypeId() -> typeTable[varargElementTypeId]
        else -> null
    }
}

fun ProtoBuf.Type.outerType(typeTable: TypeTable): ProtoBuf.Type? {
    return when {
        hasOuterType() -> outerType
        hasOuterTypeId() -> typeTable[outerTypeId]
        else -> null
    }
}

fun ProtoBuf.Type.abbreviatedType(typeTable: TypeTable): ProtoBuf.Type? =
        when {
            hasAbbreviatedType() -> abbreviatedType
            hasAbbreviatedTypeId() -> typeTable[abbreviatedTypeId]
            else -> null
        }

fun ProtoBuf.TypeAlias.underlyingType(typeTable: TypeTable): ProtoBuf.Type =
        if (hasUnderlyingTypeId()) typeTable[underlyingTypeId] else underlyingType

fun ProtoBuf.TypeAlias.expandedType(typeTable: TypeTable): ProtoBuf.Type =
        if (hasExpandedTypeId()) typeTable[expandedTypeId] else expandedType