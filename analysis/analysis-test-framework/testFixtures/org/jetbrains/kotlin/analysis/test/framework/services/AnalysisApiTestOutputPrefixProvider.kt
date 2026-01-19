/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider

/**
 * Provides a list of file prefixes used to determine the list of expected output files for Analysis API tests.
 *
 * @see AbstractAnalysisApiBasedTest.variantChain
 */
abstract class AnalysisApiTestOutputPrefixProvider : TestService {
    abstract fun getPrefixes(originalPrefixes: List<String>): List<String>
}

/**
 * A test prefix provider for multiplatform symbol tests.
 *
 * The resulting list of prefixes is produced based on
 * the original list of prefixes and the target platform set in the test configuration.
 * The resulting list is sorted from the most specific prefix to the least specific.
 *
 * For original prefix `descriptors` and JS target platform
 * [KmpSymbolTestOutputPrefixProvider] will produce `descriptors.js`, `js`, `descriptors.knm`, `knm` and `descriptors`.
 * `knm` in this case represents all non-JVM platforms.
 *
 * Note that when the JVM target platform is set, the provider returns the original list of prefixes.
 * That's because the JVM platform is considered a default platform and should not require additional prefixes.
 */
class KmpSymbolTestOutputPrefixProvider(private val testServices: TestServices) : AnalysisApiTestOutputPrefixProvider() {
    override fun getPrefixes(originalPrefixes: List<String>): List<String> {
        @OptIn(TestInfrastructureInternals::class)
        val targetPlatform = testServices.defaultsProvider.targetPlatform
        if (targetPlatform.isJvm()) {
            return originalPrefixes
        }

        val platformPrefix = when {
            targetPlatform.isJs() -> "js"
            targetPlatform.isNative() -> "native"
            targetPlatform.isCommon() -> "common"
            targetPlatform.isWasm() -> "wasm"
            else -> error("Unsupported platform $targetPlatform")
        }
        val knmPrefix = "knm"
        val originalWithPlatformPrefix = originalPrefixes.map { "$it.$platformPrefix" }
        val originalWithKnmPrefix = originalPrefixes.map { "$it.$knmPrefix" }
        return originalWithPlatformPrefix + platformPrefix + originalWithKnmPrefix + knmPrefix + originalPrefixes
    }
}

/**
 * Returns [AnalysisApiTestOutputPrefixProvider] when it's registered in the test configuration,
 * `null` otherwise.
 */
val TestServices.testOutputPrefixProvider: AnalysisApiTestOutputPrefixProvider? by TestServices.nullableTestServiceAccessor<AnalysisApiTestOutputPrefixProvider>()