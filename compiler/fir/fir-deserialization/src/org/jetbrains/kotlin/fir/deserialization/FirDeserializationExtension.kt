/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirComposableSessionComponent
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class FirDeserializationExtension(val session: FirSession) : FirComposableSessionComponent<FirDeserializationExtension> {
    open fun createConstDeserializer(
        containerSource: DeserializedContainerSource?,
        session: FirSession,
        serializerExtensionProtocol: SerializerExtensionProtocol,
    ): FirConstDeserializer? = null

    open fun FirRegularClassBuilder.configureDeserializedClass(classId: ClassId) {}

    open fun loadModuleName(classProto: ProtoBuf.Class, nameResolver: NameResolver): String? = null

    open fun loadHasBackingFieldFlag(propertyProto: ProtoBuf.Property): Boolean? = null

    open fun isMaybeMultiFieldValueClass(containerSource: DeserializedContainerSource?): Boolean = false

    @SessionConfiguration
    final override fun createComposed(components: List<FirDeserializationExtension>): Composed {
        return Composed(session, components)
    }

    class Composed(
        session: FirSession,
        override val components: List<FirDeserializationExtension>
    ) : FirDeserializationExtension(session), FirComposableSessionComponent.Composed<FirDeserializationExtension> {
        override fun createConstDeserializer(
            containerSource: DeserializedContainerSource?,
            session: FirSession,
            serializerExtensionProtocol: SerializerExtensionProtocol,
        ): FirConstDeserializer? {
            return components.firstNotNullOfOrNull { it.createConstDeserializer(containerSource, session, serializerExtensionProtocol) }
        }

        override fun FirRegularClassBuilder.configureDeserializedClass(classId: ClassId) {
            components.forEach { with(it) { configureDeserializedClass(classId) } }
        }

        override fun loadModuleName(
            classProto: ProtoBuf.Class,
            nameResolver: NameResolver,
        ): String? {
            return components.firstNotNullOfOrNull { it.loadModuleName(classProto, nameResolver) }
        }

        override fun loadHasBackingFieldFlag(propertyProto: ProtoBuf.Property): Boolean? {
            return components.firstNotNullOfOrNull { it.loadHasBackingFieldFlag(propertyProto) }
        }

        override fun isMaybeMultiFieldValueClass(containerSource: DeserializedContainerSource?): Boolean {
            return components.any { it.isMaybeMultiFieldValueClass(containerSource) }
        }
    }
}
