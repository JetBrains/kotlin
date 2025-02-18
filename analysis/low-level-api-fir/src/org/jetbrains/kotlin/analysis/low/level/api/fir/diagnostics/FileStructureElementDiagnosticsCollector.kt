/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.fir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir.LLFirStructureElementDiagnosticsCollector
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorComponents

fun collectForStructureElement(
    firDeclaration: FirDeclaration,
    filter: DiagnosticCheckerFilter,
    createVisitor: (components: DiagnosticCollectorComponents) -> CheckerRunningDiagnosticCollectorVisitor,
): FileStructureElementDiagnosticList {
    val reporter = LLFirDiagnosticReporter()
    val collector = LLFirStructureElementDiagnosticsCollector(
        firDeclaration.moduleData.session,
        createVisitor,
        filter,
    )
    collector.collectDiagnostics(firDeclaration, reporter)
    val source = firDeclaration.source
    if (source != null) {
        reporter.checkAndCommitReportsOn(source, null)
    }
    return FileStructureElementDiagnosticList(reporter.committedDiagnostics)
}
