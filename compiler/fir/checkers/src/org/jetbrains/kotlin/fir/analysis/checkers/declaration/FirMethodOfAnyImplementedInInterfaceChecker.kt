/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.FirDeclarationInspector
import org.jetbrains.kotlin.fir.analysis.checkers.FirDeclarationPresenter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirMethodOfAnyImplementedInInterfaceChecker : FirMemberDeclarationChecker(), FirDeclarationPresenter {
    private var inspector: FirDeclarationInspector? = null

    private fun getInspector(context: CheckerContext) = inspector ?: FirDeclarationInspector(this).apply {
        val anyClassId = context.session.builtinTypes.anyType.id

        context.session.firSymbolProvider.getClassLikeSymbolByFqName(anyClassId)
            ?.fir.safeAs<FirRegularClass>()
            ?.declarations
            ?.filterIsInstance<FirSimpleFunction>()
            ?.filter { it !is FirConstructor }
            ?.forEach {
                collect(it)
            }

        inspector = this
    }

    override fun represent(it: FirSimpleFunction) = buildFunctionRepresentation {
        typeArguments = it.typeParameters.joinToString { represent(it) }
        it.receiverTypeRef?.let {
            receiver = represent(it)
        }
        name = it.name.asString()
        parameters = it.valueParameters.joinToString { represent(it) }
    }

    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirClass<*> || declaration.classKind != ClassKind.INTERFACE) {
            return
        }

        for (it in declaration.declarations) {
            val inspector = getInspector(context)

            if (it is FirSimpleFunction && inspector.contains(it) && it.body != null && it.isOverride) {
                reporter.report(it.source)
            }
        }
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(FirErrors.ANY_METHOD_IMPLEMENTED_IN_INTERFACE.on(it)) }
    }
}