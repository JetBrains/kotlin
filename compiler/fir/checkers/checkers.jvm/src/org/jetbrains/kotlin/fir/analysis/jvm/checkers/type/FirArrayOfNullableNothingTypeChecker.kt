/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.type

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isMalformedExpandedType
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirResolvedTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression.isArrayOfNullableNothing
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

object FirArrayOfNullableNothingTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    override fun check(typeRef: FirResolvedTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.NullableNothingInReifiedPosition)) return
        val coneType = typeRef.coneType
        val fullyExpandedType = coneType.fullyExpandedType(context.session)

        /** Ignore vararg, see varargOfNothing.kt test */
        val lastContainingDeclaration = context.containingDeclarations.lastOrNull()
        val isVararg = (lastContainingDeclaration as? FirValueParameter)?.isVararg == true
        if (!isVararg && fullyExpandedType.isArrayOfNullableNothing()) {
            if (lastContainingDeclaration !is FirTypeAlias ||
                lastContainingDeclaration.expandedConeType?.isMalformedExpandedType(context, allowNullableNothing = false) == true
            ) {
                reporter.reportOn(typeRef.source, FirErrors.UNSUPPORTED, "'Array<Nothing?>' is not supported on the JVM.", context)
            }
        }
    }
}
