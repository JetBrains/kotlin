/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.platform.wasm.isWasmJs
import org.jetbrains.kotlin.platform.wasm.isWasmWasi

/**
 * A test variant provider for multiplatform tests.
 *
 * The resulting list of prefixes is produced based on the target platform set in the test configuration.
 * The prefixes are sorted from the least specific prefix to the most specific.
 *
 * - For non-`COMMON` and non-`JVM` target platforms, [MultiplatformTestOutputPrefixProvider] will produce `knm`
 *   along with the target platform prefix. `knm` in this case represents all non-`JVM` platforms.
 *   For example, with the `JS` target platform provided, [MultiplatformTestOutputPrefixProvider] returns `["knm", "js"]`.
 *
 *     + Note that `WASM-JS` target uses two platform prefixes instead of one: `js` and `wasmJs`.
 *
 * - The `COMMON` platform doesn't have its own `common` prefix and is mapped to just `knm`.
 *
 * - When the `JVM` target platform is set, the provider returns an empty list.
 *   That's because the `JVM` platform is considered to be a default one and should not require additional variants.
 */
object MultiplatformTestOutputPrefixProvider {
    fun getPrefixes(targetPlatform: TargetPlatform): List<String> {
        if (targetPlatform.isJvm()) {
            return emptyList()
        }

        return buildList {
            val knmPrefix = "knm"
            add(knmPrefix)

            if (!targetPlatform.isCommon()) {
                val additionalPlatformPrefixes = when {
                    targetPlatform.isJs() -> listOf("js")
                    targetPlatform.isNative() -> listOf("native")
                    targetPlatform.isWasmJs() -> listOf("js", "wasmJs")
                    targetPlatform.isWasmWasi() -> listOf("wasmWasi")
                    else -> error("Unsupported platform $targetPlatform")
                }

                addAll(additionalPlatformPrefixes)
            }
        }
    }
}