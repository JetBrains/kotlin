/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.types.*

object FirStarProjectionModifierChecker : FirTypeRefChecker(MppCheckerKind.Common) {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (typeRef !is FirResolvedTypeRef) return

        val delegatedTypeRef = typeRef.delegatedTypeRef as? FirUserTypeRef ?: return
        for (part in delegatedTypeRef.qualifier) {
            for (typeArgument in part.typeArgumentList.typeArguments) {
                if (typeArgument !is FirStarProjection) continue
                val source = typeArgument.source?.takeIf { it.kind is KtRealSourceElementKind } ?: continue
                val modifierList = source.getModifierList() ?: continue

                for (modifier in modifierList.modifiers) {
                    reporter.reportOn(
                        modifier.source,
                        FirErrors.WRONG_MODIFIER_TARGET,
                        modifier.token,
                        KotlinTarget.STAR_PROJECTION.description,
                        context,
                    )
                }
            }
        }
    }
}
