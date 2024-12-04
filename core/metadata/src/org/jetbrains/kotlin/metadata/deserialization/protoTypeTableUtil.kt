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

package org.jetbrains.kotlin.metadata.deserialization

import org.jetbrains.kotlin.metadata.ProtoBuf

// TODO: return null and report a diagnostic instead of throwing exceptions

fun ProtoBuf.Class.supertypes(typeTable: TypeTable): List<ProtoBuf.Type> =
    supertypeList.takeIf(Collection<*>::isNotEmpty) ?: supertypeIdList.map { typeTable[it] }

fun ProtoBuf.Class.inlineClassUnderlyingType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasInlineClassUnderlyingType() -> inlineClassUnderlyingType
    hasInlineClassUnderlyingTypeId() -> typeTable[inlineClassUnderlyingTypeId]
    else -> null
}

fun ProtoBuf.Type.Argument.type(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasType() -> type
    hasTypeId() -> typeTable[typeId]
    else -> null
}

fun ProtoBuf.Type.flexibleUpperBound(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasFlexibleUpperBound() -> flexibleUpperBound
    hasFlexibleUpperBoundId() -> typeTable[flexibleUpperBoundId]
    else -> null
}

fun ProtoBuf.TypeParameter.upperBounds(typeTable: TypeTable): List<ProtoBuf.Type> =
    upperBoundList.takeIf(Collection<*>::isNotEmpty) ?: upperBoundIdList.map { typeTable[it] }

fun ProtoBuf.Function.returnType(typeTable: TypeTable): ProtoBuf.Type = when {
    hasReturnType() -> returnType
    hasReturnTypeId() -> typeTable[returnTypeId]
    else -> error("No returnType in ProtoBuf.Function")
}

fun ProtoBuf.Function.hasReceiver(): Boolean = hasReceiverType() || hasReceiverTypeId()

fun ProtoBuf.Function.receiverType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasReceiverType() -> receiverType
    hasReceiverTypeId() -> typeTable[receiverTypeId]
    else -> null
}

fun ProtoBuf.Property.returnType(typeTable: TypeTable): ProtoBuf.Type = when {
    hasReturnType() -> returnType
    hasReturnTypeId() -> typeTable[returnTypeId]
    else -> error("No returnType in ProtoBuf.Property")
}

fun ProtoBuf.Property.hasReceiver(): Boolean = hasReceiverType() || hasReceiverTypeId()

fun ProtoBuf.Property.receiverType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasReceiverType() -> receiverType
    hasReceiverTypeId() -> typeTable[receiverTypeId]
    else -> null
}

fun ProtoBuf.ValueParameter.type(typeTable: TypeTable): ProtoBuf.Type = when {
    hasType() -> type
    hasTypeId() -> typeTable[typeId]
    else -> error("No type in ProtoBuf.ValueParameter")
}

fun ProtoBuf.ValueParameter.varargElementType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasVarargElementType() -> varargElementType
    hasVarargElementTypeId() -> typeTable[varargElementTypeId]
    else -> null
}

fun ProtoBuf.Type.outerType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasOuterType() -> outerType
    hasOuterTypeId() -> typeTable[outerTypeId]
    else -> null
}

fun ProtoBuf.Type.abbreviatedType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasAbbreviatedType() -> abbreviatedType
    hasAbbreviatedTypeId() -> typeTable[abbreviatedTypeId]
    else -> null
}

fun ProtoBuf.TypeAlias.underlyingType(typeTable: TypeTable): ProtoBuf.Type = when {
    hasUnderlyingType() -> underlyingType
    hasUnderlyingTypeId() -> typeTable[underlyingTypeId]
    else -> error("No underlyingType in ProtoBuf.TypeAlias")
}

fun ProtoBuf.TypeAlias.expandedType(typeTable: TypeTable): ProtoBuf.Type = when {
    hasExpandedType() -> expandedType
    hasExpandedTypeId() -> typeTable[expandedTypeId]
    else -> error("No expandedType in ProtoBuf.TypeAlias")
}

fun ProtoBuf.Expression.isInstanceType(typeTable: TypeTable): ProtoBuf.Type? = when {
    hasIsInstanceType() -> isInstanceType
    hasIsInstanceTypeId() -> typeTable[isInstanceTypeId]
    else -> null
}

fun ProtoBuf.Class.contextReceiverTypes(typeTable: TypeTable): List<ProtoBuf.Type> =
    contextReceiverTypeList.takeIf(Collection<*>::isNotEmpty) ?: contextReceiverTypeIdList.map { typeTable[it] }

fun ProtoBuf.Function.contextReceiverTypes(typeTable: TypeTable): List<ProtoBuf.Type> =
    contextReceiverTypeList.takeIf(Collection<*>::isNotEmpty) ?: contextReceiverTypeIdList.map { typeTable[it] }

fun ProtoBuf.Property.contextReceiverTypes(typeTable: TypeTable): List<ProtoBuf.Type> =
    contextReceiverTypeList.takeIf(Collection<*>::isNotEmpty) ?: contextReceiverTypeIdList.map { typeTable[it] }
