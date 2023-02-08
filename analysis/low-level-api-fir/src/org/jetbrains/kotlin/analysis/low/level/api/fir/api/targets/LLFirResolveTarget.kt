/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass


/**
 * Target to be lazily resolved by LL FIR lazy resolver.
 *
 * Specifies the path to the resolve targets and resolve targets themselves.
 * Those targets are going to be resolved by [org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirModuleLazyDeclarationResolver]
 */
sealed class LLFirResolveTarget {
    /**
     * [FirFile] where the targets are located
     */
    abstract val firFile: FirFile

    /**
     * The list of [FirRegularClass] which are the required to go from file to target declarations in the top-down order.
     *
     * If resolve target is [FirRegularClass] itself, it's not included into the [path]
     */
    abstract val path: List<FirRegularClass>

    /**
     * Executions the [action] for each target that this [LLFirResolveTarget] represents.
     */
    abstract fun forEachTarget(action: (FirElementWithResolveState) -> Unit)

    override fun toString(): String = buildString {
        append(this::class.simpleName)
        append("(")
        buildList {
            add(firFile.name)
            path.mapTo(this) { it.name.asString() }
            add(toStringForTarget())
        }.joinTo(this, separator = " -> ")
        append(")")
    }

    protected abstract fun toStringForTarget(): String
}
