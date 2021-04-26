/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.AbstractFirIdeDiagnosticsCollector

internal class FirIdeStructureElementDiagnosticsCollector(
    session: FirSession,
    private val doCreateVisitor: (components: List<AbstractDiagnosticCollectorComponent>) -> CheckerRunningDiagnosticCollectorVisitor,
    useExtendedCheckers: Boolean,
) : AbstractFirIdeDiagnosticsCollector(
    session,
    useExtendedCheckers,
) {
    override fun createVisitor(components: List<AbstractDiagnosticCollectorComponent>): CheckerRunningDiagnosticCollectorVisitor {
        return doCreateVisitor(components)
    }
}