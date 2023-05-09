/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.issues

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.linkage.issues.PotentialConflictKind.*
import org.jetbrains.kotlin.backend.common.linkage.issues.PotentialConflictKind.Companion.mostSignificantConflictKind
import org.jetbrains.kotlin.backend.common.linkage.issues.PotentialConflictReason.Companion.mostSignificantConflictReasons
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.ResolvedDependency
import org.jetbrains.kotlin.utils.ResolvedDependencyId
import org.jetbrains.kotlin.utils.ResolvedDependencyVersion
import kotlin.Comparator

abstract class KotlinIrLinkerIssue {
    protected abstract val errorMessage: String

    fun raiseIssue(messageLogger: IrMessageLogger): Nothing {
        messageLogger.report(IrMessageLogger.Severity.ERROR, errorMessage, null)
        throw CompilationErrorException()
    }
}

class UnexpectedUnboundIrSymbols(unboundSymbols: Set<IrSymbol>, whenDetected: String) : KotlinIrLinkerIssue() {
    override val errorMessage = buildString {
        // cause:
        append("There ").append(
            when (val count = unboundSymbols.size) {
                1 -> "is still an unbound symbol"
                else -> "are still $count unbound symbols"
            }
        ).append(" ").append(whenDetected).append(":\n")
        unboundSymbols.joinTo(this, separator = "\n")

        // explanation:
        append("\n\nThis could happen if there are two libraries, where one library was compiled against the different version")
        append(" of the other library than the one currently used in the project.")

        // action items:
        append(" Please check that the project configuration is correct and has consistent versions of dependencies.")
        if (unboundSymbols.any { looksLikeEnumEntries(it.signature) }) {
            append("\n\nAnother possible reason is that some parts of the project are compiled with EnumEntries language feature enabled,")
            append(" but other parts or used libraries are compiled with EnumEntries language feature disabled.")
        }
    }

    companion object {
        fun looksLikeEnumEntries(signature: IdSignature?): Boolean = when (signature) {
            is IdSignature.AccessorSignature -> looksLikeEnumEntries(signature.propertySignature)
            is IdSignature.CompositeSignature -> looksLikeEnumEntries(signature.inner)
            is IdSignature.CommonSignature -> signature.shortName == "entries"
            else -> false
        }
    }
}

class SignatureIdNotFoundInModuleWithDependencies(
    private val idSignature: IdSignature,
    private val problemModuleDeserializer: IrModuleDeserializer,
    private val allModuleDeserializers: Collection<IrModuleDeserializer>,
    private val userVisibleIrModulesSupport: UserVisibleIrModulesSupport
) : KotlinIrLinkerIssue() {
    override val errorMessage = try {
        computeErrorMessage()
    } catch (e: Throwable) {
        // Don't suppress the real cause if computation of error message failed.
        throw RuntimeException(
            buildString {
                appendLine("Failed to compute the detailed error message. See the root cause exception.")
                appendLine()
                append("Shortly: The required symbol ${idSignature.render()} is missing in the module or module dependencies.")
                append(" This could happen if the required dependency is missing in the project.")
                append(" Or if there is a dependency that has a different version (without the required symbol) in the project")
                append(" than the version (with the required symbol) that the module was initially compiled with.")
            }
        )
    }

    private fun computeErrorMessage() = buildString {
        val allModules = userVisibleIrModulesSupport.getUserVisibleModules(allModuleDeserializers)

        val problemModuleId = userVisibleIrModulesSupport.getProblemModuleId(problemModuleDeserializer, allModules)
        val problemModuleIdWithVersion = allModules.getValue(problemModuleId).moduleIdWithVersion

        // cause:
        append("Module \"$problemModuleId\" has a reference to symbol ${idSignature.render()}.")
        append(" Neither the module itself nor its dependencies contain such declaration.")

        // explanation:
        append("\n\nThis could happen if the required dependency is missing in the project.")
        append(" Or if there is a dependency of \"$problemModuleId\" that has a different version in the project")
        append(" than the version that \"$problemModuleIdWithVersion\" was initially compiled with.")

        // action items:
        append(" Please check that the project configuration is correct and has consistent versions of all required dependencies.")

        // potentially conflicting modules:
        appendPotentiallyConflictingDependencies(
            header = "The list of \"$problemModuleIdWithVersion\" dependencies that may lead to conflicts:",
            allModules = allModules,
            potentiallyConflictingDependencies = findPotentiallyConflictingOutgoingDependencies(
                problemModuleId = problemModuleId,
                allModules = allModules,
            ),
            moduleIdComparator = userVisibleIrModulesSupport.moduleIdComparator
        )

        // the tree of dependencies:
        appendProjectDependencies(
            allModules = allModules,
            problemModuleIds = setOf(problemModuleId),
            problemCause = "This module requires symbol ${idSignature.render()}",
            sourceCodeModuleId = userVisibleIrModulesSupport.sourceCodeModuleId,
            moduleIdComparator = userVisibleIrModulesSupport.moduleIdComparator
        )
    }
}

