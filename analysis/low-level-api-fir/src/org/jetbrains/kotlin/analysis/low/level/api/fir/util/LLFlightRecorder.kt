/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import jdk.jfr.*
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryFallbackDependenciesModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptDependencyModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLPartialBodyAnalysisState
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolveDesignationCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.transformers.PartialBodyAnalysisSuspendedException
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameterKind
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.utils.exceptions.shouldIjPlatformExceptionBeRethrown

private const val KOTLIN_CODE_ANALYSIS_EVENT_CATEGORY = "Kotlin Code Analysis"

@KaImplementationDetail
object LLFlightRecorder {
    private val includePhaseTraces: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        System.getProperty("kotlin.analysis.jfr.includePhaseTraces") == "true"
                || System.getenv("KOTLIN_ANALYSIS_JFR_INCLUDE_PHASE_TRACES") == "true"
    }

    private val phaseEventType = EventType.getEventType(LLPhaseEvent::class.java)
    private val phaseWithTraceEventType = EventType.getEventType(LLPhaseWithTraceEvent::class.java)

    /**
     * Notify that the [target] declaration was successfully analyzed up to the given [phase] (possibly partially).
     *
     * @param target The declaration being analyzed.
     * @param containingDeclarations The list of declarations enclosing [target] starting from the [FirFile].
     * @param phase The phase the declaration was analyzed to.
     */
    internal fun phase(
        target: FirElementWithResolveState,
        containingDeclarations: List<FirDeclaration>,
        requestedPhase: FirResolvePhase
    ): LLPhaseEventCompleter? {
        if (includePhaseTraces) {
            if (!phaseWithTraceEventType.isEnabled) {
                return null
            }

            return LLPhaseWithTraceEvent(
                path = path(containingDeclarations, target),
                hash = System.identityHashCode(target),
                phase = PHASE_COMPACT_NAMES[requestedPhase.ordinal],
                moduleKind = computeModuleKind(target)
            ).apply {
                begin()
            }
        } else {
            if (!phaseEventType.isEnabled) {
                return null
            }

            return LLPhaseEvent(
                path = path(containingDeclarations, target),
                hash = System.identityHashCode(target),
                phase = PHASE_COMPACT_NAMES[requestedPhase.ordinal],
                moduleKind = computeModuleKind(target)
            ).apply {
                begin()
            }
        }
    }

    private val partialBodyAnalysisEventType = EventType.getEventType(LLPartialBodyAnalysisEvent::class.java)

    /**
     * Notify that the [declaration]'s body is analyzed partially.
     *
     * @param declaration The declaration analyzed partially.
     * @param state The current partial analysis state of the [declaration].
     */
    internal fun partialBodyAnalyzed(declaration: FirElementWithResolveState, state: LLPartialBodyAnalysisState) {
        if (!partialBodyAnalysisEventType.isEnabled) {
            return
        }

        LLPartialBodyAnalysisEvent(
            hash = System.identityHashCode(declaration),
            count = state.analyzedPsiStatementCount,
            attempt = state.performedAnalysesCount
        ).commit()
    }

    private val readyPhaseEventType = EventType.getEventType(LLReadyPhaseEvent::class.java)

    /**
     * Notify that the [target] declaration was required to be analyzed up to the given [phase].
     * However, the declaration already reached it, so no work has been performed.
     *
     * Use `readyPhase(target, containingDeclarations, requestedPhase, withCallableMembers)` when you have the list of containing
     * declarations, e.g., from a [org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation].
     *
     * @param target The declaration being analyzed.
     * @param phase The phase the declaration is already analyzed to.
     */
    internal fun readyPhase(target: FirElementWithResolveState, requestedPhase: FirResolvePhase) {
        if (!readyPhaseEventType.isEnabled) {
            return
        }

        val designation = LLFirResolveDesignationCollector.getDesignationToResolve(target)?.designation ?: return

        LLReadyPhaseEvent(
            path = path(designation.path, target),
            hash = System.identityHashCode(target),
            phase = PHASE_COMPACT_NAMES[requestedPhase.ordinal],
            moduleKind = computeModuleKind(target)
        ).commit()
    }

    /**
     * Notify that the [target] declaration was required to be analyzed up to the given [phase].
     * However, the declaration already reached it, so no work has been performed.
     *
     * @param target The declaration being analyzed.
     * @param containingDeclarations The list of declarations enclosing [target] starting from the [FirFile].
     * @param phase The phase the declaration is already analyzed to.
     */
    internal fun readyPhase(
        target: FirElementWithResolveState,
        containingDeclarations: List<FirDeclaration>,
        requestedPhase: FirResolvePhase
    ) {
        if (!readyPhaseEventType.isEnabled) {
            return
        }

        LLReadyPhaseEvent(
            path = path(containingDeclarations, target),
            hash = System.identityHashCode(target),
            phase = PHASE_COMPACT_NAMES[requestedPhase.ordinal],
            moduleKind = computeModuleKind(target)
        ).commit()
    }

    private val phaseSuspensionEventType = EventType.getEventType(LLPhaseSuspensionEvent::class.java)

    /**
     * Notify that the current thread acknowledged the [declaration] is either finished analyzing up to [phase],
     * or got an exception, such as [com.intellij.openapi.progress.ProcessCanceledException].
     *
     * @param declaration The analyzed declaration.
     * @param phase The phase the [declaration] is being analyzed to.
     */
    internal fun phaseSuspension(declaration: FirElementWithResolveState, requestedPhase: FirResolvePhase): LLPhaseSuspensionEventCompleter? {
        if (!phaseSuspensionEventType.isEnabled) {
            return null
        }

        return LLPhaseSuspensionEvent(
            hash = System.identityHashCode(declaration),
            phase = PHASE_COMPACT_NAMES[requestedPhase.ordinal]
        ).apply {
            begin()
        }
    }

    private val stopWorldInvalidationEventType = EventType.getEventType(LLStopWorldInvalidation::class.java)

    /**
     * Notify that a stop-the-world session invalidation has been scheduled.
     */
    fun stopWorldSessionInvalidationScheduled() {
        stopWorldSessionInvalidation(newState = true)
    }

    /**
     * Notify that a stop-the-world session invalidation has been completed (either after being scheduled, or immediately).
     */
    fun stopWorldSessionInvalidationComplete() {
        stopWorldSessionInvalidation(newState = false)
    }

    private fun stopWorldSessionInvalidation(newState: Boolean) {
        if (!stopWorldInvalidationEventType.isEnabled) {
            return
        }

        LLStopWorldInvalidation(state = newState).commit()
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
            is FirEnumEntry -> "ee/" + declaration.name.asString()
            is FirField -> "fi/" + declaration.name.asString()
            is FirProperty -> "p/" + declaration.name.asString()
            is FirBackingField -> "bf/" + declaration.name.asString()
            is FirValueParameter -> {
                val kind = if (declaration.valueParameterKind == FirValueParameterKind.Regular) "vp/" else "cp/"
                kind + declaration.name.asString()
            }
            is FirVariable -> "v/" + declaration.name.asString() + "/${declaration::class.java.simpleName.lowercase()}"
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

    private fun path(containingDeclarations: List<FirDeclaration>, target: FirElementWithResolveState): String = buildString {
        for (entry in containingDeclarations) {
            append(name(entry))
            append(":")
        }
        append(name(target))
    }
}

