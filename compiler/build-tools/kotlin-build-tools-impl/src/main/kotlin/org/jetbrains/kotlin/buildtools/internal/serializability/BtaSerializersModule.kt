/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.serializability

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.kotlin.buildtools.api.js.JsIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.wasm.WasmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.js.JsHistoryBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsLinkingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.metadata.operations.KotlinMetadataKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.WasmHistoryBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.operations.WasmKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.operations.WasmLinkingOperationImpl

internal val btaSerializersModule: SerializersModule = SerializersModule {
    polymorphic(BtaSerializable::class) {
        subclass(JvmCompilationOperationImpl::class)
        subclass(JsKlibCompilationOperationImpl::class)
        subclass(WasmKlibCompilationOperationImpl::class)
        subclass(KotlinMetadataKlibCompilationOperationImpl::class)
        subclass(JsLinkingOperationImpl::class)
        subclass(WasmLinkingOperationImpl::class)
    }
    polymorphic(JvmIncrementalCompilationConfiguration::class) {
        subclass(
            JvmSnapshotBasedIncrementalCompilationConfigurationImpl::class,
            JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer
        )
    }
    polymorphic(JsIncrementalCompilationConfiguration::class) {
        subclass(
            JsHistoryBasedIncrementalCompilationConfigurationImpl::class
        )
    }
    polymorphic(WasmIncrementalCompilationConfiguration::class) {
        subclass(
            WasmHistoryBasedIncrementalCompilationConfigurationImpl::class
        )
    }
}
