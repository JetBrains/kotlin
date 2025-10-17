/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.structure

/**
 * [LLSessionStatistics] aggregates statistics about a specific [LLFirSession][org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession]
 * in the session structure graph.
 *
 * The **weight** of a session is the weight of all FIR elements that it contains. This weight is approximated by the shallow sizes of FIR
 * elements and disregards secondary objects like FIR symbols, source elements (PSI), names, and so on. The weight is thus not an absolute
 * number meant to be read as "memory consumption of the session," but rather a measure of the relative weight between sessions, or the same
 * session over time (comparing multiple session structure snapshots).
 *
 * @property kotlinWeight The weight of all *Kotlin* FIR elements in the session.
 * @property javaWeight The weight of all *Java* FIR elements in the session.
 * @property lifetime The time in seconds since the creation of the session.
 */
internal class LLSessionStatistics(
    val kotlinWeight: Long,
    val javaWeight: Long,
    val lifetime: Double,
) {
    /**
     * The total weight of the session, combining both [kotlinWeight] and [javaWeight].
     */
    val weight: Long get() = kotlinWeight + javaWeight

    companion object {
        val ZERO = LLSessionStatistics(0, 0, 0.0)
    }
}
