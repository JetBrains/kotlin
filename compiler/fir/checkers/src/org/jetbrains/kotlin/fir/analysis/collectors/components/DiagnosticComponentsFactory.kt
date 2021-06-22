/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter

object DiagnosticComponentsFactory {
    fun createAllDiagnosticComponents(session: FirSession, reporter: DiagnosticReporter): List<AbstractDiagnosticCollectorComponent> {
        return listOf(
            // NB: the component for declaration checkers should precede the CFA component.
            // See comments in [PropertyInitializationInfoCache] for more details.
            DeclarationCheckersDiagnosticComponent(session, reporter),
            ExpressionCheckersDiagnosticComponent(session, reporter),
            TypeCheckersDiagnosticComponent(session, reporter),
            ErrorNodeDiagnosticCollectorComponent(session, reporter),
            ControlFlowAnalysisDiagnosticComponent(session, reporter),
        )
    }
}
