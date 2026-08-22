/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.grouping

import org.jetbrains.kotlin.test.GroupingTestRunner
import org.jetbrains.kotlin.test.NonGroupingTestRunner

/**
 * This is a base class for all two-stage compiler tests which are executed by the [CompilerTestGroupingTestEngine].
 */
abstract class AbstractTwoStageKotlinCompilerTestBase {
    abstract val nonGroupingRunner: NonGroupingTestRunner
    abstract val nonGroupingStageRunnerInitialized: Boolean

    abstract val groupingStageRunner: GroupingTestRunner
    abstract val secondStageRunnerInitialized: Boolean

    /**
     * Identifies which other generated test classes' tests are allowed to share a [CompilerTestGroupingTestEngine]
     * batch with this one. Defaults to this instance's outermost enclosing class (e.g. `WasmJsCodegenBoxTestGenerated`
     * itself, never a `@Nested` class within it) - the strictest, always-safe choice, see `BatchKey` in
     * [CompilerTestGroupingTestEngine] for why a generated class boundary matters at all.
     *
     * The outermost class, rather than `this::class`, is the right default: [AbstractTwoStageKotlinCompilerTestBase]
     * is normally implemented only by the outermost generated class, with `@Nested` classes underneath it left as
     * plain JUnit containers, so `this` here is already that outermost instance in every suite using this engine
     * today. But that is an incidental property of how those suites happen to be structured, not something this
     * class enforces - a `@Nested` class that *did* implement [AbstractTwoStageKotlinCompilerTestBase] itself would
     * silently fragment batches by nested class too. Walking up to the outermost enclosing class explicitly keeps
     * the "one generated class == one default scope" invariant true regardless of which class in the nesting chain
     * JUnit happens to hand back as this instance.
     *
     * Override this to a value shared by a whole family of generated classes only after verifying that every
     * member of the family configures an identical grouping-stage runner (facade, handlers, target) *and* an
     * identical non-grouping stage (transformers, configurators) - the engine has no generic way to check this
     * for you, and getting it wrong silently compiles some of the batch's tests with the wrong configuration
     * instead of failing loudly. Prefer a single shared, named marker (e.g. an `object`) over deriving one
     * structurally from each class, since structural derivation is exactly the comparison that isn't safe to
     * automate here.
     */
    open val groupingBatchScope: Any get() = this::class.java.outermostEnclosingClass()
}

private tailrec fun Class<*>.outermostEnclosingClass(): Class<*> = enclosingClass?.outermostEnclosingClass() ?: this
