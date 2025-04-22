/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative

/**
 * Represents the platform kind of implementation (non-common) module.
 * Unlike [TargetPlatform], here we discard specifics of platforms (such as the compatible JDK version).
 */
internal enum class ImplementationPlatformKind(private val matcher: (TargetPlatform) -> Boolean) {
    JVM({ it.isJvm() }),
    JAVASCRIPT({ it.isJs() }),
    NATIVE({ it.isNative() }),
    WASM({ it.isWasm() });

    /**
     * True if the given [targetPlatform] has a compatible implementation platform kind.
     */
    fun matches(targetPlatform: TargetPlatform): Boolean {
        return matcher(targetPlatform)
    }

    @KaImplementationDetail
    companion object {
        fun fromTargetPlatform(targetPlatform: TargetPlatform): ImplementationPlatformKind? {
            return entries.firstOrNull { it.matches(targetPlatform) }
        }
    }
}