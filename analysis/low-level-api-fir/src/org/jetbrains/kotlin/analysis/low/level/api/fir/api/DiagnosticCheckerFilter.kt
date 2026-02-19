/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

data class DiagnosticCheckerFilter(
    val runDefaultCheckers: Boolean,
    val runExtraCheckers: Boolean,
    val runExperimentalCheckers: Boolean,
) {
    companion object {
        val ONLY_DEFAULT_CHECKERS = DiagnosticCheckerFilter(
            runDefaultCheckers = true, runExtraCheckers = false, runExperimentalCheckers = false,
        )
        val ONLY_EXTRA_CHECKERS = DiagnosticCheckerFilter(
            runDefaultCheckers = false, runExtraCheckers = true, runExperimentalCheckers = false,
        )
        val ONLY_EXPERIMENTAL_CHECKERS = DiagnosticCheckerFilter(
            runDefaultCheckers = false, runExtraCheckers = false, runExperimentalCheckers = true,
        )
    }
}

operator fun DiagnosticCheckerFilter.plus(other: DiagnosticCheckerFilter) =
    DiagnosticCheckerFilter(
        runDefaultCheckers || other.runDefaultCheckers,
        runExtraCheckers || other.runExtraCheckers,
        runExperimentalCheckers || other.runExperimentalCheckers,
    )
