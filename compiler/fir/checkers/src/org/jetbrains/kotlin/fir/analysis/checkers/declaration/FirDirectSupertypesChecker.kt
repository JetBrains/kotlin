/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.directInheritors
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.fir.types.coneType

object FirDirectSupertypesChecker : FirClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (LanguageFeature.DirectClassInheritors.isDisabled()) return
        if (declaration.source?.kind is KtFakeSourceElementKind) return
        val classId = declaration.symbol.classId
        for (superTypeRef in declaration.superTypeRefs) {
            if (superTypeRef.source == null || superTypeRef.source?.kind == KtFakeSourceElementKind.EnumSuperTypeRef) continue

            val expandedSupertype = superTypeRef.coneType.fullyExpandedType()
            val supertypeSymbol = expandedSupertype.abbreviatedTypeOrSelf.toSymbol() ?: continue
            if (supertypeSymbol is FirRegularClassSymbol) {
                if (classId !in supertypeSymbol.directInheritors) {
                    reporter.reportOn(superTypeRef.source, FirErrors.MISSING_INHERITOR_FOR, supertypeSymbol)
                }
            }
        }
    }
}
