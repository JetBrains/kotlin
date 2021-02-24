/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.FirDeclarationInspector
import org.jetbrains.kotlin.fir.analysis.checkers.FirDeclarationPresenter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirMethodOfAnyImplementedInInterfaceChecker : FirRegularClassChecker(), FirDeclarationPresenter {
    private var inspector: FirDeclarationInspector? = null

    private fun getInspector(context: CheckerContext) = inspector ?: FirDeclarationInspector(this).apply {
        val anyClassId = context.session.builtinTypes.anyType.id

        context.session.symbolProvider.getClassLikeSymbolByFqName(anyClassId)
            ?.fir.safeAs<FirRegularClass>()
            ?.declarations
            ?.filterIsInstance<FirSimpleFunction>()
            ?.filter { it !is FirConstructor }
            ?.forEach {
                collect(it)
            }

        inspector = this
    }

    // We need representations that look like JVM signatures. Thus, just function names, not fully qualified ones.
    override fun StringBuilder.appendRepresentation(it: CallableId) {
        append(it.callableName)
    }

    // We need representations that look like JVM signatures. Hence, no need to represent operator.
    override fun StringBuilder.appendOperatorTag(it: FirSimpleFunction) {
        // Intentionally empty
    }

    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isInterface) {
            return
        }

        for (it in declaration.declarations) {
            if (it !is FirSimpleFunction || !it.isOverride || !it.hasBody) continue

            val inspector = getInspector(context)

            if (inspector.contains(it)) {
                reporter.reportOn(it.source, FirErrors.ANY_METHOD_IMPLEMENTED_IN_INTERFACE, context)
            }
        }
    }
}
