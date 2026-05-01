/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal sealed interface Evt {
    val timestamp: Long
}

data class FirId(val id: String)

class PhaseEvt(val id: FirId, val phase: FirResolvePhase) : LLEvent, Evt {
    class CompletedSuccessfully(val id: FirId, val phase: FirResolvePhase) : Evt {
        override val timestamp: Long = System.nanoTime()
    }

    class CompletedWithFailure(val id: FirId, val phase: FirResolvePhase, val throwable: Throwable) : Evt {
        override val timestamp: Long = System.nanoTime()
    }

    override val timestamp: Long = System.nanoTime()

    override fun notifyCompleted() {
        LLEventRecorder.addEvent(CompletedSuccessfully(id, phase))
    }

    override fun notifyCompletedWithFailure(throwable: Throwable) {
        LLEventRecorder.addEvent(CompletedWithFailure(id, phase, throwable))
    }
}

@KaImplementationDetail
object LLEventRecorder : LLEventTracker {
    @OptIn(ExperimentalAtomicApi::class)
    @KaImplementationDetail
    var enabled: AtomicBoolean = AtomicBoolean(false)

    // thread safety: on the map itself we might get concurrent modifications,
    // but on the event lists themselves all operations will run on a single thread,
    // so no need for any additional synchronization
    @KaImplementationDetail
    private val events = ConcurrentHashMap<Thread, MutableList<Evt>>()

    override fun phase(
        target: FirElementWithResolveState,
        containingDeclarations: List<FirDeclaration>,
        requestedPhase: FirResolvePhase,
    ): LLEvent? {
        @OptIn(ExperimentalAtomicApi::class)
        if (!enabled.load()) return null

        val id = FirId(target.toString())
        return PhaseEvt(id, requestedPhase).also(::addEvent)
    }

    @KaImplementationDetail
    internal fun addEvent(evt: Evt) {
        @OptIn(ExperimentalAtomicApi::class)
        if (!enabled.load()) return

        events.getOrPut(Thread.currentThread()) { LinkedList<Evt>() }.add(evt)
    }
}
