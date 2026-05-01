/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirId.TopLevelFunction.Companion.stringifyValueParameters
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@KaImplementationDetail
sealed interface Evt {
    val timestamp: Long
}

@KaImplementationDetail
sealed interface FirId {
    fun findElement(symbolProvider: FirSymbolProvider): FirElementWithResolveState?

    class TopLevelFunction(val packageFqName: FqName, val name: Name, val params: String) : FirId {
        override fun findElement(symbolProvider: FirSymbolProvider): FirElementWithResolveState? {
            return symbolProvider.getTopLevelFunctionSymbols(packageFqName, name)
                .find { f -> stringifyValueParameterSymbols(f.valueParameterSymbols) == params }
                ?.fir
        }

        override fun toString(): String {
            return "$packageFqName.$name$params"
        }

        companion object {
            fun stringifyValueParameters(
                params: List<FirValueParameter>,
            ): String {
                return params.joinToString(prefix = "(", postfix = ")") { it.name.asString() }
            }

            fun stringifyValueParameterSymbols(
                params: List<FirValueParameterSymbol>,
            ): String {
                return params.joinToString(prefix = "(", postfix = ")") { it.name.asString() }
            }
        }
    }

    companion object {
        fun of(containingDeclarations: List<FirDeclaration>, target: FirElementWithResolveState): FirId? {
            return when (target) {
                is FirFunction -> {
                    if (target.isLocal) {
                        return null
                    }

                    val psiFile = target.psi?.containingFile!! as KtFile
                    val packageFqName = psiFile.packageFqName

                    val name = target.nameOrSpecialName

                    TopLevelFunction(packageFqName, name, stringifyValueParameters(target.valueParameters))
                }
                else -> null
            }
        }
    }
}

@KaImplementationDetail
class PhaseEvt(val id: FirId, val phase: FirResolvePhase) : LLEvent {
    @KaImplementationDetail
    class CompletedSuccessfully(val id: FirId, val phase: FirResolvePhase) : Evt {
        override val timestamp: Long = System.nanoTime()

        override fun toString(): String =
            "CompletedSuccessfully(id=$id, phase=$phase)"
    }

    @KaImplementationDetail
    class CompletedWithFailure(val id: FirId, val phase: FirResolvePhase, val throwable: Throwable) : Evt {
        override val timestamp: Long = System.nanoTime()

        override fun toString(): String =
            "CompletedWithFailure(id=$id, phase=$phase, throwable=$throwable)"
    }

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
    private var enabled: AtomicBoolean = AtomicBoolean(false)

    // thread safety: on the map itself we might get concurrent modifications,
    // but on the event lists themselves all operations will run on a single thread,
    // so no need for any additional synchronization
    @KaImplementationDetail
    private val events = ConcurrentHashMap<Thread, MutableList<Evt>>()

    @KaImplementationDetail
    fun enable() {
        @OptIn(ExperimentalAtomicApi::class)
        enabled.store(true)
    }

    @KaImplementationDetail
    fun disable() {
        @OptIn(ExperimentalAtomicApi::class)
        enabled.store(false)
    }

    override fun phase(
        target: FirElementWithResolveState,
        containingDeclarations: List<FirDeclaration>,
        requestedPhase: FirResolvePhase,
    ): LLEvent? {
        @OptIn(ExperimentalAtomicApi::class)
        if (!enabled.load()) return null

        val id = FirId.of(containingDeclarations, target) ?: return null
        return PhaseEvt(id, requestedPhase)
    }

    @KaImplementationDetail
    fun clearedCache() {
        @OptIn(ExperimentalAtomicApi::class)
        if (!enabled.load()) return

        events.clear()
    }

    @KaImplementationDetail
    fun serialize(): List<Evt> {
        val nextIndices = mutableMapOf<Thread, Int>()
        val result = mutableListOf<Evt>()
        events.keys.forEach { nextIndices[it] = 0 }
        while (true) {
            val (thread, threadEvents) = events.asSequence().filter { (nextIndices[it.key] ?: 0) < it.value.size }
                .minByOrNull { it.value[nextIndices[it.key]!!].timestamp } ?: break
            val index = nextIndices[thread]!!
            result.add(threadEvents[index])
            nextIndices[thread] = index + 1
        }
        return result
    }

    @RequiresReadLock
    fun runOrdered(events: List<Evt>, symbolProvider: FirSymbolProvider) {
        events.forEach { rerun(it, symbolProvider) }
    }

    @RequiresReadLock
    private fun rerun(evt: Evt, symbolProvider: FirSymbolProvider) {
        when (evt) {
            is PhaseEvt.CompletedSuccessfully -> {
                val el = evt.id.findElement(symbolProvider)
                    ?: throw IllegalStateException("Element not found")

                el.lazyResolveToPhase(evt.phase)
            }
            is PhaseEvt.CompletedWithFailure -> TODO()
        }
    }

    @KaImplementationDetail
    internal fun addEvent(evt: Evt) {
        @OptIn(ExperimentalAtomicApi::class)
        if (!enabled.load()) return

        events.getOrPut(Thread.currentThread()) { LinkedList<Evt>() }.add(evt)
    }
}
