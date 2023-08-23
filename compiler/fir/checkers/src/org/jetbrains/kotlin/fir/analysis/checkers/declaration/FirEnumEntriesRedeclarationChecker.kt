/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry

object FirEnumEntriesRedeclarationChecker : FirEnumEntryChecker() {
    override fun check(declaration: FirEnumEntry, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.name == StandardNames.ENUM_ENTRIES) {
            reporter.reportOn(declaration.source, FirErrors.DEPRECATED_DECLARATION_OF_ENUM_ENTRY, context)
        }
    }
}
