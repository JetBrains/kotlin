/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js.klib

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.web.IncrementalDataProvider
import org.jetbrains.kotlin.ir.backend.web.WebFactories
import org.jetbrains.kotlin.js.analyze.AbstractTopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration

// TODO: put it in separated module `frontend.js`
object TopDownAnalyzerFacadeForJSIR : AbstractTopDownAnalyzerFacadeForJS() {
    override fun loadIncrementalCacheMetadata(
        incrementalData: IncrementalDataProvider,
        moduleContext: ModuleContext,
        lookupTracker: LookupTracker,
        languageVersionSettings: LanguageVersionSettings
    ): PackageFragmentProvider {
        return WebFactories.DefaultDeserializedDescriptorFactory.createCachedPackageFragmentProvider(
            incrementalData.compiledPackageParts.values.map { it.metadata },
            moduleContext.storageManager,
            moduleContext.module,
            CompilerDeserializationConfiguration(languageVersionSettings),
            lookupTracker
        )
    }
}