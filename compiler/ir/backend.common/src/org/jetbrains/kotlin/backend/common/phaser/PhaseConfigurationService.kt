/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

/**
 * Control which parts of compilation pipeline are enabled and
 * how compiler should validate their invariants.
 */
interface PhaseConfigurationService {
    /**
     * Check if the given [phase] should be executed during compilation.
     */
    fun isEnabled(phase: AnyNamedPhase): Boolean

    /**
     * Check if the compiler should print additional information
     * during [phase] execution.
     */
    fun isVerbose(phase: AnyNamedPhase): Boolean

    /**
     * Prevent compiler from executing the given [phase].
     */
    fun disable(phase: AnyNamedPhase)

    /**
     * Check if compiler should dump its state right before
     * the execution of the given [phase].
     */
    fun shouldDumpStateBefore(phase: AnyNamedPhase): Boolean

    /**
     * Check if compiler should dump its state right after
     * the execution of the given [phase].
     */
    fun shouldDumpStateAfter(phase: AnyNamedPhase): Boolean

    /**
     * Check if compiler should validate its state right before
     * the execution of the given [phase].
     */
    fun shouldValidateStateBefore(phase: AnyNamedPhase): Boolean

    /**
     * Check if compiler should validate its state right after
     * the execution of the given [phase].
     */
    fun shouldValidateStateAfter(phase: AnyNamedPhase): Boolean

    /**
     * Returns true if compiler should measure how long takes each phase.
     */
    val needProfiling: Boolean

    /**
     * Returns true if compiler should check pre- and post-conditions of compiler phases.
     */
    val checkConditions: Boolean

    /**
     * Returns true if compiler should check post-conditions that are applicable to subsequent (thus "sticky") phases.
     */
    val checkStickyConditions: Boolean

    /**
     * Returns a path to a directory that should store phase dump.
     * null if directory is not set.
     */
    val dumpToDirectory: String?

    /**
     * Returns a fully-qualified name that should be used to filter phase dump.
     * null if dump should not be filtered.
     */
    val dumpOnlyFqName: String?
}