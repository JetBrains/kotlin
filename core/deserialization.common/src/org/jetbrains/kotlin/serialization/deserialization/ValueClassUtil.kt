/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.ExtendedValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.JvmInlineMultiFieldValueClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.RigidTypeMarker

fun <T : RigidTypeMarker> ProtoBuf.Class.loadValueClassRepresentation(
    tryLoadJvmInlineMultiFieldValueClass: Boolean,
    tryLoadExtendedValueClass: Boolean,
    nameResolver: NameResolver,
    typeTable: TypeTable,
    typeDeserializer: (ProtoBuf.Type) -> T,
    typeOfPublicProperty: (Name) -> T?,
): ValueClassRepresentation<T>? {
    val hasJvmInline = annotationList.any {
        val annotationId = nameResolver.getClassId(it.id)
        annotationId.relativeClassName.asString() == "JvmInline" && annotationId.packageFqName.asString() == "kotlin.jvm"
    }
    if (!hasJvmInline && tryLoadExtendedValueClass && Flags.IS_VALUE_CLASS.get(flags) && !hasInlineClassUnderlyingPropertyName()) {
        val modality = Flags.MODALITY.get(flags)
        val isAbstractOrSealed = modality == ProtoBuf.Modality.ABSTRACT || modality == ProtoBuf.Modality.SEALED
        val fields = if (isAbstractOrSealed) {
            null
        } else {
            val primaryConstructor = constructorList.singleOrNull { !Flags.IS_SECONDARY.get(it.flags) } ?: return null
            primaryConstructor.valueParameterList.map {
                nameResolver.getName(it.name) to typeDeserializer(it.type(typeTable))
            }
        }
        return ExtendedValueClassRepresentation(fields)
    }

    if (hasInlineClassUnderlyingPropertyName()) {
        val propertyName = nameResolver.getName(inlineClassUnderlyingPropertyName)
        val propertyType = inlineClassUnderlyingType(typeTable)?.let(typeDeserializer)
            ?: typeOfPublicProperty(propertyName)
            ?: error("cannot determine underlying type for value class ${nameResolver.getName(fqName)} with property $propertyName")
        return InlineClassRepresentation(propertyName, propertyType)
    }

    // Value classes without inline_class_underlying_property_name are treated as multi-field value classes, but only on JVM and if the
    // metadata version is large enough (1.5.1+), because we must be able to load inline classes compiled with earlier versions correctly.
    if (tryLoadJvmInlineMultiFieldValueClass && Flags.IS_VALUE_CLASS.get(flags)) {
        val primaryConstructor = constructorList.singleOrNull { !Flags.IS_SECONDARY.get(it.flags) } ?: return null
        return JvmInlineMultiFieldValueClassRepresentation(primaryConstructor.valueParameterList.map {
            nameResolver.getName(it.name) to typeDeserializer(it.type(typeTable))
        })
    }

    return null
}
