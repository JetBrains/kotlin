/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.*

object FirUnsupportedArrayLiteralChecker : FirArrayOfCallChecker() {
    override fun check(expression: FirArrayOfCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!isInsideAnnotationCall(expression, context) &&
            (context.callsOrAssignments.isNotEmpty() || !isInsideAnnotationClass(context))
        ) {
            reporter.reportOn(
                expression.source,
                FirErrors.UNSUPPORTED,
                "Collection literals outside of annotations",
                context
            )
        }
    }

    private fun isInsideAnnotationCall(expression: FirArrayOfCall, context: CheckerContext): Boolean {
        context.callsOrAssignments.lastOrNull()?.let {
            val arguments = when (it) {
                is FirFunctionCall ->
                    if (it.typeRef.toRegularClassSymbol(context.session)?.classKind == ClassKind.ANNOTATION_CLASS) {
                        it.arguments
                    } else {
                        return false
                    }
                is FirAnnotationCall -> it.arguments
                else -> return false
            }

            return arguments.any { argument ->
                val unwrappedArguments =
                    if (argument is FirVarargArgumentsExpression) {
                        argument.arguments.map { arg -> arg.unwrapArgument() }
                    } else {
                        listOf(argument.unwrapArgument())
                    }

                for (unwrapped in unwrappedArguments) {
                    if (unwrapped == expression ||
                        unwrapped is FirArrayOfCall &&
                        unwrapped.arguments.any { arrayOfCallElement -> arrayOfCallElement.unwrapArgument() == expression }
                    ) {
                        return@any true
                    }
                }

                return@any false
            }
        }

        return false
    }

    private fun isInsideAnnotationClass(context: CheckerContext): Boolean {
        for (declaration in context.containingDeclarations.asReversed()) {
            if (declaration is FirRegularClass) {
                if (declaration.isCompanion) {
                    continue
                }

                if (declaration.classKind == ClassKind.ANNOTATION_CLASS) {
                    return true
                }
            } else if (declaration is FirValueParameter || declaration is FirPrimaryConstructor) {
                continue
            }

            break
        }

        return false
    }
}
