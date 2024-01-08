/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.checkSubTypes
import org.jetbrains.kotlin.fir.analysis.checkers.findContextReceiverListSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.types.*

object FirContextReceiversTypeChecker : FirTypeRefChecker(MppCheckerKind.Platform) {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (typeRef !is FirResolvedTypeRef) return
        if (typeRef.source?.kind is KtFakeSourceElementKind) return
        if (!typeRef.coneType.hasContextReceivers) return
        val source = typeRef.source?.findContextReceiverListSource() ?: return

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) {
            if (checkSubTypes(typeRef.coneType.contextReceiversTypes(context.session), context)) {
                reporter.reportOn(
                    source,
                    FirErrors.SUBTYPING_BETWEEN_CONTEXT_RECEIVERS,
                    context
                )
            }
            return
        }

        reporter.reportOn(
            source,
            FirErrors.UNSUPPORTED_FEATURE,
            LanguageFeature.ContextReceivers to context.languageVersionSettings,
            context
        )
    }
}

