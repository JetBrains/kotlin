/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorComponents

object DiagnosticComponentsFactory {
    fun createAllDiagnosticComponents(session: FirSession, reporter: DiagnosticReporter): DiagnosticCollectorComponents {
        val regularComponents = listOf(
            DeclarationCheckersDiagnosticComponent(session, reporter),
            ExpressionCheckersDiagnosticComponent(session, reporter),
            TypeCheckersDiagnosticComponent(session, reporter),
            ErrorNodeDiagnosticCollectorComponent(session, reporter),
            ControlFlowAnalysisDiagnosticComponent(session, reporter),
            LanguageVersionSettingsDiagnosticComponent(session, reporter)
        )
        return DiagnosticCollectorComponents(regularComponents, ReportCommitterDiagnosticComponent(session, reporter))
    }

}