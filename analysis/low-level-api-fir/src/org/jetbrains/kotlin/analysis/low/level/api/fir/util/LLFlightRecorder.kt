/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.openapi.progress.ProcessCanceledException
import jdk.jfr.*
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLPartialBodyAnalysisState
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolveDesignationCollector
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName

@KaImplementationDetail
object LLFlightRecorder {
    /**
     * Notify that the [target] declaration was successfully analyzed up to the given [phase] (possibly partially).
     *
     * @param target The declaration being analyzed.
     * @param phase The phase the declaration was analyzed to.
     */
    internal fun phase(target: LLFirResolveTarget, phase: FirResolvePhase): ILLPhaseEvent? {
        val event = LLPhaseEvent()
        if (event.isEnabled) {
            event.path = path(target.designation)
            event.hash = System.identityHashCode(target.target)
            event.phase = PHASE_COMPACT_NAMES[phase.ordinal]
            return event
        }

        return null
    }

    /**
     * Notify that the [file] was successfully analyzed up to the given [phase].
     *
     * @param file The file being analyzed.
     * @param phase The phase the file was analyzed to.
     */
    internal fun phase(file: FirFile, phase: FirResolvePhase): ILLPhaseEvent? {
        val event = LLPhaseEvent()
        if (event.isEnabled) {
            event.path = name(file)
            event.hash = System.identityHashCode(file)
            event.phase = PHASE_COMPACT_NAMES[phase.ordinal]
            return event
        }

        return null
    }

//    /**
//     * Notify that analysis of the [target] declaration was interrupted by an exception.
//     *
//     * @param target The declaration being analyzed.
//     * @param phase The phase the declaration was being analyzed to.
//     * @param startNanos The time the analysis started, in nanoseconds, as returned by [System.nanoTime].
//     * @param throwable The exception that interrupted the analysis.
//     */
//    internal fun interruptedPhase(target: LLFirResolveTarget, phase: FirResolvePhase, startNanos: Long, throwable: Throwable) {
//        val event = LLInterruptedPhaseEvent()
//        if (event.isEnabled) {
//            event.path = path(target.designation)
//            event.hash = System.identityHashCode(target.target)
//            event.phase = PHASE_COMPACT_NAMES[phase.ordinal]
//            event.duration = System.nanoTime() - startNanos
//            event.failure = when (throwable) {
//                is ProcessCanceledException -> 1
//                else -> 0
//            }
//            event.commit()
//        }
//    }
//
//    /**
//     * Notify that analysis of the [file] was interrupted by an exception.
//     *
//     * @param file The file being analyzed.
//     * @param phase The phase the file was being analyzed to.
//     * @param startNanos The time the analysis started, in nanoseconds, as returned by [System.nanoTime].
//     * @param throwable The exception that interrupted the analysis.
//     */
//    internal fun interruptedPhase(file: FirFile, phase: FirResolvePhase, startNanos: Long, throwable: Throwable) {
//        val event = LLInterruptedPhaseEvent()
//        if (event.isEnabled) {
//            event.path = name(file)
//            event.hash = System.identityHashCode(file)
//            event.phase = PHASE_COMPACT_NAMES[phase.ordinal]
//            event.duration = System.nanoTime() - startNanos
//            event.failure = when (throwable) {
//                is ProcessCanceledException -> 1
//                else -> 0
//            }
//            event.commit()
//        }
//    }

    /**
     * Notify that the [declaration]'s body is analyzed partially.
     *
     * @param declaration The declaration analyzed partially.
     * @param state The current partial analysis state of the [declaration].
     */
    internal fun partialBodyAnalyzed(declaration: FirElementWithResolveState, state: LLPartialBodyAnalysisState) {
        val event = LLPartialBodyAnalysisEvent()
        if (event.isEnabled) {
            event.hash = System.identityHashCode(declaration)
            event.count = state.analyzedPsiStatementCount
            event.attempt = state.performedAnalysesCount
            event.commit()
        }
    }

