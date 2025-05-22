/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintsLogger
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.FixationLogRecord
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.FixationLogVariableInfo
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.TypeVariableFixationReadiness
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.inference.model.InitialConstraint
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.types.model.TypeVariableMarker

open class FirConstraintsLogger : ConstraintsLogger(), FirSessionComponent {
    sealed class LoggingElement

    class Call(
        val fir: FirElement,
        val render: String = fir.render(),
    )

    sealed class BlockOwner {
        class Candidate(
            val candidate: org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate,
            val owningCall: Call,
        ) : BlockOwner()

        /**
         * A fallback for those cases when the constraints system is used without
         * a preceding clarification call like `logCandidate()`.
         */
        object Unknown : BlockOwner()
    }

    open class StageBlockElement(
        val name: String,
        val elements: MutableList<StageElement> = mutableListOf(),
        val owner: BlockOwner,
    ) : LoggingElement()

    sealed class StageElement() : LoggingElement()

    class NewVariableElement(val variable: TypeVariableMarker) : StageElement()

    class ErrorElement(val error: ConstraintSystemError) : StageElement()

    sealed class ConstraintElement(val previous: List<ConstraintElement>) : StageElement()

    class InitialConstraintElement(val constraint: String, val position: String) : ConstraintElement(emptyList())

    class IncorporatedConstraintElement(
        val constraint: String,
        previous: List<ConstraintElement>,
    ) : ConstraintElement(previous)

    class ConstraintSubstitutionElement(val constraint: String) : ConstraintElement(emptyList())

    class FixationLogRecordElement(val record: FixationLogRecord) : StageElement()

    val topLevelElements: MutableList<StageBlockElement> = mutableListOf<StageBlockElement>()

    override var currentContext: TypeSystemInferenceExtensionContext? = null

    private lateinit var currentBlock: StageBlockElement

    private val contextToKnownBlock: MutableMap<TypeSystemInferenceExtensionContext, StageBlockElement> = mutableMapOf()
    private val contextToCandidate: MutableMap<TypeSystemInferenceExtensionContext, BlockOwner.Candidate> = mutableMapOf()

    override fun verifyContext(context: TypeSystemInferenceExtensionContext) {
        if (context == currentContext) return
        updateCurrentContext(context)

        val knownPreviousBlock = contextToKnownBlock[context]
        val nextBlockTitle = when {
            knownPreviousBlock != null && currentCandidate != null -> "Continue " + knownPreviousBlock.name
            else -> error("UNKNOWN CONTEXT")
        }
        currentBlock = StageBlockElement(nextBlockTitle, owner = currentCandidate ?: BlockOwner.Unknown).apply { register(context) }
    }

    private fun updateCurrentContext(context: TypeSystemInferenceExtensionContext) {
        if (context == currentContext) return
        currentContext = context

        currentCandidate = contextToCandidate[context]
        currentCall = currentCandidate?.owningCall
    }

    private fun StageBlockElement.register(context: TypeSystemInferenceExtensionContext) {
        if (topLevelElements.lastOrNull()?.elements?.isEmpty() == true) {
            topLevelElements.removeLast()
        }

        contextToKnownBlock[context] = this
        topLevelElements += this
    }

    override lateinit var currentState: State

    private val knownConstraintsCache = mutableMapOf<Any, ConstraintElement>()

    private fun cachedElementFor(constraint: InitialConstraint) =
        knownConstraintsCache[constraint]
            ?: error("This constraint has not yet been logged: $constraint")

    private fun cachedElementFor(variable: TypeVariableMarker, constraint: Constraint) =
        knownConstraintsCache[variable to constraint]
            ?: error("This constraint has not yet been logged: $variable with $constraint")

    private var currentCall: Call? = null
    private var currentCandidate: BlockOwner.Candidate? = null

    fun logCandidate(candidate: Candidate) {
        // A candidate may have not been processed in one go.
        // See `fullyProcessCandidate()`.
        if (currentCandidate?.candidate !== candidate) {
            val candidateOwner = BlockOwner.Candidate(candidate, Call(candidate.callInfo.callSite))
            contextToCandidate[candidate.system] = candidateOwner
            currentCandidate = candidateOwner
            currentContext = candidate.system
        }
    }

    fun logStage(name: String, context: TypeSystemInferenceExtensionContext) {
        updateCurrentContext(context)
        currentBlock = StageBlockElement(name, owner = currentCandidate ?: BlockOwner.Unknown).apply { register(context) }
        currentState = State(this)
    }

    private val currentStageElements: MutableList<StageElement>
        get() = currentBlock.elements

    override fun logInitial(constraint: InitialConstraint, context: TypeSystemInferenceExtensionContext) {
        verifyContext(context)
        // The constraint position must be rendered right away, because it may contain
        // FIR renders, and the FIR may have changed by the time we finish inference.
        val element = InitialConstraintElement(formatConstraint(constraint), sanitizeFqNames(constraint.position.toString()))
        knownConstraintsCache.putIfAbsent(constraint, element)
        currentStageElements += element
    }

