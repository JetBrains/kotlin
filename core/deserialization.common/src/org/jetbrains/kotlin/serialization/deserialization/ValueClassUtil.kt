/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.MultiFieldValueClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.inlineClassUnderlyingType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

fun <T : SimpleTypeMarker> ProtoBuf.Class.loadValueClassRepresentation(
    nameResolver: NameResolver,
    typeTable: TypeTable,
    typeDeserializer: (ProtoBuf.Type) -> T,
    typeOfPublicProperty: (Name) -> T?,
): ValueClassRepresentation<T>? {
    if (multiFieldValueClassUnderlyingNameCount > 0) {
        val names = multiFieldValueClassUnderlyingNameList.map { nameResolver.getName(it) }
        val typeIdCount = multiFieldValueClassUnderlyingTypeIdCount
        val typeCount = multiFieldValueClassUnderlyingTypeCount
        val types = when (typeIdCount to typeCount) {
            names.size to 0 -> multiFieldValueClassUnderlyingTypeIdList.map { typeTable[it] }
            0 to names.size -> multiFieldValueClassUnderlyingTypeList
            else -> error("class ${nameResolver.getName(fqName)} has illegal multi-field value class representation")
        }.map(typeDeserializer)
        return MultiFieldValueClassRepresentation(names zip types)
    }

    if (hasInlineClassUnderlyingPropertyName()) {
        val propertyName = nameResolver.getName(inlineClassUnderlyingPropertyName)
        val propertyType = inlineClassUnderlyingType(typeTable)?.let(typeDeserializer)
            ?: typeOfPublicProperty(propertyName)
            ?: error("cannot determine underlying type for value class ${nameResolver.getName(fqName)} with property $propertyName")
        return InlineClassRepresentation(propertyName, propertyType)
    }

    return null
}
