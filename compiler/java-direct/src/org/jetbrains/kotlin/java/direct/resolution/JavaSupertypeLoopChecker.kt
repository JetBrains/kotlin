/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct.resolution

import org.jetbrains.kotlin.name.ClassId

/**
 * Per-[JavaResolutionContext], thread-local active-`ClassId` set that bounds inheritance
 * cycles on every supertype-walking entry point.
 *
 * Step 4.5a deliverable per
 * [implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md] §6.1.
 *
 * Models K1's `SupertypeLoopChecker` (`core/descriptors.jvm/.../context.kt:57`) and FIR
 * Kotlin's `SupertypeComputationStatus.Computing` sentinel
 * (`compiler/fir/resolve/.../FirSupertypesResolution.kt:355,844`). Subsumes the per-call
 * `visited<ClassId>` parameters that the old BFS threaded through every recursive call.
 *
 * Two responsibilities:
 *  1. **Terminate.** On re-entry to a [ClassId] already on the active set, [guarded]
 *     returns the supplied default and records the offending edge.
 *  2. **Report.** The recorded `(parentClassId, supertypeClassId)` edges are exposed via
 *     [consumeCycleEdges] so a later iteration can surface them as
 *     `CYCLIC_INHERITANCE_HIERARCHY` via `DiagnosticKind.LoopInSupertype` (Step 4.5a's
 *     diagnostic-emission wiring is documented in §12 Q4 of the proposal; the wiring
 *     itself is left to the iteration that owns `FirJavaClass.computeSuperTypeRefsByJavaClass`).
 *
 * **Threading invariant.** The active set lives per-thread, per-[JavaResolutionContext].
 * A Kotlin lookup that bounces back through `firSession.symbolProvider` and ultimately
 * re-asks the model for a Java class will hit the active set on the *same thread* —
 * provided the model never spawns supertype walks on a different thread.
 */
internal class JavaSupertypeLoopChecker {
    private val resolving: ThreadLocal<ArrayDeque<ClassId>> =
        ThreadLocal.withInitial { ArrayDeque() }

    private val cycleEdges: MutableList<Pair<ClassId, ClassId>> = mutableListOf()

    /**
     * Wraps a supertype-walking computation against re-entry on [classId].
     *
     * If [classId] is already on the active set on the current thread, returns [default]
     * without invoking [block] and records the offending `(parent, classId)` edge for
     * later diagnostic emission (where `parent` is the most recently entered class).
     *
     * Otherwise, pushes [classId] onto the active set, invokes [block], and pops [classId]
     * back off in a `finally` (so an exception in [block] does not corrupt the set).
     */
    inline fun <R> guarded(classId: ClassId, default: R, block: () -> R): R {
        val active = resolving.get()
        if (classId in active) {
            recordCycleEdge(active.lastOrNull(), classId)
            return default
        }
        active.addLast(classId)
        try {
            return block()
        } finally {
            active.removeLast()
        }
    }

    /**
     * Records a cycle edge for later diagnostic emission. [parent] is the class whose
     * supertype walk encountered the re-entry; if there is no enclosing walk on the stack
     * (a self-cycle entered as the first call), [parent] is `null` and we use [child] for
     * both ends of the edge.
     */
    @PublishedApi
    internal fun recordCycleEdge(parent: ClassId?, child: ClassId) {
        synchronized(cycleEdges) {
            cycleEdges.add((parent ?: child) to child)
        }
    }

    /**
     * Returns and clears the recorded cycle edges. Intended to be called after a top-level
     * supertype walk so the FIR side can synthesize `LoopInSupertype` `FirErrorTypeRef`s
     * mirroring `breakLoops` in `FirSupertypesResolution.kt:825,844`.
     */
    fun consumeCycleEdges(): List<Pair<ClassId, ClassId>> {
        synchronized(cycleEdges) {
            if (cycleEdges.isEmpty()) return emptyList()
            val snapshot = cycleEdges.toList()
            cycleEdges.clear()
            return snapshot
        }
    }
}
