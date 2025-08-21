/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirFunctionTypeRefChecker
import org.jetbrains.kotlin.fir.analysis.jvm.FirJvmNamesChecker
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef

object FirFunctionalTypeParameterNameChecker : FirFunctionTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirFunctionTypeRef) {
        for (parameter in typeRef.parameters) {
            check(parameter)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun check(typeRef: FirFunctionTypeParameter, ) {
        val name = typeRef.name ?: return
        val typeRefSource = typeRef.source
        FirJvmNamesChecker.checkNameAndReport(name, typeRefSource)
    }
}