    /**
     * Notify that the [declaration] was required to be analyzed up to the given [phase].
     * However, the [declaration] already reached it, so no work has been performed.
     *
     * @param declaration The declaration being analyzed.
     * @param phase The phase the [declaration] is already analyzed to.
     * @param withCallableMembers Whether the analysis should have also included callable members.
     */
    internal fun readyPhase(declaration: FirElementWithResolveState, phase: FirResolvePhase, withCallableMembers: Boolean) {
        val event = LLReadyPhaseEvent()
        if (event.isEnabled) {
            val designation = LLFirResolveDesignationCollector.getDesignationToResolve(declaration)?.designation ?: return
            event.path = path(designation)
            event.hash = System.identityHashCode(designation.target)
            event.phase = PHASE_COMPACT_NAMES[phase.ordinal]
            event.withMembers = withCallableMembers
            event.commit()
        }
    }

    /**
     * Notify that the current thread acknowledged the [declaration] is either finished analyzing up to [phase],
     * or got an exception, such as [com.intellij.openapi.progress.ProcessCanceledException].
     *
     * @param declaration The analyzed declaration.
     * @param phase The phase the [declaration] is being analyzed to.
     */
    internal fun phaseSuspension(declaration: FirElementWithResolveState, phase: FirResolvePhase): ILLPhaseSuspensionEvent? {
        val event = LLPhaseSuspensionEvent()
        if (event.isEnabled) {
            event.hash = System.identityHashCode(declaration)
            event.phase = PHASE_COMPACT_NAMES[phase.ordinal]
            return event
        }

        return null
    }

    fun stopWorldSessionInvalidationScheduled() {
        stopWorldSessionInvalidation(state = true)
    }

    fun stopWorldSessionInvalidationComplete() {
        stopWorldSessionInvalidation(state = false)
    }

    private fun stopWorldSessionInvalidation(state: Boolean) {
        val event = LLStopWorldInvalidation()
        if (event.isEnabled) {
            event.state = state
            event.commit()
        }
    }

    private fun name(declaration: FirElementWithResolveState): String {
        /**
         * As [name] is used as a component of [path], names must not contain colons.
         * So theoretically, we should escape/substitute all colon characters.
         * However, colons are forbidden in JVM bytecode, and overall, the chance that we find them is considerably low.
         */
        @Suppress("SpellCheckingInspection")
        return when (declaration) {
            is FirFile -> "fl/" + declaration.name
            is FirScript -> "s/" + declaration.name.asString()
            is FirTypeParameter -> "tp/" + declaration.name.asString()
            is FirTypeAlias -> "ta/" + declaration.classId.asString()
            is FirClass -> "c/" + declaration.classId.asString()
            is FirVariable -> "v/" + declaration.name.asString()
            is FirPropertyAccessor -> (if (declaration.isGetter) "pg/" else "ps/") + declaration.propertySymbol.name.asString()
            is FirConstructor -> "ctor/" + signature(declaration)
            is FirAnonymousFunction -> "lambda"
            is FirFunction -> {
                val baseName = "f/" + declaration.nameOrSpecialName.asString()
                baseName + '/' + signature(declaration)
            }
            is FirReplSnippet -> "repl"
            is FirCodeFragment -> "code"
            is FirReceiverParameter -> "recv"
            is FirDanglingModifierList -> "dml"
            is FirAnonymousInitializer -> "init"
            else -> "?/" + declaration.javaClass.simpleName
        }
    }

    private fun signature(declaration: FirFunction): String {
        return declaration.valueParameters.joinToString(",") { it.name.asString() }
    }

    private fun path(designation: FirDesignation): String = buildString {
        for (entry in designation.path) {
            append(name(entry))
            append(":")
        }
        append(name(designation.target))
    }
}

/**
 *                  !!!
 * When adding or removing phases, use unused numbers.
 * Never change existing mappings!
 */
