/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.compiler.wjs

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.js.analyze.AbstractTopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration

class TopDownAnalyzerFacadeForJSIR(private val jsFactories: KlibMetadataFactories) : AbstractTopDownAnalyzerFacadeForJS() {
    override fun loadIncrementalCacheMetadata(
        incrementalData: IncrementalDataProvider,
        moduleContext: ModuleContext,
        lookupTracker: LookupTracker,
        languageVersionSettings: LanguageVersionSettings
    ): PackageFragmentProvider {
        return jsFactories.DefaultDeserializedDescriptorFactory.createCachedPackageFragmentProvider(
            incrementalData.compiledPackageParts.values.map { it.metadata },
            moduleContext.storageManager,
            moduleContext.module,
            CompilerDeserializationConfiguration(languageVersionSettings),
            lookupTracker
        )
    }
}