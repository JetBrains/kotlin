/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.StubUtils.readNullableCollection
import org.jetbrains.kotlin.psi.stubs.StubUtils.writeNullableCollection
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFullValueClassRepresentation
import org.jetbrains.kotlin.psi.stubs.impl.KotlinInlineClassRepresentation
import org.jetbrains.kotlin.psi.stubs.impl.KotlinRigidTypeBean
import org.jetbrains.kotlin.psi.stubs.impl.KotlinValueClassRepresentation

private enum class KotlinValueClassRepresentationKind {
    // The order of entries is important, as an entry's ordinal is used to serialize/deserialize it.
    ABSENT,
    INLINE_CLASS,
    FULL_VALUE_CLASS,
}

internal fun serializeValueClassRepresentation(
    dataStream: StubOutputStream,
    representation: KotlinValueClassRepresentation?,
) {
    when (representation) {
        null -> dataStream.writeVarInt(KotlinValueClassRepresentationKind.ABSENT.ordinal)

        is KotlinInlineClassRepresentation -> {
            dataStream.writeVarInt(KotlinValueClassRepresentationKind.INLINE_CLASS.ordinal)
            dataStream.serializeUnderlyingProperty(representation.underlyingPropertyName, representation.underlyingType)
        }

        is KotlinFullValueClassRepresentation -> {
            dataStream.writeVarInt(KotlinValueClassRepresentationKind.FULL_VALUE_CLASS.ordinal)
            dataStream.writeNullableCollection(representation.underlyingPropertyNamesToTypes) { [name, type] ->
                serializeUnderlyingProperty(name, type)
            }
        }
    }
}

internal fun deserializeValueClassRepresentation(dataStream: StubInputStream): KotlinValueClassRepresentation? {
    return when (KotlinValueClassRepresentationKind.entries[dataStream.readVarInt()]) {
        KotlinValueClassRepresentationKind.ABSENT -> null

        KotlinValueClassRepresentationKind.INLINE_CLASS -> {
            val [name, type] = dataStream.deserializeUnderlyingProperty()
            KotlinInlineClassRepresentation(name, type)
        }

        KotlinValueClassRepresentationKind.FULL_VALUE_CLASS -> {
            val properties = dataStream.readNullableCollection { deserializeUnderlyingProperty() }
            KotlinFullValueClassRepresentation(properties)
        }
    }
}

private fun StubOutputStream.serializeUnderlyingProperty(name: Name, type: KotlinRigidTypeBean) {
    writeName(name.asString())
    serializeTypeBean(this, type)
}

private fun StubInputStream.deserializeUnderlyingProperty(): Pair<Name, KotlinRigidTypeBean> {
    val name = Name.identifier(requireNotNull(readNameString()))
    val type = deserializeTypeBean(this) as KotlinRigidTypeBean
    return name to type
}
