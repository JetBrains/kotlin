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
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol

object FirHomePackageResolutionInvestigationChecker : FirFileChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        if (LanguageFeature.HomePackageResolution.isDisabled()) return

        val hpr = declaration.imports.count { it.redundantHPR() }
        val star = declaration.imports.count { it.isAllUnder }

        val total = declaration.imports.size
        reporter.reportOn(
            declaration.source,
            FirErrors.HOME_PACKAGE_RESOLUTION_INVESTIGATION,
            total,
            star,
            hpr
        )
    }

    context(context: CheckerContext)
    fun FirImport.redundantHPR(): Boolean {
        if (isAllUnder) return false  // * imports never count
        if (this !is FirResolvedImport) return false
        val importedFqName = importedFqName?.takeIf { !it.isRoot } ?: return false
        val importedName = importedFqName.shortName()
        if (importedName.isSpecial || importedName.identifier.isEmpty()) return false

        val symbolProvider = context.session.symbolProvider
        val parentFqName = importedFqName.parent()
        val topLevelCallableSymbol = symbolProvider.getTopLevelCallableSymbols(parentFqName, importedName)

        return topLevelCallableSymbol.any {
            it.resolvedReceiverType?.toClassLikeSymbol()?.packageFqName() == parentFqName
        }
    }

}

