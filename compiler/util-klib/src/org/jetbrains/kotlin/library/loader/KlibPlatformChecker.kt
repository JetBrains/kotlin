/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.loader

import org.jetbrains.kotlin.library.BaseKotlinLibrary
import org.jetbrains.kotlin.library.builtInsPlatform
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.irProviderName
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.PlatformCheckMismatch
import org.jetbrains.kotlin.library.nativeTargets
import org.jetbrains.kotlin.library.wasmTargets

interface KlibPlatformChecker {
    fun check(library: BaseKotlinLibrary): PlatformCheckMismatch?

    /**
     * Checks if a library is a Kotlin/Native library.
     * If [target] is not null, then additionally checks that the given target is supported in the library.
     */
    class Native(
        private val target: String? = null,
        private val knownIrProviders: Set<String> = emptySet()
    ) : KlibPlatformChecker {
        override fun check(library: BaseKotlinLibrary): PlatformCheckMismatch? =
            checkPlatform(
                expectedPlatform = BuiltInsPlatform.NATIVE,
                actualPlatform = library.builtInsPlatform
            ) ?: checkTarget(
                platform = BuiltInsPlatform.NATIVE,
                expectedTarget = target,
                actualTargets = library.nativeTargets
            ) ?: checkIrProvider(
                expectedIrProviders = knownIrProviders,
                actualIrProvider = library.irProviderName
            )

        companion object {
            private fun checkIrProvider(
                expectedIrProviders: Set<String>,
                actualIrProvider: String?
            ): PlatformCheckMismatch? {
                if (actualIrProvider == null || actualIrProvider in expectedIrProviders) return null

                return PlatformCheckMismatch(
                    property = "irProvider",
                    expected = if (expectedIrProviders.isEmpty()) "<none>" else expectedIrProviders.joinToString(),
                    actual = actualIrProvider
                )
            }
        }
    }

    /**
     * Checks if a library is a Kotlin/JS library.
     */
    object JS : KlibPlatformChecker {
        override fun check(library: BaseKotlinLibrary): PlatformCheckMismatch? =
            checkPlatform(
                expectedPlatform = BuiltInsPlatform.JS,
                actualPlatform = library.builtInsPlatform
            )
    }

    /**
     * Checks if a library is a Kotlin/Wasm library.
     * If [target] is not null, then additionally checks that the given target is supported in the library.
     */
    class Wasm(private val target: String? = null) : KlibPlatformChecker {
        override fun check(library: BaseKotlinLibrary): PlatformCheckMismatch? =
            checkPlatform(
                expectedPlatform = BuiltInsPlatform.WASM,
                actualPlatform = library.builtInsPlatform
            ) ?: run {

                val expectedTarget: String = target ?: return null
                val actualTargets: List<String> = library.wasmTargets

                if (actualTargets.isEmpty() && library.versions.abiVersion?.isAtMost(1, 8, 0) == true) {
                    // Only Kotlin/Wasm KLIBs produced with the compiler version >= 2.0.0 have targets in the manifest (see KT-66327).
                    // In 2.0.0 (as well as in the preceding 1.9.x) we had the ABI version = 1.8.0.
                    return null
                }

                checkTarget(
                    platform = BuiltInsPlatform.WASM,
                    expectedTarget = expectedTarget,
                    actualTargets = actualTargets
                )
            }
    }

    companion object {
        private fun checkPlatform(
            expectedPlatform: BuiltInsPlatform,
            actualPlatform: BuiltInsPlatform?
        ): PlatformCheckMismatch? {
            if (actualPlatform == expectedPlatform) return null

            return PlatformCheckMismatch(
                property = "platform",
                expected = expectedPlatform.name,
                actual = actualPlatform?.name ?: "<unknown>"
            )
        }

        private fun checkTarget(
            platform: BuiltInsPlatform,
            expectedTarget: String?,
            actualTargets: List<String>,
        ): PlatformCheckMismatch? {
            if (expectedTarget in actualTargets) return null

            return PlatformCheckMismatch(
                property = "target",
                expected = "${platform.name}[$expectedTarget]",
                actual = "${platform.name}[${actualTargets.joinToString()}]"
            )
        }
    }
}
