/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.HASHCODE_NAME
import org.jetbrains.kotlin.fir.analysis.checkers.FirDeclarationPresenter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.TO_STRING

object FirMethodOfAnyImplementedInInterfaceChecker : FirRegularClassChecker(), FirDeclarationPresenter {
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

        for (function in declaration.declarations) {
            if (function !is FirSimpleFunction || !function.isOverride || !function.hasBody) continue
            var methodOfAny = false
            if (function.valueParameters.isEmpty() &&
                (function.name == HASHCODE_NAME || function.name == TO_STRING)
            ) {
                methodOfAny = true
            } else {
                val singleParameter = function.valueParameters.singleOrNull() ?: continue
                if (singleParameter.returnTypeRef.isNullableAny && function.name == EQUALS) {
                    methodOfAny = true
                }
            }

            if (methodOfAny) {
                withSuppressedDiagnostics(function, context) {
                    reporter.reportOn(function.source, FirErrors.METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE, context)
                }
            }
        }
    }
}
