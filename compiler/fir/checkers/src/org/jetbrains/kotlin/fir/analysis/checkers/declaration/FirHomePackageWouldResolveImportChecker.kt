/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toClassSymbol

object FirHomePackageWouldResolveImportChecker : FirFileChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        if (LanguageFeature.HomePackageResolution.isDisabled()) return
        for (import in declaration.imports) {
            if (import.isAllUnder || import.aliasName != null || import !is FirResolvedImport) continue
            val importedName = import.importedName ?: continue
            for (symbol in context.session.symbolProvider.getTopLevelCallableSymbols(import.packageFqName, importedName)) {
                val receiver = symbol.resolvedReceiverType?.toClassSymbol(context.session) ?: continue
                if (receiver.classId.packageFqName == import.packageFqName) {
                    reporter.reportOn(import.source, FirErrors.HOME_PACKAGE_WOULD_RESOLVE_THIS)
                    break
                }
            }
        }
    }
}
