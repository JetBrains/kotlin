/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

object FirHomePackageResolutionInvestigationChecker : FirFileChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        // if (LanguageFeature.HomePackageResolution.isDisabled()) return

        val imports = declaration.imports.groupBy { it.importKind() }
        val extensionsNotHPR = imports.filterKeys { it is ExtensionNotHPR }
        reporter.reportOn(
            declaration.source,
            FirErrors.HOME_PACKAGE_RESOLUTION_INVESTIGATION_1,
            imports[ImportKind.HPR]?.size ?: 0,
            extensionsNotHPR.values.sumOf { it.size },
            imports[ImportKind.NotExtension]?.size ?: 0,
        )
        reporter.reportOn(
            declaration.source,
            FirErrors.HOME_PACKAGE_RESOLUTION_INVESTIGATION_2,
            imports[ImportKind.Star]?.size ?: 0,
            imports[ImportKind.Rest]?.size ?: 0,
            imports[ImportKind.Problematic]?.size ?: 0,
        )
        for ([key, values] in extensionsNotHPR) {
            if (key !is ExtensionNotHPR) continue
            for (value in values) {
                reporter.reportOn(
                    value.source,
                    FirErrors.HOME_PACKAGE_RESOLUTION_NON_HPR_EXTENSION,
                    key.type,
                )
            }
        }
    }

    sealed interface ImportKind {
        data object Star : ImportKind
        data object HPR : ImportKind
        data class ExtensionNotHPR(val type: FirClassLikeSymbol<*>) : ImportKind
        data object NotExtension : ImportKind
        data object Rest : ImportKind
        data object Problematic : ImportKind
    }

    context(context: CheckerContext)
    fun FirImport.importKind(): ImportKind {
        if (isAllUnder) return Star
        if (this !is FirResolvedImport) return Problematic
        val importedFqName = importedFqName?.takeIf { !it.isRoot } ?: return Problematic
        val importedName = importedFqName.shortName()
        if (importedName.isSpecial || importedName.identifier.isEmpty()) return Problematic

        val symbolProvider = context.session.symbolProvider
        val parentFqName = importedFqName.parent()
        val topLevelCallableSymbol = symbolProvider.getTopLevelCallableSymbols(parentFqName, importedName)

        if (topLevelCallableSymbol.isEmpty()) return Rest
        if (topLevelCallableSymbol.any { it.resolvedReceiverType?.toClassLikeSymbol()?.packageFqName() == parentFqName }) return HPR

        val receiver = topLevelCallableSymbol.firstOrNull { it.resolvedReceiverType != null }?.resolvedReceiverType?.toClassLikeSymbol()
        if (receiver == null) return NotExtension
        return ImportKind.ExtensionNotHPR(receiver)
    }

}

