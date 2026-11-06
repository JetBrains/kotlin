/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.serializability

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.abi.AbiFilters
import org.jetbrains.kotlin.buildtools.api.js.JsIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.wasm.WasmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.abi.AbiFiltersImpl
import org.jetbrains.kotlin.buildtools.internal.abi.operations.CompareAbiTextFilesOperationImpl
import org.jetbrains.kotlin.buildtools.internal.abi.operations.DumpJvmAbiToStringOperationImpl
import org.jetbrains.kotlin.buildtools.internal.abi.operations.DumpKlibAbiToStringOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jps.JvmJpsManagedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.js.JsHistoryBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsLinkingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.DiscoverScriptExtensionsOperationImpl
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
        subclass(DiscoverScriptExtensionsOperationImpl::class)
        subclass(CompareAbiTextFilesOperationImpl::class)
        subclass(DumpKlibAbiToStringOperationImpl::class)
        subclass(DumpJvmAbiToStringOperationImpl::class)
    }
    polymorphic(JvmIncrementalCompilationConfiguration::class) {
        subclass(
            JvmSnapshotBasedIncrementalCompilationConfigurationImpl::class,
            JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer
        )
        try {
            Class.forName("org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration").let {
                @OptIn(DelicateBuildToolsApi::class)
                subclass(JvmJpsManagedIncrementalCompilationConfigurationImpl::class)
            }
        } catch (_: ClassNotFoundException) {
            // ignore - optional JPS module was not loaded, JPS classes will not be used
        }
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
    polymorphic(AbiFilters::class) {
        subclass(AbiFiltersImpl::class)
    }
}
