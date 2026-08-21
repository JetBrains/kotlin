/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

/**
 * Join-semilattice used by [ForwardDataflowSolver].
 *
 * Implementations must provide a finite-height lattice: ascending chains under [join]
 * are finite, so the worklist algorithm terminates without an artificial iteration cap.
 */
interface Lattice<T> {
    fun bottom(): T
    fun join(a: T, b: T): T
    fun equivalent(a: T, b: T): Boolean
}

/**
 * Forward transfer: maps a block and its IN state to a (possibly distinct) OUT state
 * for each successor. Branch narrowing is expressed by returning different states per edge.
 */
fun interface TransferFunction<S> {
    fun transfer(block: JsBasicBlock, inn: S): Map<JsBasicBlock, S>
}

/**
 * Classic forward dataflow fixed-point over a [JsControlFlowGraph].
 *
 * Returns the IN state of every reachable block. Because [Lattice] has finite height,
 * the fixed point is reached in finitely many joins; iterationBudget is only an
 * assertion-guarded bug detector and must never be used to silently truncate results.
 */
class ForwardDataflowSolver {
    fun <S> solve(
        cfg: JsControlFlowGraph,
        entryState: S,
        lattice: Lattice<S>,
        transfer: TransferFunction<S>,
        iterationBudget: Int = cfg.blocks.size * 64 + 256,
    ): Map<JsBasicBlock, S> = solve(
        entry = cfg.entry,
        entryState = entryState,
        lattice = lattice,
        transfer = transfer,
        iterationBudget = iterationBudget,
        debugName = cfg.function.name.asString(),
    )

    fun <S> solve(
        entry: JsBasicBlock,
        entryState: S,
        lattice: Lattice<S>,
        transfer: TransferFunction<S>,
        iterationBudget: Int,
        debugName: String = "cfg",
    ): Map<JsBasicBlock, S> {
        val inStates = mutableMapOf<JsBasicBlock, S>()
        val worklist = ArrayDeque<JsBasicBlock>()
        inStates[entry] = entryState
        worklist.add(entry)

        var iterations = 0
        while (worklist.isNotEmpty()) {
            check(iterations++ < iterationBudget) {
                "Dataflow solver exceeded iteration budget ($iterationBudget) on $debugName; " +
                        "finite-height lattices must converge — this indicates a lattice/transfer bug"
            }
            val block = worklist.removeFirst()
            val inn = inStates[block] ?: lattice.bottom()
            val outs = transfer.transfer(block, inn)
            for (entry in outs.entries) {
                val succ = entry.key
                val outState = entry.value
                val previous = inStates[succ]
                val joined = if (previous == null) outState else lattice.join(previous, outState)
                if (previous == null || !lattice.equivalent(previous, joined)) {
                    inStates[succ] = joined
                    worklist.add(succ)
                }
            }
        }
        return inStates
    }
}
