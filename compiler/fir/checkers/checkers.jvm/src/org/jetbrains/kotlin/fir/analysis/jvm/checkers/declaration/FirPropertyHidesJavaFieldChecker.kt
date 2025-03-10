/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.isDeprecationLevelHidden
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.hasContextParameters
import org.jetbrains.kotlin.fir.visibilityChecker

object FirPropertyHidesJavaFieldChecker : FirClassChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val scope = declaration.unsubstitutedScope(context)
        scope.processAllProperties { propertySymbol ->
            if (propertySymbol !is FirPropertySymbol) return@processAllProperties
            if (propertySymbol.getContainingClassSymbol() != declaration.symbol) return@processAllProperties
            if (propertySymbol.origin != FirDeclarationOrigin.Source) return@processAllProperties
            if (propertySymbol.receiverParameterSymbol != null) return@processAllProperties
            if (propertySymbol.hasContextParameters) return@processAllProperties
            if (propertySymbol.isDeprecationLevelHidden(context.session)) return@processAllProperties
            var warningReported = false
            scope.processPropertiesByName(propertySymbol.name) { fieldSymbol ->
                if (!warningReported &&
                    fieldSymbol is FirFieldSymbol &&
                    fieldSymbol.isJavaOrEnhancement &&
                    fieldSymbol.visibility != Visibilities.Private
                ) {
                    val propertyVisibility = propertySymbol.visibility
                    if (propertyVisibility == Visibilities.Private || propertyVisibility == Visibilities.PrivateToThis) {
                        @OptIn(SymbolInternals::class)
                        if (!context.session.visibilityChecker.isVisibleForOverriding(propertySymbol.fir, fieldSymbol.fir)) {
                            // As private property is visible only inside the class,
                            // it cannot hide some field that is not visible inside the same class
                            return@processPropertiesByName
                        }
                    }
                    reporter.reportOn(propertySymbol.source, FirJvmErrors.PROPERTY_HIDES_JAVA_FIELD, fieldSymbol, context)
                    warningReported = true
                }
            }
        }
    }
}