class NoDeserializerForModule(moduleName: Name, idSignature: IdSignature?) : KotlinIrLinkerIssue() {
    override val errorMessage = buildString {
        append("Could not load module ${moduleName.asString()}")
        if (idSignature != null) append(" in an attempt to find deserializer for symbol ${idSignature.render()}.")
    }
}

class SymbolTypeMismatch(
    private val cause: IrSymbolTypeMismatchException,
    private val allModuleDeserializers: Collection<IrModuleDeserializer>,
    private val userVisibleIrModulesSupport: UserVisibleIrModulesSupport
) : KotlinIrLinkerIssue() {
    override val errorMessage = try {
        computeErrorMessage()
    } catch (e: Throwable) {
        // Don't suppress the real cause if computation of error message failed.
        throw if (e === cause) e else e.apply { addSuppressed(cause) }
    }

    private fun computeErrorMessage() = buildString {
        val allModules = userVisibleIrModulesSupport.getUserVisibleModules(allModuleDeserializers)

        val idSignature = cause.actual.signature
        // There might be multiple declaring modules. Which is also an error, and should re reported separately.
        val declaringModuleIds: Set<ResolvedDependencyId> = if (idSignature != null) {
            allModuleDeserializers.mapNotNullTo(mutableSetOf()) { deserializer ->
                if (idSignature in deserializer) userVisibleIrModulesSupport.getProblemModuleId(deserializer, allModules) else null
            }
        } else emptySet()

        // cause:
        append(cause.message)

        // explanation:
        append("\n\nThis could happen if there are two libraries, where one library was compiled against the different version")
        append(" of the other library than the one currently used in the project.")

        // action items:
        append(" Please check that the project configuration is correct and has consistent versions of dependencies.")

        // potentially conflicting modules:
        declaringModuleIds.forEach { declaringModuleId ->
            appendPotentiallyConflictingDependencies(
                header = "The list of libraries that depend on \"$declaringModuleId\" and may lead to conflicts:",
                allModules = allModules,
                potentiallyConflictingDependencies = findPotentiallyConflictingIncomingDependencies(
                    problemModuleId = declaringModuleId,
                    allModules = allModules,
                    sourceCodeModuleId = userVisibleIrModulesSupport.sourceCodeModuleId,
                ),
                moduleIdComparator = userVisibleIrModulesSupport.moduleIdComparator
            )
        }

        // the tree of dependencies:
        appendProjectDependencies(
            allModules = allModules,
            problemModuleIds = declaringModuleIds,
            problemCause = "This module contains ${
                idSignature?.render()?.let { "symbol $it" } ?: "a symbol"
            } that is the cause of the conflict",
            sourceCodeModuleId = userVisibleIrModulesSupport.sourceCodeModuleId,
            moduleIdComparator = userVisibleIrModulesSupport.moduleIdComparator
        )
    }
}

private fun UserVisibleIrModulesSupport.getProblemModuleId(
    problemModuleDeserializer: IrModuleDeserializer,
    allModules: Map<ResolvedDependencyId, ResolvedDependency>
): ResolvedDependencyId = allModules.findMatchingModule(getUserVisibleModuleId(problemModuleDeserializer)).id

/**
 * Do the best effort to find a module that matches the given [moduleId]:
 * - If there is a node in the map with such [ResolvedDependencyId] as [moduleId], then return the value from this node.
 * - If not, then try to find a [ResolvedDependency] which contains all unique names from the given [moduleId]. This makes sense
 *   for such cases when the map contains a node for "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-macosx64)"
 *   but we are looking just for "org.jetbrains.kotlinx:kotlinx-coroutines-core".
 */
