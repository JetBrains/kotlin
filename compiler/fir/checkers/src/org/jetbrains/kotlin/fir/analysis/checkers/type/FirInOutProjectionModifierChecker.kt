/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.checkModifiersCompatibility
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef

object FirInOutProjectionModifierChecker : FirResolvedTypeRefChecker() {
    override fun check(typeRef: FirResolvedTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        val delegatedTypeRef = typeRef.delegatedTypeRef as? FirUserTypeRef ?: return
        for (part in delegatedTypeRef.qualifier) {
            for (typeArgument in part.typeArgumentList.typeArguments) {
                checkModifiersCompatibility(typeArgument, context, reporter)
            }
        }
    }
}
