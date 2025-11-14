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
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemMarker
import org.jetbrains.kotlin.resolve.calls.inference.components.InferenceLogger
import org.jetbrains.kotlin.resolve.calls.inference.components.LegacyVariableReadinessCalculator
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableReadinessCalculator
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableReadinessCalculator.TypeVariableFixationReadinessQuality as Q
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.inference.model.InitialConstraint
import org.jetbrains.kotlin.types.model.TypeVariableMarker

open class FirInferenceLogger : InferenceLogger(), FirSessionComponent {
    sealed class LoggingElement

    class Call(
        val fir: FirElement,
        val render: String = fir.render(),
    )

    sealed class BlockOwner {
        class Candidate(
            val candidate: org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate,
        ) : BlockOwner() {
            val owningCall: Call = Call(candidate.callInfo.callSite)
        }

        /**
         * A fallback for those cases when the constraints system is used without
         * a preceding clarification call like `logCandidate()`.
         */
        object Unknown : BlockOwner()
    }

    class BlockElement(
        val name: String,
        val items: MutableList<BlockItemElement> = mutableListOf(),
        val owner: BlockOwner,
    ) : LoggingElement()

    sealed class BlockItemElement() : LoggingElement()

    class NewVariableElement(val variable: TypeVariableMarker) : BlockItemElement()

    class ErrorElement(val error: ConstraintSystemError) : BlockItemElement()

    sealed class ConstraintElement(
        /**
         * Constraints the current one results from.
         * Currently, each constraint may only have 0, 1, or 2 origins.
         */
        val origins: List<ConstraintElement>,
    ) : BlockItemElement()

    class InitialConstraintElement(val constraint: String, val position: String) : ConstraintElement(emptyList())

    /**
     * Represents constraints on type variables.
     * For example, `TypeVariable(T) <: Type`, `TypeVariable(T) :> Type` or `TypeVariable(T) == Type`.
     */
    class VariableConstraintElement(
        val constraint: String,
        origins: List<ConstraintElement>,
    ) : ConstraintElement(origins)

    class FixationLogRecordElement(val record: FixationLogRecord) : BlockItemElement()

    val topLevelElements: MutableList<BlockElement> = mutableListOf<BlockElement>()

    private var currentSystem: ConstraintSystemMarker? = null

    private lateinit var currentBlock: BlockElement

    private val systemToKnownBlock: MutableMap<ConstraintSystemMarker, BlockElement> = mutableMapOf()
    private val systemToCandidate: MutableMap<ConstraintSystemMarker, BlockOwner.Candidate> = mutableMapOf()

    /**
     * Events (such as a constraint inference) within different systems may happen asynchronously,
     * and we must make sure we store them appropriately, inside different blocks.
     * This function verifies the system hasn't changed or instantiates a new block otherwise.
     */
    private fun prepareProperBlock(system: ConstraintSystemMarker) {
        if (system == currentSystem) return
        updateCurrentSystem(system)

        val knownPreviousBlock = systemToKnownBlock[system]
        val nextBlockTitle = when {
            knownPreviousBlock != null && currentCandidate != null -> "Continue " + knownPreviousBlock.name
            else -> error("UNKNOWN SYSTEM")
        }
        currentBlock = BlockElement(nextBlockTitle, owner = currentCandidate ?: BlockOwner.Unknown).apply { register(system) }
    }

    private fun updateCurrentSystem(system: ConstraintSystemMarker) {
        if (system == currentSystem) return
        currentSystem = system
        currentCandidate = systemToCandidate[system]
    }

    private fun BlockElement.register(system: ConstraintSystemMarker) {
        if (topLevelElements.lastOrNull()?.items?.isEmpty() == true) {
            topLevelElements.removeLast()
        }

        systemToKnownBlock[system] = this
        topLevelElements += this
    }

    private val initialConstraintToKnownElement = mutableMapOf<InitialConstraint, ConstraintElement>()
    private val variableConstraintToKnownElement = mutableMapOf<Pair<TypeVariableMarker, Constraint>, ConstraintElement>()

    private fun cachedElementFor(constraint: InitialConstraint) =
        initialConstraintToKnownElement[constraint]
            ?: error("This constraint has not yet been logged: $constraint")

    private fun cachedElementFor(variable: TypeVariableMarker, constraint: Constraint) =
        variableConstraintToKnownElement[variable to constraint]
            ?: error("This constraint has not yet been logged: $variable with $constraint")

    private var currentCandidate: BlockOwner.Candidate? = null

    fun logCandidate(candidate: Candidate) {
        // A candidate may have not been processed in one go.
        // See `fullyProcessCandidate()`.
        if (currentCandidate?.candidate !== candidate) {
            val candidateOwner = BlockOwner.Candidate(candidate)
            systemToCandidate[candidate.system] = candidateOwner
            currentCandidate = candidateOwner
            currentSystem = candidate.system
        }
    }