private fun Map<ResolvedDependencyId, ResolvedDependency>.findMatchingModule(moduleId: ResolvedDependencyId): ResolvedDependency {
    this[moduleId]?.let { module ->
        // Yes, there is a module with such ID. Just return it.
        return module
    }

    // No, there is no module with such ID. => Find a module with a wider set of unique names.
    // Typical case is when we are looking for "org.jetbrains.kotlinx:kotlinx-coroutines-core", but there is no such module.
    // But there is "org.jetbrains.kotlinx:kotlinx-coroutines-core (org.jetbrains.kotlinx:kotlinx-coroutines-core-macosx64)"
    // that should be used instead.
    return values.first { moduleId in it.id }
}

private fun StringBuilder.appendProjectDependencies(
    allModules: Map<ResolvedDependencyId, ResolvedDependency>,
    problemModuleIds: Set<ResolvedDependencyId>,
    problemCause: String,
    sourceCodeModuleId: ResolvedDependencyId,
    moduleIdComparator: Comparator<ResolvedDependencyId>
) {
    append("\n\nProject dependencies:")
    if (allModules.isEmpty()) {
        append(" <empty>")
        return
    }

    val incomingDependencyIdToDependencies: MutableMap<ResolvedDependencyId, MutableCollection<ResolvedDependency>> = hashMapOf()
    allModules.values.forEach { module ->
        module.requestedVersionsByIncomingDependencies.keys.forEach { incomingDependencyId ->
            incomingDependencyIdToDependencies.getOrPut(incomingDependencyId) { mutableListOf() } += module
        }
    }

    val renderedModules: MutableSet<ResolvedDependencyId> = hashSetOf()
    var everDependenciesOmitted = false

    fun renderModules(modules: Collection<ResolvedDependency>, parentData: Data?) {
        val filteredModules: Collection<ResolvedDependency> = if (parentData == null)
            modules.filter { it.visibleAsFirstLevelDependency }
        else
            modules

        val sortedModules: List<ResolvedDependency> = filteredModules.sortedWith { a, b -> moduleIdComparator.compare(a.id, b.id) }

        sortedModules.forEachIndexed { index, module ->
            val data = Data(
                parent = parentData,
                incomingDependencyId = module.id, // For children.
                isLast = index + 1 == sortedModules.size
            )

            append('\n').append(data.regularLinePrefix)
            append(module.id)

            val incomingDependencyId: ResolvedDependencyId = parentData?.incomingDependencyId ?: sourceCodeModuleId
            val requestedVersion: ResolvedDependencyVersion = module.requestedVersionsByIncomingDependencies.getValue(incomingDependencyId)
            if (!requestedVersion.isEmpty() || !module.selectedVersion.isEmpty()) {
                append(": ")
                append(requestedVersion.version.ifEmpty { UNKNOWN_VERSION })
                if (requestedVersion != module.selectedVersion) {
                    append(" -> ")
                    append(module.selectedVersion.version.ifEmpty { UNKNOWN_VERSION })
                }
            }

            val renderedFirstTime = renderedModules.add(module.id)
            val dependencies: Collection<ResolvedDependency>? = incomingDependencyIdToDependencies[module.id]

            val needToRenderDependencies = when {
                renderedFirstTime -> {
                    // Rendered for the first time => also render dependencies, if any.
                    true
                }
                !dependencies.isNullOrEmpty() -> {
                    // Already rendered at least once. Do not render dependencies, but add a mark that dependencies are omitted.
                    everDependenciesOmitted = true
                    append(" (*)")
                    false
                }
                else -> {
                    // Already rendered at least once. No dependencies.
                    false
                }
            }


            if (module.id in problemModuleIds) {
                append('\n').append(data.errorLinePrefix)
                append("^^^ $problemCause.")
            }

            if (needToRenderDependencies && !dependencies.isNullOrEmpty()) {
                renderModules(dependencies, data)
            }
        }
    }

    // Find first-level dependencies. I.e. the modules that the source code module directly depends on.
    val firstLevelDependencies: Collection<ResolvedDependency> = incomingDependencyIdToDependencies.getValue(sourceCodeModuleId)

    renderModules(firstLevelDependencies, parentData = null)

    if (everDependenciesOmitted) {
        append("\n\n(*) - dependencies omitted (listed previously)")
    }
}

