/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.loader

import org.jetbrains.kotlin.library.BaseKotlinLibrary
import org.jetbrains.kotlin.library.builtInsPlatform
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
        override fun check(library: BaseKotlinLibrary): PlatformCheckMismatch? {
            return checkPlatform(BuiltInsPlatform.NATIVE, library.builtInsPlatform)
                ?: checkTarget(BuiltInsPlatform.NATIVE, target, library.nativeTargets)
        }
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
        override fun check(library: BaseKotlinLibrary): PlatformCheckMismatch? {
            return checkPlatform(BuiltInsPlatform.WASM, library.builtInsPlatform)
                ?: checkTarget(BuiltInsPlatform.WASM, target, library.wasmTargets)
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
            if (expectedTarget == null || expectedTarget in actualTargets) return null

            return PlatformCheckMismatch(
                property = "target",
                expected = "${platform.name}[$expectedTarget]",
                actual = "${platform.name}[${actualTargets.joinToString()}]"
            )
        }
    }
}
