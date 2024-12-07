/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class FirDeserializationExtension(val session: FirSession) : FirSessionComponent {
    open fun createConstDeserializer(
        containerSource: DeserializedContainerSource?,
        session: FirSession,
        serializerExtensionProtocol: SerializerExtensionProtocol,
    ): FirConstDeserializer? = null

    open fun FirRegularClassBuilder.configureDeserializedClass(classId: ClassId) {}

    open fun loadModuleName(classProto: ProtoBuf.Class, nameResolver: NameResolver): String? = null
}