private const val UNKNOWN_VERSION = "unknown"

private class Data(val parent: Data?, val incomingDependencyId: ResolvedDependencyId, val isLast: Boolean) {
    val regularLinePrefix: String
        get() {
            return generateSequence(this) { it.parent }.map {
                if (it === this) {
                    if (it.isLast) "\\--- " else "+--- "
                } else {
                    if (it.isLast) "     " else "|    "
                }
            }.toList().asReversed().joinToString(separator = "")
        }

    val errorLinePrefix: String
        get() {
            return generateSequence(this) { it.parent }.map {
                if (it.isLast) "     " else "|    "
            }.toList().asReversed().joinToString(separator = "")
        }
}

/**
 * Find all outgoing dependencies of [problemModuleId] that might conflict with [problemModuleId] because they have
 * different (overridden) selected version then the version that [problemModuleId] was initially compiled with.
 */
private fun findPotentiallyConflictingOutgoingDependencies(
    problemModuleId: ResolvedDependencyId,
    allModules: Map<ResolvedDependencyId, ResolvedDependency>
): Map<ResolvedDependencyId, PotentialConflictDescription> {
    data class OutgoingDependency(
        val id: ResolvedDependencyId,
        val requestedVersion: ResolvedDependencyVersion,
        val selectedVersion: ResolvedDependencyVersion
    )

    // Reverse dependency index.
    val outgoingDependenciesIndex: MutableMap<ResolvedDependencyId, MutableList<OutgoingDependency>> = hashMapOf()

    allModules.values.forEach { module ->
        module.requestedVersionsByIncomingDependencies.forEach { (incomingDependencyId, requestedVersion) ->
            outgoingDependenciesIndex.getOrPut(incomingDependencyId) { mutableListOf() } += OutgoingDependency(
                id = module.id,
                requestedVersion = requestedVersion,
                selectedVersion = module.selectedVersion
            )
        }
    }

    val dependencyStatesMap: MutableMap<ResolvedDependencyId, MutableSet<DependencyState>> = mutableMapOf()

    fun recurse(moduleId: ResolvedDependencyId, underConflictingDependency: Boolean) {
        val outgoingDependencies: List<OutgoingDependency> = outgoingDependenciesIndex[moduleId].orEmpty()

        outgoingDependencies.forEach { outgoingDependency ->
            val dependencyState: DependencyState = when {
                underConflictingDependency -> {
                    // Can't guarantee that any library that is a dependency of a potentially conflicting dependency
                    // is not a potentially conflicting dependency itself.
                    DependencyState(
                        conflictReason = PotentialConflictReason(
                            kind = BEHIND_CONFLICTING_DEPENDENCY,
                            conflictingModuleId = outgoingDependency.id
                        )
                    )
                }
                outgoingDependency.selectedVersion.isEmpty() -> {
                    // Selected version is empty (unknown). Can't guarantee that this is the same library that was requested even if
                    // the requested version is also empty (unknown).
                    DependencyState(
                        conflictReason = PotentialConflictReason(
                            kind = UNKNOWN_SELECTED_VERSION,
                            conflictingModuleId = outgoingDependency.id,
                            requestedVersion = outgoingDependency.requestedVersion
                        )
                    )
                }
                outgoingDependency.requestedVersion != outgoingDependency.selectedVersion -> {
                    // The requested and selected versions don't match.
                    DependencyState(
                        conflictReason = PotentialConflictReason(
                            kind = REQUESTED_SELECTED_VERSIONS_MISMATCH,
                            conflictingModuleId = outgoingDependency.id,
                            requestedVersion = outgoingDependency.requestedVersion,
                            selectedVersion = outgoingDependency.selectedVersion
                        )
                    )
                }
                else -> DependencyState.SUCCESS
            }

            val dependencyStates: MutableSet<DependencyState> = dependencyStatesMap.getOrPut(outgoingDependency.id) { mutableSetOf() }
            val notBeenHereYet = dependencyStates.add(dependencyState)

            if (notBeenHereYet) {
                // Don't visit the same dependency twice.
                recurse(moduleId = outgoingDependency.id, underConflictingDependency = dependencyState.conflictReason != null)
            }
        }
    }

    recurse(moduleId = problemModuleId, underConflictingDependency = false)

    return dependencyStatesMap.describeDependencyStates { potentialConflictReason ->
        when (potentialConflictReason.kind) {
            UNKNOWN_SELECTED_VERSION -> {
                "a library with unknown version"
            }
            REQUESTED_SELECTED_VERSIONS_MISMATCH -> {
                val requested = potentialConflictReason.conflictingModuleId.withVersion(potentialConflictReason.requestedVersion)
                "was initially compiled with \"$requested\""
            }
            BEHIND_CONFLICTING_DEPENDENCY -> {
                "a dependency of the library with unknown version or versions mismatch: \"${potentialConflictReason.conflictingModuleId}\""
            }
        }
    }
}

