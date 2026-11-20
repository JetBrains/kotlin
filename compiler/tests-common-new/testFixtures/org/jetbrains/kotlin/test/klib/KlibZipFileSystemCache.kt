/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.zipFileSystemAccessor
import org.jetbrains.kotlin.io.ZipFileSystemCacheableAccessor
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.service

/**
 * Cache for test pipelines. It avoids re-opening a zipped KLIB by the frontend, and rebuilding the whole index of the archive
 * on every metadata lookup, which takes most of the time of a test against the zipped stdlib.
 * In contrast, the CLI pipeline caches the opened KLIBs by default (`-Xklib-zip-file-accessor-cache-limit`).
 */
class KlibZipFileSystemCache : TestService {
    val accessor: ZipFileSystemCacheableAccessor = ZipFileSystemCacheableAccessor(cacheLimit = CACHE_LIMIT)

    private companion object {
        /** The same as the default of `-Xklib-zip-file-accessor-cache-limit`. */
        const val CACHE_LIMIT = 64
    }
}

private val TestServices.klibZipFileSystemCache: KlibZipFileSystemCache by TestServices.testServiceAccessor()

/** Makes the first compilation stage of a test read the zipped KLIBs through its [KlibZipFileSystemCache]. */
class KlibZipFileSystemCacheConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::KlibZipFileSystemCache))

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.zipFileSystemAccessor = testServices.klibZipFileSystemCache.accessor
    }
}

/**
 * Closes the [KlibZipFileSystemCache] of a test once its first compilation stage is over.
 * Closing it on the disposal of the test would be too late: the temporary directories with the KLIBs are deleted before,
 * and a file system of a deleted archive would fail to close.
 */
class KlibZipFileSystemCacheCloser(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun check(thereWereFailures: Boolean) {
        testServices.klibZipFileSystemCache.accessor.reset()
    }
}
