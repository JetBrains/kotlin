/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.AbstractFirReflectionApiCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.load.java.JvmAbi

object FirJvmReflectionApiCallChecker : AbstractFirReflectionApiCallChecker() {
    override fun isWholeReflectionApiAvailable(context: CheckerContext): Boolean =
        context.session.symbolProvider.getClassLikeSymbolByClassId(JvmAbi.REFLECTION_FACTORY_IMPL) != null

    override fun report(source: KtSourceElement?, context: CheckerContext, reporter: DiagnosticReporter) {
        reporter.reportOn(source, FirJvmErrors.NO_REFLECTION_IN_CLASS_PATH, context)
    }
}