/**
 * Find all incoming dependencies of [problemModuleId] that might conflict with [problemModuleId] because they were
 * initially compiled with the different version of [problemModuleId] than the one used in the project.
 */
private fun findPotentiallyConflictingIncomingDependencies(
    problemModuleId: ResolvedDependencyId,
    allModules: Map<ResolvedDependencyId, ResolvedDependency>,
    sourceCodeModuleId: ResolvedDependencyId
): Map<ResolvedDependencyId, PotentialConflictDescription> {

    val dependencyStatesMap: MutableMap<ResolvedDependencyId, MutableSet<DependencyState>> = mutableMapOf()

    fun recurse(moduleId: ResolvedDependencyId, aboveConflictingDependency: Boolean) {
        val module = allModules.findMatchingModule(moduleId)

        module.requestedVersionsByIncomingDependencies.forEach { (incomingDependencyId, requestedVersion) ->
            if (incomingDependencyId == sourceCodeModuleId) return@forEach

            val dependencyState: DependencyState = when {
                aboveConflictingDependency -> {
                    // Can't guarantee that any library that is an incoming dependency of a potentially conflicting dependency
                    // is not a potentially conflicting dependency itself.
                    DependencyState(
                        conflictReason = PotentialConflictReason(
                            kind = BEHIND_CONFLICTING_DEPENDENCY,
                            conflictingModuleId = module.id
                        )
                    )
                }
                module.selectedVersion.isEmpty() -> {
                    // Selected version is empty (unknown). Can't guarantee that this is the same library that was requested even if
                    // the requested version is also empty (unknown).
                    DependencyState(
                        conflictReason = PotentialConflictReason(
                            kind = UNKNOWN_SELECTED_VERSION,
                            conflictingModuleId = module.id,
                            requestedVersion = requestedVersion
                        )
                    )
                }
                requestedVersion != module.selectedVersion -> {
                    // The requested and selected versions don't match.
                    DependencyState(
                        conflictReason = PotentialConflictReason(
                            kind = REQUESTED_SELECTED_VERSIONS_MISMATCH,
                            conflictingModuleId = module.id,
                            requestedVersion = requestedVersion,
                            selectedVersion = module.selectedVersion
                        )
                    )
                }
                else -> DependencyState.SUCCESS
            }

            val dependencyStates: MutableSet<DependencyState> = dependencyStatesMap.getOrPut(incomingDependencyId) { mutableSetOf() }
            val notBeenHereYet = dependencyStates.add(dependencyState)

            if (notBeenHereYet) {
                // Don't visit the same dependency twice.
                recurse(moduleId = incomingDependencyId, aboveConflictingDependency = dependencyState.isConflicting)
            }
        }
    }

    recurse(moduleId = problemModuleId, aboveConflictingDependency = false)

    return dependencyStatesMap.describeDependencyStates { potentialConflictReason ->
        when (potentialConflictReason.kind) {
            UNKNOWN_SELECTED_VERSION -> {
                "depends on the library with unknown version: \"${potentialConflictReason.conflictingModuleId}\""
            }
            REQUESTED_SELECTED_VERSIONS_MISMATCH -> {
                val requested = potentialConflictReason.conflictingModuleId.withVersion(potentialConflictReason.requestedVersion)
                val selected = potentialConflictReason.conflictingModuleId.withVersion(potentialConflictReason.selectedVersion)
                "was compiled against \"$requested\" but \"$selected\" is used in the project"
            }
            BEHIND_CONFLICTING_DEPENDENCY -> {
                "depends on the library with unknown version or versions mismatch: \"${potentialConflictReason.conflictingModuleId}\""
            }
        }
    }
}

