/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.loader

import org.jetbrains.kotlin.library.BaseKotlinLibrary
import org.jetbrains.kotlin.library.builtInsPlatform
import org.jetbrains.kotlin.library.commonizerNativeTargets
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.PlatformCheckMismatch
import org.jetbrains.kotlin.library.nativeTargets
import org.jetbrains.kotlin.library.wasmTargets

interface KlibPlatformChecker {
    fun check(library: BaseKotlinLibrary): PlatformCheckMismatch?

    /**
     * Checks if a library is a Kotlin/Native library.
     * If [target] is not null, then additionally checks that the given target is supported in the library.
     */
    class Native(private val target: String? = null) : KlibPlatformChecker {
        override fun check(library: BaseKotlinLibrary) = firstFailedCheckOf(
            {
                checkPlatform(
                    expectedPlatform = BuiltInsPlatform.NATIVE,
                    actualPlatform = library.builtInsPlatform,
                )
            }, targetCheck@{
                checkTarget(
                    platform = BuiltInsPlatform.NATIVE,
                    expectedTarget = target ?: return@targetCheck null,
                    /**
                     * The Kotlin/Native standard library published as a separate artifact has no targets in the manifest.
                     * We need to skip the check if this is the case.
                     */
                    actualTargets = library.nativeTargets.takeIf { it.isNotEmpty() } ?: return@targetCheck null,
                )
            }
        )
    }

    /**
     * Checks if a library is a Kotlin/Native metadata-compilation suitable library:
     * - Either a natural Kotlin/Native library
     * - Or a commonized Kotlin/Native library
     * - Or a metadata-aware Kotlin/Native library
     */
    class NativeMetadata(private val target: String) : KlibPlatformChecker {
        override fun check(library: BaseKotlinLibrary) = firstFailedCheckOf(
            platformCheck@{
                checkPlatform(
                    expectedPlatform = BuiltInsPlatform.NATIVE,
                    /**
                     * Common (platform-agnostic) libraries use [BuiltInsPlatform.COMMON] built-ins platform.
                     * But [BuiltInsPlatform.COMMON] is a special value that is not written to the manifest,
                     * effectively leaving such libraries without the `builtins_platform` property.
                     * So, to make this check work properly, we should skip validation of the platform when
                     * the `builtins_platform` property is missing in the manifest assuming this is a "common" platform.
                     */
                    actualPlatform = library.builtInsPlatform ?: return@platformCheck null,
                )
            }, targetCheck@{
                val commonizerNativeTargets = library.commonizerNativeTargets
                if (!commonizerNativeTargets.isNullOrEmpty()) {
                    /**
                     * This is a commonized Kotlin/Native library.
                     * We need to check that the given target is supported in the commonized library.
                     */
                    checkTarget(
                        platform = BuiltInsPlatform.NATIVE,
                        expectedTarget = target,
                        actualTargets = commonizerNativeTargets,
                    )
                } else {
                    checkTarget(
                        platform = BuiltInsPlatform.NATIVE,
                        expectedTarget = target,
                        /**
                         * Some metadata-only Kotlin/Native libraries might not have any targets written in the manifest.
                         * Also, the Kotlin/Native standard library published as a separate artifact has no targets in the manifest.
                         * We need to skip the check if this is the case.
                         */
                        actualTargets = library.nativeTargets.takeIf { it.isNotEmpty() } ?: return@targetCheck null,
                    )
                }
            }
        )
    }

    /**
     * Checks if a library is a Kotlin/JS library.
     */
    object JS : KlibPlatformChecker {
        override fun check(library: BaseKotlinLibrary): PlatformCheckMismatch? {
            return checkPlatform(BuiltInsPlatform.JS, library.builtInsPlatform)
        }
    }

    /**
     * Checks if a library is a Kotlin/Wasm library.
     * If [target] is not null, then additionally checks that the given target is supported in the library.
     */
    class Wasm(private val target: String? = null) : KlibPlatformChecker {
        override fun check(library: BaseKotlinLibrary) = firstFailedCheckOf(
            {
                checkPlatform(
                    expectedPlatform = BuiltInsPlatform.WASM,
                    actualPlatform = library.builtInsPlatform
                )
            }, targetCheck@{
                checkTarget(
                    platform = BuiltInsPlatform.WASM,
                    expectedTarget = target ?: return@targetCheck null,
                    actualTargets = library.wasmTargets.takeUnless {
                        /**
                         * Only Kotlin/Wasm KLIBs produced with the compiler version >= 2.0.0 have targets in the manifest (see KT-66327).
                         * In 2.0.0 (as well as in the preceding 1.9.x) we had the ABI version = 1.8.0.
                         */
                        it.isEmpty() && library.versions.abiVersion?.isAtMost(1, 8, 0) == true
                    } ?: return@targetCheck null
                )
            }
        )
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
            expectedTarget: String,
            actualTargets: List<String>,
        ): PlatformCheckMismatch? {
            if (expectedTarget in actualTargets) return null

            return PlatformCheckMismatch(
                property = "target",
                expected = "${platform.name}[$expectedTarget]",
                actual = "${platform.name}[${actualTargets.joinToString()}]"
            )
        }

        private fun firstFailedCheckOf(vararg checks: () -> PlatformCheckMismatch?): PlatformCheckMismatch? {
            for (check in checks) {
                val mismatch = check()
                if (mismatch != null) return mismatch
            }
            return null
        }
    }
}

