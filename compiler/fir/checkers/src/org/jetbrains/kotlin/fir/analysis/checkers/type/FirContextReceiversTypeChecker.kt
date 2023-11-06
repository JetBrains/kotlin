/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.types.*

object FirContextReceiversTypeChecker : FirTypeRefChecker() {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) return
        if (typeRef !is FirResolvedTypeRef) return
        val source = typeRef.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        if (typeRef.coneType.hasContextReceivers) {
            reporter.reportOn(
                source,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.ContextReceivers to context.languageVersionSettings,
                context
            )
        }
    }
}

