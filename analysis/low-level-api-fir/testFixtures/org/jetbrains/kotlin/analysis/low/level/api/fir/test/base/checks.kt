/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import org.jetbrains.kotlin.analysis.low.level.api.fir.backReferences.checkFirFileBackReferences
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.id.checkSymbolIdConstraints
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.checkers.LLDistinctSourceElementsChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.checkers.LLFirFileBackReferenceChecker
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.checkers.LLSymbolIdConstraintsChecker
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.frontend.fir.checkDistinctSourceElements

/**
 * Registers all FIR consistency checkers as [after-analysis checkers][org.jetbrains.kotlin.test.model.AfterAnalysisChecker].
 *
 * This enumerates the same set of checks as [checkFirConsistency], which performs them inline (e.g. phase by phase). **Keep both in sync:**
 * when adding or removing a consistency check here, mirror the change in [checkFirConsistency] and vice versa.
 */
fun TestConfigurationBuilder.configureFirConsistencyChecks() {
    useAfterAnalysisCheckers(::LLDistinctSourceElementsChecker)
    useAfterAnalysisCheckers(::LLSymbolIdConstraintsChecker)
    useAfterAnalysisCheckers(::LLFirFileBackReferenceChecker)
}

/**
 * Performs all FIR consistency checks on the given [firFiles] inline (as opposed to [configureFirConsistencyChecks], which registers them
 * as after-analysis checkers running once at the end of the test).
 *
 * This is useful when the checks need to run repeatedly during a single test, for example phase by phase in
 * [AbstractFirLazyDeclarationResolveOverAllPhasesTest][org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractFirLazyDeclarationResolveOverAllPhasesTest].
 *
 * This enumerates the same set of checks as [configureFirConsistencyChecks]. **Keep both in sync:** when adding or removing a consistency
 * check here, mirror the change in [configureFirConsistencyChecks] and vice versa.
 *
 * @param lazyLocationDescription Describes *where* the check runs (e.g. `"at phase BODY_RESOLVE"`) and is appended to the error message of
 *  each failing check.
 */
fun checkFirConsistency(
    firFiles: List<FirFile>,
    lazyLocationDescription: () -> String = { "after analysis" },
) {
    checkDistinctSourceElements(firFiles) { _, _ -> "Duplicate source elements ${lazyLocationDescription()}" }
    checkSymbolIdConstraints(firFiles) { "Symbol ID constraint violation ${lazyLocationDescription()}" }
    firFiles.forEach { firFile ->
        checkFirFileBackReferences(firFile) { "FIR file back reference violation ${lazyLocationDescription()}" }
    }
}
