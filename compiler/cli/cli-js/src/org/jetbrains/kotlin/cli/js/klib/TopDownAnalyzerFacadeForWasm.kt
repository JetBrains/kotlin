/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js.klib

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.js.analyze.AbstractTopDownAnalyzerFacadeForWeb
import org.jetbrains.kotlin.js.config.WasmTarget
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.wasm.resolve.WasmJsPlatformAnalyzerServices
import org.jetbrains.kotlin.wasm.resolve.WasmWasiPlatformAnalyzerServices

abstract class TopDownAnalyzerFacadeForWasm : AbstractTopDownAnalyzerFacadeForWeb() {
    override val platform: TargetPlatform = WasmPlatforms.Default

    override fun loadIncrementalCacheMetadata(
        incrementalData: IncrementalDataProvider,
        moduleContext: ModuleContext,
        lookupTracker: LookupTracker,
        languageVersionSettings: LanguageVersionSettings
    ): PackageFragmentProvider {
        return JsFactories.DefaultDeserializedDescriptorFactory.createCachedPackageFragmentProvider(
            incrementalData.compiledPackageParts.values.map { it.metadata },
            moduleContext.storageManager,
            moduleContext.module,
            CompilerDeserializationConfiguration(languageVersionSettings),
            lookupTracker
        )
    }

    companion object {
        fun facadeFor(target: WasmTarget?): TopDownAnalyzerFacadeForWasm = when (target) {
            WasmTarget.WASI -> TopDownAnalyzerFacadeForWasmWasi
            else -> TopDownAnalyzerFacadeForWasmJs
        }
    }
}

object TopDownAnalyzerFacadeForWasmJs : TopDownAnalyzerFacadeForWasm() {
    override val analyzerServices: PlatformDependentAnalyzerServices = WasmJsPlatformAnalyzerServices
}

object TopDownAnalyzerFacadeForWasmWasi : TopDownAnalyzerFacadeForWasm() {
    override val analyzerServices: PlatformDependentAnalyzerServices = WasmWasiPlatformAnalyzerServices
}