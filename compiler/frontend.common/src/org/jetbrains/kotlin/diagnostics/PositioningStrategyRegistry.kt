/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

/** A registry of the implementations of [PositioningStrategyKind]s for a concrete parse tree. */
abstract class PositioningStrategyRegistry<S : PositioningStrategy> {
    val kindToStrategy: Map<PositioningStrategyKind, S>
        field: MutableMap<PositioningStrategyKind, S> = mutableMapOf()

    operator fun get(kind: PositioningStrategyKind): S =
        kindToStrategy[kind] ?: error("No strategy is registered in ${this::class.simpleName} for ${kind::class.simpleName}")

    /** The function to register an implementation, plugins can extend strategies in the future. */
    protected fun <T : S> T.registerAs(kind: PositioningStrategyKind): T {
        require(kind !in kindToStrategy) { "${kind::class.simpleName} is already registered" }
        kindToStrategy[kind] = this
        return this
    }
}
