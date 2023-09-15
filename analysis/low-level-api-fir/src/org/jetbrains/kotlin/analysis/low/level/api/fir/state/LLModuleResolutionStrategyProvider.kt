/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtScriptModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

interface LLModuleResolutionStrategyProvider {
    /**
     * Returns [LLModuleResolutionStrategy.STATIC] if the [module] is treated as a binary for the current session,
     * and [LLModuleResolutionStrategy.LAZY] otherwise.
     *
     * In some cases, modules of the same type might be treated differently by the session, and have a different [LLModuleResolutionStrategy].
     * For instance, for a resolvable library session, only the target library is considered resolvable, and its dependencies are binary.
     */
    fun getKind(module: KtModule): LLModuleResolutionStrategy
}

/**
 * Specifies the way declarations are loaded and handled in the module.
 */
enum class LLModuleResolutionStrategy {
    /**
     * When a module is analyzed with a [LAZY] resolution strategy, its declarations might be in an unresolved
     * (or partially resolved) state. Call [lazyResolveToPhase] on the declarations before usage if needed.
     *
     * Some modules, such as [KtSourceModule] or [KtScriptModule], are always analyzed as [LAZY].
     * [KtLibraryModule] can be analyzed both ways (different types of sessions will be created).
     */
    LAZY,

    /**
     * With a [STATIC] resolution strategy, all declarations in a module are always considered fully resolved. Typically, they are loaded
     * from a binary storage, such as a JAR file, or a klib, where the complete type information is present.
     * Normally, declarations inside [STATIC] modules do not change. On a backing binary storage change, the whole session is invalided.
     */
    STATIC
}

/**
 * A resolution strategy that treats all modules but the [useSiteModule] as [LLModuleResolutionStrategy.STATIC].
 */
internal class LLSimpleResolutionStrategyProvider(private val useSiteModule: KtModule) : LLModuleResolutionStrategyProvider {
    override fun getKind(module: KtModule): LLModuleResolutionStrategy {
        return when (module) {
            useSiteModule -> LLModuleResolutionStrategy.LAZY
            else -> LLModuleResolutionStrategy.STATIC
        }
    }
}