private val PHASE_COMPACT_NAMES = run {
    val phases = FirResolvePhase.entries
    ByteArray(phases.size) {
        when (phases[it]) {
            FirResolvePhase.RAW_FIR -> 0
            FirResolvePhase.IMPORTS -> 1
            FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS -> 2
            FirResolvePhase.COMPANION_GENERATION -> 3
            FirResolvePhase.SUPER_TYPES -> 4
            FirResolvePhase.SEALED_CLASS_INHERITORS -> 5
            FirResolvePhase.TYPES -> 6
            FirResolvePhase.STATUS -> 7
            FirResolvePhase.EXPECT_ACTUAL_MATCHING -> 8
            FirResolvePhase.CONTRACTS -> 9
            FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> 10
            FirResolvePhase.CONSTANT_EVALUATION -> 11
            FirResolvePhase.ANNOTATION_ARGUMENTS -> 12
            FirResolvePhase.BODY_RESOLVE -> 13
        }
    }
}

internal interface ILLEvent {
    fun end()
}

internal interface ILLPhaseEvent : ILLEvent {
    fun endWithFailure(throwable: Throwable)
}

@Name("org.jetbrains.kotlin.LLPhase")
@Category("Kotlin Code Analysis")
@Label("Successful analysis of a Kotlin declaration")
@Description("A Kotlin declaration is successfully analyzed to the specified FIR resolution phase")
@StackTrace(false)
private class LLPhaseEvent : Event(), ILLPhaseEvent {
    @JvmField
    @Label("Declaration designation path")
    var path: String = ""

    @JvmField
    @Label("Declaration identity hash")
    var hash: Int = -1

    @JvmField
    @Label("Phase")
    var phase: Byte = -1

    @JvmField
    @Label("Failure reason (1 – cancellation, 2 – unknowne exception, 0 – successful analysis)")
    var result: Byte = -1

    override fun endWithFailure(throwable: Throwable) {
        result = when {
            throwable is ProcessCanceledException -> 1
            else -> 0
        }

        end()
    }
}

@Name("org.jetbrains.kotlin.LLPartialBodyAnalysis")
@Category("Kotlin Code Analysis")
@Label("Successful partial body analysis of a Kotlin callable declaration")
@Description("A Kotlin declaration's body is analyzed up to the specified PSI statement number (inclusive)")
@StackTrace(false)
private class LLPartialBodyAnalysisEvent : Event() {
    @JvmField
    @Label("Declaration identity hash")
    var hash: Int = -1

    @JvmField
    @Label("Analyzed statement count")
    var count: Int = -1

    @JvmField
    @Label("Analysis attempt number")
    var attempt: Int = -1
}

@Name("org.jetbrains.kotlin.LLReadyPhase")
@Category("Kotlin Code Analysis")
@Label("Kotlin declaration analysis is requested but already done")
@Description("A Kotlin declaration is requested to be analyzed, yet the analysis have been already done")
@StackTrace(false)
private class LLReadyPhaseEvent : Event() {
    @JvmField
    @Label("Declaration designation path")
    var path: String = ""

    @JvmField
    @Label("Declaration identity hash")
    var hash: Int = -1

    @JvmField
    @Label("Phase")
    var phase: Byte = -1

    @JvmField
    @Label("Whether analysis with callable members is requested")
    var withMembers: Boolean = false
}

internal interface ILLPhaseSuspensionEvent : ILLEvent

@Name("org.jetbrains.kotlin.LLPhaseSuspension")
@Category("Kotlin Code Analysis")
@Label("Kotlin declaration analysis was temporarily suspended")
@Description("A Kotlin declaration analysis was suspended, as the other thread was already progressing with the same analysis")
@StackTrace(false)
private class LLPhaseSuspensionEvent : Event(), ILLPhaseSuspensionEvent {
    @JvmField
    @Label("Declaration identity hash")
    var hash: Int = -1

    @JvmField
    @Label("Phase")
    var phase: Byte = -1
}

@Name("org.jetbrains.kotlin.LLStopWorldInvalidation")
@Category("Kotlin Code Analysis")
@Label("Stop-the-world session invalidation state has changed")
@Description("stop-the-world session invalidation either has been requested, or it has just completed")
@StackTrace(false)
private class LLStopWorldInvalidation : Event() {
    @JvmField
    @Label("Invalidation state ('true' if started, 'false' if finished)")
    var state: Boolean = false
}