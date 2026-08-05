/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.descriptors.FullValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.impl.KotlinRigidTypeBean

private enum class KotlinValueClassRepresentationKind {
    // The order of entries is important, as an entry's ordinal is used to serialize/deserialize it.
    ABSENT,
    INLINE_CLASS,
    FULL_VALUE_CLASS,
}

internal fun serializeValueClassRepresentation(
    dataStream: StubOutputStream,
    representation: ValueClassRepresentation<KotlinRigidTypeBean>?,
) {
    when (representation) {
        null -> dataStream.writeVarInt(KotlinValueClassRepresentationKind.ABSENT.ordinal)

        is InlineClassRepresentation -> {
            dataStream.writeVarInt(KotlinValueClassRepresentationKind.INLINE_CLASS.ordinal)
            dataStream.serializeUnderlyingProperty(representation.underlyingPropertyName, representation.underlyingType)
        }

        is FullValueClassRepresentation -> {
            dataStream.writeVarInt(KotlinValueClassRepresentationKind.FULL_VALUE_CLASS.ordinal)

            // An abstract or a sealed full value class has no underlying properties at all, which differs from having an empty list
            val properties = representation.underlyingPropertyNamesToTypes
            dataStream.writeVarInt(if (properties == null) 0 else properties.size + 1)
            properties?.forEach { [name, type] -> dataStream.serializeUnderlyingProperty(name, type) }
        }
    }
}

internal fun deserializeValueClassRepresentation(dataStream: StubInputStream): ValueClassRepresentation<KotlinRigidTypeBean>? {
    return when (KotlinValueClassRepresentationKind.entries[dataStream.readVarInt()]) {
        KotlinValueClassRepresentationKind.ABSENT -> null

        KotlinValueClassRepresentationKind.INLINE_CLASS -> {
            val [name, type] = dataStream.deserializeUnderlyingProperty()
            InlineClassRepresentation(name, type)
        }

        KotlinValueClassRepresentationKind.FULL_VALUE_CLASS -> {
            val size = dataStream.readVarInt()
            val properties = if (size == 0) null else List(size - 1) { dataStream.deserializeUnderlyingProperty() }
            FullValueClassRepresentation(properties)
        }
    }
}

private fun StubOutputStream.serializeUnderlyingProperty(name: Name, type: KotlinRigidTypeBean) {
    writeName(name.asString())
    serializeTypeBean(this, type)
}

private fun StubInputStream.deserializeUnderlyingProperty(): Pair<Name, KotlinRigidTypeBean> {
    val name = Name.guessByFirstCharacter(requireNotNull(readNameString()))
    val type = deserializeTypeBean(this) as KotlinRigidTypeBean
    return name to type
}
