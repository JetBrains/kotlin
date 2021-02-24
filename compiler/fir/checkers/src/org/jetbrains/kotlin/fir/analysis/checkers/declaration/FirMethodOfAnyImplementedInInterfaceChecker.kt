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

    @Suppress("DuplicatedCode")
    override fun represent(it: FirSimpleFunction) = buildString {
        append('<')
        it.typeParameters.forEach {
            appendRepresentation(it)
            append(',')
        }
        append('>')
        append('[')
        it.receiverTypeRef?.let {
            appendRepresentation(it)
        }
        append(']')
        append(it.name.asString())
        append('(')
        it.valueParameters.forEach {
            appendRepresentation(it)
            append(',')
        }
        append(')')
    }

    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isInterface) {
            return
        }

        for (it in declaration.declarations) {
            val inspector = getInspector(context)

            if (it is FirSimpleFunction && inspector.contains(it) && it.body != null && it.isOverride) {
                reporter.reportOn(it.source, FirErrors.ANY_METHOD_IMPLEMENTED_IN_INTERFACE, context)
            }
        }
    }
}
