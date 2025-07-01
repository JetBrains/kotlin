/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames.HASHCODE_NAME
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.isEquals
import org.jetbrains.kotlin.fir.declarations.processAllDeclaredCallables
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.util.OperatorNameConventions.TO_STRING

object FirMethodOfAnyImplementedInInterfaceChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (!declaration.isInterface) {
            return
        }

        declaration.symbol.processAllDeclaredCallables(context.session) { function ->
            if (function !is FirNamedFunctionSymbol || !function.hasBody) return@processAllDeclaredCallables
            var methodOfAny = false
            if (function.valueParameterSymbols.isEmpty() &&
                function.contextParameterSymbols.isEmpty() &&
                function.receiverParameterSymbol == null &&
                (function.name == HASHCODE_NAME || function.name == TO_STRING)
            ) {
                methodOfAny = true
            } else if (function.isEquals(context.session)) {
                methodOfAny = true
            }

            if (methodOfAny) {
                reporter.reportOn(function.source, FirErrors.METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE)
            }
        }
    }
}
