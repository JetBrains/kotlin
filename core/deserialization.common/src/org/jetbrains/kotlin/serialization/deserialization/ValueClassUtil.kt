/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.FullValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.RigidTypeMarker

fun <T : RigidTypeMarker> ProtoBuf.Class.loadValueClassRepresentation(
    tryLoadFullValueClass: Boolean,
    nameResolver: NameResolver,
    typeTable: TypeTable,
    typeDeserializer: (ProtoBuf.Type) -> T,
    typeOfPublicProperty: (Name) -> T?,
): ValueClassRepresentation<T>? {
    val hasJvmInline = annotationList.any {
        val annotationId = nameResolver.getClassId(it.id)
        annotationId.relativeClassName.asString() == "JvmInline" && annotationId.packageFqName.asString() == "kotlin.jvm"
    }
    if (!hasJvmInline && tryLoadFullValueClass && Flags.IS_VALUE_CLASS.get(flags) && !hasInlineClassUnderlyingPropertyName()) {
        val modality = Flags.MODALITY.get(flags)
        val isAbstractOrSealed = modality == ProtoBuf.Modality.ABSTRACT || modality == ProtoBuf.Modality.SEALED
        val fields = if (isAbstractOrSealed) {
            null
        } else {
            val [names, types] = loadFullValueClassUnderlyingProperties(nameResolver, typeTable)
            names zip types.map(typeDeserializer)
        }
        return FullValueClassRepresentation(fields)
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

fun ProtoBuf.Class.loadFullValueClassUnderlyingProperties(
    nameResolver: NameResolver,
    typeTable: TypeTable,
): Pair<List<Name>, List<ProtoBuf.Type>> {
    val names = fullValueClassUnderlyingPropertyNameList.map { nameResolver.getName(it) }
    val types = when (fullValueClassUnderlyingTypeIdCount to fullValueClassUnderlyingTypeCount) {
        names.size to 0 -> fullValueClassUnderlyingTypeIdList.map { typeTable[it] }
        0 to names.size -> fullValueClassUnderlyingTypeList
        else -> error("class ${nameResolver.getName(fqName)} has illegal full value class representation")
    }
    return names to types
}
