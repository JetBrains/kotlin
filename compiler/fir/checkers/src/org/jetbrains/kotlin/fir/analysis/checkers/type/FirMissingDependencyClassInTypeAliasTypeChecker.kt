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
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.abbreviatedType
import org.jetbrains.kotlin.fir.types.hasError

object FirMissingDependencyClassInTypeAliasTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        val fullyExpandedType = typeRef.coneType.fullyExpandedType().takeIf { !it.hasError() } ?: return
        val abbreviatedType = fullyExpandedType.abbreviatedType ?: return // Only type aliases are relevant

        /**
         * We can't call [org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol]
         * because sometimes RHS of type alias isn't a class, for instance, a type parameter.
         * Although in this case the [FirErrors.TYPEALIAS_SHOULD_EXPAND_TO_CLASS] is reported, the above-mentioned function returns `null`.
         * And if we used the [org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol], we would get a false-positive
         * [FirErrors.MISSING_DEPENDENCY_CLASS] in addition.
         */
        val expandedSymbol = fullyExpandedType.toSymbol()
        if (expandedSymbol == null) {
            /**
             * Use the error [FirErrors.MISSING_DEPENDENCY_CLASS] without deprecation cycle if the type arguments are not empty.
             * The reason: such a code could cause severe problems like KT-79633 because of specific of type parameters serialization in K2.
             * That's why it's better to prohibit it as soon as possible.
             */
            if (context.languageVersionSettings.supportsFeature(LanguageFeature.ForbidTypeAliasWithMissingDependencyType) ||
                fullyExpandedType.typeArguments.isNotEmpty()
            ) {
                reporter.reportOn(typeRef.source, FirErrors.MISSING_DEPENDENCY_CLASS, fullyExpandedType)
            } else {
                reporter.reportOn(typeRef.source, FirErrors.MISSING_DEPENDENCY_CLASS_IN_TYPEALIAS, fullyExpandedType, abbreviatedType)
            }
        }
    }
}