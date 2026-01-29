/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isEffectivelyExternal
import org.jetbrains.kotlin.fir.analysis.js.checkers.isExportedObject
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.expectForActual
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

object FirJsExportedActualMatchExpectChecker : FirBasicDeclarationChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (
            !LanguageFeature.AllowExpectDeclarationsInJsExport.isEnabled() ||
            declaration !is FirMemberDeclaration ||
            !declaration.isActual
        ) return

        val expectForActualMatchingData = declaration.symbol.expectForActual.orEmpty()
        val correspondingExpectDeclaration = expectForActualMatchingData[ExpectActualMatchingCompatibility.MatchedSuccessfully]
            ?.singleOrNull() ?: return

        if (!correspondingExpectDeclaration.isExportedObject()) return

        val correspondingActualDeclaration = when (declaration) {
            is FirTypeAlias -> {
                declaration.expandedTypeRef.coneType.abbreviatedTypeOrSelf.toClassSymbol() ?: return
            }
            else -> declaration.symbol
        }

        if (!correspondingActualDeclaration.isReachableOutsideOfKotlin()) {
            reporter.reportOn(declaration.source, FirJsErrors.NOT_EXPORTED_OR_EXTERNAL_ACTUAL_DECLARATION_WHILE_EXPECT_IS_EXPORTED)
        }
    }

    context(context: CheckerContext)
    private fun FirBasedSymbol<*>.isReachableOutsideOfKotlin(): Boolean =
        when (this) {
            is FirClassLikeSymbol -> isExportedObject() || isEffectivelyExternal()
            else -> isExportedObject()
        }
}