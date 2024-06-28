/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.jvm.FirJvmNamesChecker
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef

object FirFunctionalTypeParameterNameChecker : FirTypeRefChecker(MppCheckerKind.Common) {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (typeRef !is FirFunctionTypeRef) return
        for (parameter in typeRef.parameters) {
            check(parameter, context, reporter)
        }
    }

    private fun check(typeRef: FirFunctionTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        val name = typeRef.name ?: return
        val typeRefSource = typeRef.source ?: return
        FirJvmNamesChecker.checkNameAndReport(name, typeRefSource, context, reporter)
    }
}
