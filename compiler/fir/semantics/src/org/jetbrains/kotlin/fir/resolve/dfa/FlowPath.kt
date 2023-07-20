/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeLabel

/**
 * Sealed class representing a type of path through a [Control Flow Graph (CFG) Node][org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode] used
 * for data flow analysis. Most CFG nodes only have a single data flow path through them, but there are times when a node may require
 * multiple paths to be calculated. The most common use case is for `finally` code blocks, where multiple code paths may enter, but these
 * paths diverge after exiting the code block. Consider the following (very) contrived example:
 *
 * ```kotlin
 * fun test() {
 *     var x: Any? = null
 *     while (true) {
 *         try {
 *             x = "" // (1)
 *             doSomething()
 *         } catch (e: Exception) {
 *             x = 1 // (2)
 *             break // (3)
 *         } finally {
 *             x.inc() // (4) Should be error.
 *             x.length // (5) Should be error.
 *         }
 *         x.length // (6) Should be OK.
 *     }
 *     x.inc() // (7) Should be OK.
 * }
 * ```
 *
 * A few things to note here:
 *
 * 1. The statement at (1) implies all following code can smartcast `x` to be a `String`.
 * 2. The statement at (2) implies all following code can smartcast `x` to be a `Int`.
 * 3. The statement at (3) is the only way to exit the infinite `while` loop without throwing an exception.
 * 4. Both of the statements at (4) and (5) should be considered errors because it is unknown through which path they will be reached. It is
 * possible to reach them only through (1) or after an exception is caught and going through (2).
 * 5. The statement at (6) should compile successfully since the only way to reach it is if the `try-catch-finally` expression completes
 * without exception, therefore only executing the statement at (1) and ***not*** (2).
 * 6. The statement at (7) should compile successfully since the only way to reach it is if an exception is caught and processed by the
 * `catch` block, therefore executing the statement at (2).
 *
 * To achieve this behavior, multiple data flows must be maintained through the `finally` code block for use by each path that comes after.
 * In the above example, 3 separate flows must be maintained:
 *
 * 1. A flow which will be used within the `finally` block itself. This data flow is a combination of all flows which lead into the
 * `finally` block.
 * 2. A flow which will be used when the entire `try` expression exits by jumping out of the while loop. This data flow is a continuation of
 * the data flow leading into the `finally` block from the `break` statement.
 * 3. A flow which will be used when the entire `try` expression exits without exception or jumping. This data flow is a continuation of the
 * data flow leading into the `finally` block from the main `try` block.
 */
sealed class FlowPath {
    /**
     * The [FlowPath] which represents the combination of all flows leading into a CFG Node.
     */
    data object Default : FlowPath()

    /**
     * The [FlowPath] which represents the combination of all flows leading into a CFG Node that follow an edge with the specified
     * [edge label][EdgeLabel]. The edge label is also combined with an [FIR element][FirElement] as the same edge label can be used for
     * multiple flows. For example, a `finally` block within another `finally` block will require multiple
     * [normal paths][org.jetbrains.kotlin.fir.resolve.dfa.cfg.NormalPath] through each that diverge at different nodes.
     */
    data class CfgEdge(val label: EdgeLabel, val fir: FirElement) : FlowPath()
}
