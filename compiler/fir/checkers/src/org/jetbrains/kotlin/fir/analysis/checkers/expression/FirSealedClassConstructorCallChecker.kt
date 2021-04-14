/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirSealedClassConstructorCallChecker : FirQualifiedAccessChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val constructorFir = expression.calleeReference.safeAs<FirResolvedNamedReference>()
            ?.resolvedSymbol
            ?.fir.safeAs<FirConstructor>()
            ?: return

        val typeFir = constructorFir.returnTypeRef.coneType
            .safeAs<ConeClassLikeType>()
            ?.lookupTag?.toSymbol(context.session)
            ?.fir as? FirRegularClass
            ?: return

        if (typeFir.status.modality == Modality.SEALED) {
            reporter.reportOn(expression.source, FirErrors.SEALED_CLASS_CONSTRUCTOR_CALL, context)
        }
    }
}
