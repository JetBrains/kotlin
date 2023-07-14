/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.LanguageVersionSettingsCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.config.FirLanguageVersionSettingsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkersComponent

@OptIn(CheckersComponentInternal::class)
class LanguageVersionSettingsDiagnosticComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
    private val checkers: LanguageVersionSettingsCheckers = session.checkersComponent.languageVersionSettingsCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    override fun start(data: CheckerContext) {
        checkers.languageVersionSettingsCheckers.check(data)
    }

    private fun Collection<FirLanguageVersionSettingsChecker>.check(context: CheckerContext) {
        for (checker in this) {
            checker.check(context, reporter)
        }
    }
}
