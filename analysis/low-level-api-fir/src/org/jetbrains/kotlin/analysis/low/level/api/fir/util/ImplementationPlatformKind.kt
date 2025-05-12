/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative

/**
 * Represents the platform kind of implementation (non-common) module.
 * Unlike [TargetPlatform], here we discard specifics of platforms (such as the compatible JDK version).
 */
@KaImplementationDetail
enum class ImplementationPlatformKind(private val matcher: (TargetPlatform) -> Boolean) {
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

/**
 * Provides an implementation counterpart for common modules.
 */
@KaImplementationDetail
fun interface LLPlatformActualizer {
    /**
     * Returns the implementation module for the given [module], or `null` if there is no implementing module, or the given module
     * is not a common module.
     */
    fun actualize(module: KaModule): KaModule?
}

/**
 * A simple implementation of [LLPlatformActualizer] which returns the first module of the given [kind].
 */
@KaImplementationDetail
class LLKindBasedPlatformActualizer(private val kind: ImplementationPlatformKind) : LLPlatformActualizer {
    override fun actualize(module: KaModule): KaModule? {
        if (!module.targetPlatform.isCommon()) {
            return null
        }

        return KotlinProjectStructureProvider.getInstance(module.project)
            .getImplementingModules(module)
            .find { ImplementationPlatformKind.fromTargetPlatform(it.targetPlatform) == kind }
    }
}