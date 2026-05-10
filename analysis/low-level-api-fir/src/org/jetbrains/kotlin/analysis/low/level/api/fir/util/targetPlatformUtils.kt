/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.platform.*
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatformWithTarget
import org.jetbrains.kotlin.platform.wasm.WasmTarget

/**
 * Iterates over each component platform kind present in this [TargetPlatform], invoking the corresponding callback for each.
 *
 * This is the LL equivalent of
 * [AbstractFirMetadataSessionFactory.processPlatforms][org.jetbrains.kotlin.fir.session.AbstractFirMetadataSessionFactory.processPlatforms].
 */
internal inline fun TargetPlatform.forEachComponentPlatform(
    onJvm: () -> Unit,
    onJs: () -> Unit,
    onWasm: (WasmTarget) -> Unit,
    onNative: () -> Unit,
) {
    if (has<JvmPlatform>()) {
        onJvm()
    }

    if (has<JsPlatform>()) {
        onJs()
    }

    if (has<WasmPlatform>()) {
        val wasmTargets = subplatformsOfType<WasmPlatform>().mapTo(mutableSetOf()) { platform ->
            (platform as? WasmPlatformWithTarget)?.target ?: WasmTarget.JS
        }
        wasmTargets.forEach { onWasm(it) }
    }

    if (has<NativePlatform>()) {
        onNative()
    }
}