    fun logStage(name: String, system: ConstraintSystemMarker) {
        updateCurrentSystem(system)
        currentBlock = BlockElement(name, owner = currentCandidate ?: BlockOwner.Unknown).apply { register(system) }
    }

    private val currentBlockItemElements: MutableList<BlockItemElement>
        get() = currentBlock.items

    override fun logInitial(constraint: InitialConstraint, system: ConstraintSystemMarker) {
        prepareProperBlock(system)
        // The constraint position must be rendered right away, because it may contain
        // FIR renders, and the FIR may have changed by the time we finish inference.
        val element = InitialConstraintElement(formatConstraint(constraint), sanitizeFqNames(constraint.position.toString()))
        initialConstraintToKnownElement.putIfAbsent(constraint, element)
        currentBlockItemElements += element
    }

    override fun log(variable: TypeVariableMarker, constraint: Constraint, system: ConstraintSystemMarker) {
        prepareProperBlock(system)
        val element = VariableConstraintElement(formatConstraint(variable, constraint), origins)
        variableConstraintToKnownElement.putIfAbsent(variable to constraint, element)
        currentBlockItemElements += element
    }

    override fun logError(error: ConstraintSystemError, system: ConstraintSystemMarker) {
        prepareProperBlock(system)
        currentBlockItemElements.add(ErrorElement(error))
    }

    override fun logNewVariable(variable: TypeVariableMarker, system: ConstraintSystemMarker) {
        prepareProperBlock(system)
        currentBlockItemElements.add(NewVariableElement(variable))
    }

    override fun logReadiness(
        fixationLog: FixationLogRecord,
        system: ConstraintSystemMarker,
    ) {
        prepareProperBlock(system)
        val fixationLogs = currentBlockItemElements.mapNotNull { (it as? FixationLogRecordElement)?.record }

        if (fixationLogs.isEmpty() || !fixationLogs.last().isSimilarTo(fixationLog)) {
            currentBlockItemElements.add(FixationLogRecordElement(fixationLog))
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

    private fun FixationLogVariableInfo<*>.isSimilarTo(info: FixationLogVariableInfo<*>?): Boolean {
        if (info == null) return false
        if (readiness != info.readiness) return false
        if (constraints.size != info.constraints.size) return false
        for (i in 0 until constraints.size) {
            if (constraints[i] !== info.constraints[i]) return false
        }
        return true
    }

    fun assignFixedToInFixationLogs(system: ConstraintSystemCompletionContext) {
        prepareProperBlock(system)
        for ((constructor, type) in system.fixedTypeVariables) {
            val typeVariable = system.allTypeVariables[constructor] ?: continue
            val relevantBlocks = topLevelElements.filter { (it.owner as? BlockOwner.Candidate)?.candidate?.system == system }

            for (block in relevantBlocks) {
                for (element in block.items) {
                    if (element !is FixationLogRecordElement) continue
                    val log = element.record
                    if (log.chosen !== typeVariable) continue
                    if (log.map[typeVariable]?.isForbiddenReadiness == true) continue
                    log.fixedTo = type
                }
            }
        }
    }

    private val FixationLogVariableInfo<*>.isForbiddenReadiness: Boolean
        get() = when (val readiness = readiness) {
            is VariableReadinessCalculator.TypeVariableFixationReadiness -> !readiness[Q.ALLOWED]
            is LegacyVariableReadinessCalculator.TypeVariableFixationReadiness -> readiness == LegacyVariableReadinessCalculator.TypeVariableFixationReadiness.FORBIDDEN
            else -> error("Unexpected readiness type: ${readiness::class}")
        }

    var origins: List<ConstraintElement> = listOf()

    private inline fun <T> withOriginatingElements(elements: List<ConstraintElement>, block: () -> T): T {
        val oldOrigins = origins
        return try {
            origins = elements
            block()
        } finally {
            origins = oldOrigins
        }
    }

    override fun <T> withOrigin(constraint: InitialConstraint, block: () -> T): T =
        withOriginatingElements(listOf(cachedElementFor(constraint)), block)

    override fun <T> withOrigins(
        variable1: TypeVariableMarker,
        constraint1: Constraint,
        variable2: TypeVariableMarker,
        constraint2: Constraint,
        block: () -> T
    ): T {
        val elements = listOf(
            cachedElementFor(variable1, constraint1),
            cachedElementFor(variable2, constraint2),
        )
        return withOriginatingElements(elements, block)
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

val FirSession.inferenceLogger: FirInferenceLogger? by FirSession.nullableSessionComponentAccessor<FirInferenceLogger>()