    override fun log(variable: TypeVariableMarker, constraint: Constraint, context: TypeSystemInferenceExtensionContext) {
        verifyContext(context)
        val element = IncorporatedConstraintElement(formatConstraint(variable, constraint), currentState.previous)
        knownConstraintsCache.putIfAbsent(variable to constraint, element)
        currentStageElements += element
    }

    override fun logConstraintSubstitution(
        variable: TypeVariableMarker,
        substitutedConstraint: Constraint,
        context: TypeSystemInferenceExtensionContext,
    ) {
        verifyContext(context)
        val element = ConstraintSubstitutionElement(formatConstraint(variable, substitutedConstraint))
        knownConstraintsCache.putIfAbsent(variable to substitutedConstraint, element)
        currentStageElements += element
    }

    override fun logError(error: ConstraintSystemError, context: TypeSystemInferenceExtensionContext) {
        verifyContext(context)
        currentStageElements.add(ErrorElement(error))
    }

    override fun logNewVariable(variable: TypeVariableMarker, context: TypeSystemInferenceExtensionContext) {
        verifyContext(context)
        currentStageElements.add(NewVariableElement(variable))
    }

    override fun logReadiness(
        fixationLog: FixationLogRecord,
        context: TypeSystemInferenceExtensionContext,
    ) {
        verifyContext(context)
        val fixationLogs = currentStageElements.mapNotNull { (it as? FixationLogRecordElement)?.record }

        if (fixationLogs.isEmpty() || !fixationLogs.last().isSimilarTo(fixationLog)) {
            currentStageElements.add(FixationLogRecordElement(fixationLog))
        }
    }

    private fun FixationLogRecord.isSimilarTo(record: FixationLogRecord): Boolean {
        if (record.chosen !== chosen) return false
        if (record.map.size != map.size) return false
        for ((variable, info) in record.map) {
            if (!info.isSimilarTo(map[variable])) return false
        }
        return true
    }

    private fun FixationLogVariableInfo.isSimilarTo(info: FixationLogVariableInfo?): Boolean {
        if (info == null) return false
        if (readiness != info.readiness) return false
        if (constraints.size != info.constraints.size) return false
        for (i in 0 until constraints.size) {
            if (constraints[i] !== info.constraints[i]) return false
        }
        return true
    }

    fun assignFixedToInFixationLogs(context: VariableFixationFinder.Context) {
        verifyContext(context)
        for ((constructor, type) in context.fixedTypeVariables) {
            val typeVariable = context.allTypeVariables[constructor] ?: continue
            val relevantBlocks = topLevelElements.filter { (it.owner as? BlockOwner.Candidate)?.candidate?.system == context }

            for (block in relevantBlocks) {
                for (element in block.elements) {
                    if (element !is FixationLogRecordElement) continue
                    val log = element.record
                    if (log.chosen !== typeVariable) continue
                    if (log.map[typeVariable]?.readiness == TypeVariableFixationReadiness.FORBIDDEN) continue
                    log.fixedTo = type
                }
            }
        }
    }

    data class State(
        private val outer: FirConstraintsLogger,
        val previous: List<ConstraintElement> = mutableListOf(),
    ) : ConstraintsLogger.State() {
        override fun withPrevious(constraint: InitialConstraint) {
            outer.currentState = copy(previous = listOf(outer.cachedElementFor(constraint)))
        }

        override fun withPrevious(
            variable1: TypeVariableMarker,
            constraint1: Constraint,
            variable2: TypeVariableMarker,
            constraint2: Constraint,
        ) {
            outer.currentState = copy(
                previous = listOf(
                    outer.cachedElementFor(variable1, constraint1),
                    outer.cachedElementFor(variable2, constraint2),
                ),
            )
        }

        override fun restore() {
            outer.currentState = this
        }
    }

    companion object {
        @JvmStatic
        protected fun formatConstraint(constraint: InitialConstraint): String {
            return when (constraint.constraintKind) {
                ConstraintKind.UPPER -> "${constraint.a} <: ${constraint.b}"
                ConstraintKind.LOWER -> "${constraint.b} <: ${constraint.a}"
                ConstraintKind.EQUALITY -> "${constraint.a} == ${constraint.b}"
            }
        }

        @JvmStatic
        protected fun formatConstraint(variable: TypeVariableMarker, constraint: Constraint): String {
            return when (constraint.kind) {
                ConstraintKind.LOWER -> "${constraint.type} <: $variable"
                ConstraintKind.UPPER -> "$variable <: ${constraint.type}"
                ConstraintKind.EQUALITY -> "$variable == ${constraint.type}"
            }
        }

        private val fqNameRegex = """(?:\w+\.)*(\w+)@\w+""".toRegex()

        @JvmStatic
        fun sanitizeFqNames(string: String): String = string.replace(fqNameRegex, "$1")
    }
}

val FirSession.constraintsLogger: FirConstraintsLogger? by FirSession.nullableSessionComponentAccessor<FirConstraintsLogger>()