private typealias PotentialConflictDescription = String

private class PotentialConflictReason(
    val kind: PotentialConflictKind,
    val conflictingModuleId: ResolvedDependencyId,
    val requestedVersion: ResolvedDependencyVersion = ResolvedDependencyVersion.EMPTY,
    val selectedVersion: ResolvedDependencyVersion = ResolvedDependencyVersion.EMPTY
) {
    override fun equals(other: Any?) =
        other is PotentialConflictReason && other.kind == kind && other.conflictingModuleId == conflictingModuleId

    override fun hashCode() = kind.hashCode() + 31 * conflictingModuleId.hashCode()

    companion object {
        val Collection<PotentialConflictReason>.mostSignificantConflictReasons: Collection<PotentialConflictReason>
            get() {
                val mapping: Map<PotentialConflictKind, List<PotentialConflictReason>> = groupBy { it.kind }
                val mostSignificantConflictKind: PotentialConflictKind = mapping.keys.mostSignificantConflictKind ?: return emptyList()
                return mapping.getValue(mostSignificantConflictKind)
            }
    }
}


// Important: Enum entries must be declared in the order of most-to-least significant reasons.
private enum class PotentialConflictKind {
    UNKNOWN_SELECTED_VERSION,
    REQUESTED_SELECTED_VERSIONS_MISMATCH,
    BEHIND_CONFLICTING_DEPENDENCY;

    companion object {
        val Collection<PotentialConflictKind>.mostSignificantConflictKind: PotentialConflictKind?
            get() = minOrNull()
    }
}

private data class DependencyState(val conflictReason: PotentialConflictReason? /* Null assumes success. */) {
    val isConflicting: Boolean get() = conflictReason != null

    companion object {
        val SUCCESS = DependencyState(conflictReason = null)
    }
}

private fun Map<ResolvedDependencyId, MutableSet<DependencyState>>.describeDependencyStates(
    getDescription: (PotentialConflictReason) -> PotentialConflictDescription
): Map<ResolvedDependencyId, PotentialConflictDescription> = mapNotNull { (dependencyId, dependencyStates) ->
    val mostSignificantConflictReasons = dependencyStates.mapNotNull { it.conflictReason }.mostSignificantConflictReasons

    when {
        mostSignificantConflictReasons.isEmpty() -> {
            // No conflicts.
            return@mapNotNull null
        }
        mostSignificantConflictReasons.first().kind == BEHIND_CONFLICTING_DEPENDENCY -> {
            // Ignore dependency of conflicting dependency if this library has no conflicts when referred from
            // non-conflicting dependencies.
            val hasNoConflictsBehindNonConflictingDependencies = dependencyStates.any { !it.isConflicting }
            if (hasNoConflictsBehindNonConflictingDependencies) return@mapNotNull null
        }
        else -> Unit
    }

    dependencyId to mostSignificantConflictReasons.joinToString(separator = "; ") { getDescription(it) }
}.toMap()

private fun StringBuilder.appendPotentiallyConflictingDependencies(
    header: String,
    allModules: Map<ResolvedDependencyId, ResolvedDependency>,
    potentiallyConflictingDependencies: Map<ResolvedDependencyId, PotentialConflictDescription>,
    moduleIdComparator: Comparator<ResolvedDependencyId>
) {
    if (potentiallyConflictingDependencies.isEmpty()) return

    append("\n\n$header")

    val paddingSize = (potentiallyConflictingDependencies.size + 1).toString().length
    potentiallyConflictingDependencies.toSortedMap(moduleIdComparator).entries.forEachIndexed { index, (moduleId, potentialConflictReason) ->
        val padding = (index + 1).toString().padStart(paddingSize, ' ')
        val moduleIdWithVersion = allModules.getValue(moduleId).moduleIdWithVersion
        append("\n$padding. \"$moduleIdWithVersion\" ($potentialConflictReason)")
    }
}
