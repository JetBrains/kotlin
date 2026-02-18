/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.*

/**
 * Checks cases like `part1<Int>.part2.Class`.
 *
 * TODO: KT-84254
 * It is recommended to get rid of this checker once [LanguageFeature.ForbidUselessTypeArgumentsIn25] is dropped
 * and return the corresponding logic to `FirTypeResolverImpl`. However, during the deprecation period, the checker is
 * arguably the more straightforward approach than storing deprecation information somewhere in the type reference.
 */
object TypeArgumentsInPackagesTypeRefChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        typeRef.forEachPackagePart { isLastPackagePart, qualifierPart ->
            if (qualifierPart.typeArgumentList.typeArguments.isEmpty()) return@forEachPackagePart

            val mustReportError = when {
                // We always (since K2) reported an error on the last package part, hence continuing to do so
                // while reporting warning on all other parts during the deprecation period
                isLastPackagePart -> true
                LanguageFeature.ForbidUselessTypeArgumentsIn25.isEnabled() -> true
                else -> false
            }

            reporter.reportOn(
                qualifierPart.typeArgumentList.source,
                if (mustReportError) FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED else FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED_WARNING,
                "for packages",
            )
        }
    }

    context(context: CheckerContext)
    private inline fun FirResolvedTypeRef.forEachPackagePart(block: (isLastPackagePart: Boolean, FirQualifierPart) -> Unit) {
        val delegatedTypeRef = delegatedTypeRef as? FirUserTypeRef ?: return
        var symbol: FirClassLikeSymbol<*>? =
            coneType.abbreviatedTypeOrSelf.toSymbol(context.session) as? FirClassLikeSymbol<*> ?: return
        var onePackagePartAlreadyMet = false

        for (qualifierPart in delegatedTypeRef.qualifier.asReversed()) {
            if (symbol == null) {
                block(!onePackagePartAlreadyMet, qualifierPart)
                onePackagePartAlreadyMet = true
            }
            symbol = symbol?.getContainingDeclaration(context.session)
        }
    }
}