private fun computeModuleKind(target: FirElementWithResolveState): Byte {
    val moduleData = target.moduleData as LLFirModuleData
    return when (moduleData.ktModule) {
        is KaSourceModule -> 0
        is KaDanglingFileModule -> 1
        is KaNotUnderContentRootModule -> 2
        is KaScriptModule -> 3
        is KaScriptDependencyModule -> 4
        is KaLibraryFallbackDependenciesModule -> 5
        is KaLibraryModule -> 6
        is KaLibrarySourceModule -> 7
        is KaBuiltinsModule -> 8
        else -> -1
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

internal interface LLPhaseEventCompleter {
    fun notifyCompleted()
    fun notifyCompletedWithFailure(throwable: Throwable)
}

@Suppress("unused")
@Name("org.jetbrains.kotlin.LLPhase")
@Category(KOTLIN_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("Kotlin Declaration Phase Execution")
@Description("A Kotlin declaration is analyzed to the specified FIR resolution phase (either successfully or with an error)")
@StackTrace(false)
private class LLPhaseEvent(
    @Label("Designation Path")
    private val path: String,

    @Label("Declaration Hash")
    private val hash: Int,

    @Label("Phase")
    private val phase: Byte,

    @Label("Module Kind")
    private val moduleKind: Byte
) : LLAbstractPhaseEvent() {
    @Label("Execution Result")
    @Description("0 - Success, 1 - Cancellation, 2 - Exception")
    override var result: Byte = -1
}

@Suppress("unused")
@Name("org.jetbrains.kotlin.LLPhaseWithTrace")
@Category(KOTLIN_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("Kotlin Declaration Phase Execution")
@Description("A Kotlin declaration is analyzed to the specified FIR resolution phase (either successfully or with an error)")
@StackTrace(true)
private class LLPhaseWithTraceEvent(
    @Label("Designation Path")
    private val path: String,

    @Label("Declaration Hash")
    private val hash: Int,

    @Label("Phase")
    private val phase: Byte,

    @Label("Module Kind")
    private val moduleKind: Byte
) : LLAbstractPhaseEvent() {
    @Label("Execution Result")
    @Description("0 - Success, 1 - Cancellation, 2 - Exception")
    override var result: Byte = -1
}

private abstract class LLAbstractPhaseEvent : Event(), LLPhaseEventCompleter {
    protected abstract var result: Byte

    override fun notifyCompleted() {
        result = 0
        end()
        commit()
    }

    override fun notifyCompletedWithFailure(throwable: Throwable) {
        result = when {
            throwable is PartialBodyAnalysisSuspendedException -> 0
            shouldIjPlatformExceptionBeRethrown(throwable) -> 1
            else -> 2
        }
        end()
        commit()
    }
}

@Suppress("unused")
@Name("org.jetbrains.kotlin.LLPartialBodyAnalysis")
@Category(KOTLIN_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("Kotlin Declaration Partial Body Analysis")
@Description("A Kotlin declaration's body is analyzed up to the specified PSI statement number (inclusive)")
@StackTrace(false)
private class LLPartialBodyAnalysisEvent(
    @Label("Declaration Hash")
    private val hash: Int,

    @Label("Analyzed Statement Count")
    private val count: Int,

    @Label("Analysis Attempt Number")
    private val attempt: Int
) : Event()

@Suppress("unused")
@Enabled(false) // The event is disabled by default due to the huge number of events
@Name("org.jetbrains.kotlin.LLReadyPhase")
@Category(KOTLIN_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("Ready Kotlin Declaration Analysis")
@Description("A Kotlin declaration is requested to be analyzed, yet the analysis have been already done")
@StackTrace(false)
private class LLReadyPhaseEvent(
    @Label("Designation path")
    private val path: String,

    @Label("Declaration Hash")
    private val hash: Int,

    @Label("Module Kind")
    private val moduleKind: Byte,

    @Label("Phase")
    private val phase: Byte
) : Event()

internal interface LLPhaseSuspensionEventCompleter {
    fun notifyCompleted()
}

@Suppress("unused")
@Name("org.jetbrains.kotlin.LLPhaseSuspension")
@Category(KOTLIN_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("Suspended Kotlin Declaration Analysis")
@Description("A Kotlin declaration analysis was suspended, as the other thread was already progressing with the same analysis")
@StackTrace(false)
private class LLPhaseSuspensionEvent(
    @Label("Declaration Hash")
    private val hash: Int,

    @Label("Phase")
    private val phase: Byte
) : Event(), LLPhaseSuspensionEventCompleter {
    override fun notifyCompleted() {
        end()
        commit()
    }
}

@Suppress("unused")
@Name("org.jetbrains.kotlin.LLStopWorldInvalidation")
@Category(KOTLIN_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("Stop-the-world Session Invalidation")
@Description("Stop-the-world session invalidation either has been requested, or it has just completed")
@StackTrace(false)
private class LLStopWorldInvalidation(
    @Label("Invalidation State")
    @Description("If true, the invalidation has been requested, otherwise it has completed")
    private val state: Boolean
) : Event()