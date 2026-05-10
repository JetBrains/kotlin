/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.platform.*
import org.jetbrains.kotlin.platform.jvm.JvmPlatform

/**
 * The module platform kind classifies a [KaModule]'s [TargetPlatform] into a single kind, determining how the Analysis API interprets the
 * module.
 *
 * It is either a single *kind of* concrete platform ([JVM], [JS], [WASM], [NATIVE]), or [METADATA] when the module's [TargetPlatform]
 * contains multiple *kinds of* concrete platforms.
 *
 * This classification determines:
 *
 *  - The kind of session built for the module and its platform-specific symbol loading.
 *    - For example, while we load Java symbols in a JVM session, we do not do the same in a JVM+Native metadata session. Java symbols
 *      cannot be accessed from Native, and so such symbols should not be loaded in a metadata context.
 *  - The module's expected layout (e.g., JAR vs. Klib library roots) and associated content scope restrictions for library modules.
 *
 * Platform-specific, composable configurations are instead applied based on the [TargetPlatform]'s component platforms.
 *
 * There is an important difference to [isMultiPlatform]: A target platform with multiple [JvmPlatform]s can be multiplatform, while the
 * module platform kind would still be [JVM] rather than [METADATA]. A [METADATA] module platform kind must always come from a target
 * platform with multiple *kinds of* component platforms.
 */
@KaPlatformInterface
public enum class KaModulePlatformKind {
    /**
     * Represents a metadata module.
     *
     * The underlying [TargetPlatform] has multiple kinds of component platforms.
     */
    METADATA,

    /**
     * Represents a JVM module.
     *
     * All component platforms of the underlying [TargetPlatform] are [JvmPlatform]s.
     */
    JVM,

    /**
     * Represents a JS module.
     *
     * All component platforms of the underlying [TargetPlatform] are [JsPlatform]s.
     */
    JS,

    /**
     * Represents a Wasm module.
     *
     * All component platforms of the underlying [TargetPlatform] are [WasmPlatform]s.
     *
     * This module platform kind covers both Wasm targets (JS and Wasi). While it could be argued that WasmJs and WasmWasi are totally
     * different platforms since they behave like different targets (e.g. different default imports and checkers), the current structure of
     * [WasmPlatform] treats the Wasm target as a second-layer abstraction. The platform kind follows that and currently has this single
     * value for Wasm regardless of the target.
     */
    WASM,

    /**
     * Represents a Native module.
     *
     * All component platforms of the underlying [TargetPlatform] are [NativePlatform]s.
     */
    NATIVE,
}

/**
 * @see KaModulePlatformKind
 */
@KaPlatformInterface
public fun TargetPlatform.toModulePlatformKind(): KaModulePlatformKind = when {
    all { it is JvmPlatform } -> KaModulePlatformKind.JVM
    all { it is JsPlatform } -> KaModulePlatformKind.JS
    all { it is WasmPlatform } -> KaModulePlatformKind.WASM
    all { it is NativePlatform } -> KaModulePlatformKind.NATIVE
    else -> KaModulePlatformKind.METADATA
}

/**
 * @see KaModulePlatformKind
 */
@KaPlatformInterface
public val KaModule.platformKind: KaModulePlatformKind
    get() = targetPlatform.toModulePlatformKind()
