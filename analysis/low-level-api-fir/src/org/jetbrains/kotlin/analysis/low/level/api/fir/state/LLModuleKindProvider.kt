/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

interface LLModuleKindProvider {
    /**
     * Returns [KtModuleKind.BINARY_MODULE] if the [module] is treated as a binary for the current session,
     * and [KtModuleKind.RESOLVABLE_MODULE] otherwise.
     *
     * In some cases, modules of the same type might be treated differently by the session, and have a different [KtModuleKind].
     * For instance, for a resolvable library session, only the target library is considered resolvable, and its dependencies are binary.
     */
    fun getKind(module: KtModule): KtModuleKind
}

/**
 * Specifies the way declarations are loaded and handled in the module.
 */
enum class KtModuleKind {
    /**
     * Declarations in resolvable modules might be in an unresolved state.
     * Call [lazyResolveToPhase] on them before usage if needed.
     */
    RESOLVABLE_MODULE,

    /**
     * Declarations in binary modules always come fully resolved, as they are loaded from JAR/klib, where the complete type information
     * is available.
     */
    BINARY_MODULE
}