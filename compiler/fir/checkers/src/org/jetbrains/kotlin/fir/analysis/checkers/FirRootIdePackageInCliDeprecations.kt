/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirResolvedQualifierChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.FirResolvedSymbolOrigin
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

object FirRootIdePackageDeprecatedInCliQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirResolvedQualifier) {
        if (expression.resolvedSymbolOrigin == FirResolvedSymbolOrigin.QualifiedWithDeprecatedRootIdePackage) {
            reporter.reportOn(expression.source, FirErrors.ROOT_IDE_PACKAGE_DEPRECATED)
        }
    }
}

object FirRootIdePackageDeprecatedInCliTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        if (typeRef.resolvedSymbolOrigin == FirResolvedSymbolOrigin.QualifiedWithDeprecatedRootIdePackage) {
            reporter.reportOn(typeRef.source, FirErrors.ROOT_IDE_PACKAGE_DEPRECATED)
        }
    }
}