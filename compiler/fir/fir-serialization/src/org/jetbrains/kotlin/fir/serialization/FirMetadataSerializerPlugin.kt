/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.FirExtensionPointName
import org.jetbrains.kotlin.fir.extensions.FirExtensionService
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import kotlin.reflect.KClass


/**
 * This extension point is temporary and should not be used
 *
 * It will be removed soon: KT-64695
 * DON'T USE IT
 */
@FirExtensionApiInternals
abstract class FirMetadataSerializerPlugin(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("MetadataSerializerPlugin")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension>
        get() = FirMetadataSerializerPlugin::class

    abstract fun registerProtoExtensions(
        symbol: FirRegularClassSymbol,
        stringTable: FirElementAwareStringTable,
        protoRegistrar: ProtoRegistrar
    )

    interface ProtoRegistrar {
        fun <Type> setExtension(
            extension: GeneratedMessageLite.GeneratedExtension<ProtoBuf.Class, Type>,
            value: Type,
        )
    }

    fun interface Factory : FirExtension.Factory<FirMetadataSerializerPlugin>
}

@OptIn(FirExtensionApiInternals::class)
internal val FirExtensionService.metadataSerializerPlugins: List<FirMetadataSerializerPlugin> by FirExtensionService.registeredExtensions()
