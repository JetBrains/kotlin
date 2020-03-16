/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics.checkers.call

import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.resolve.calls.isSuperReferenceExpression
import org.jetbrains.kotlin.fir.resolve.dfa.coneType
import org.jetbrains.kotlin.fir.resolve.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.resolve.diagnostics.onSource
import org.jetbrains.kotlin.fir.resolve.transformers.firClassLike
import org.jetbrains.kotlin.fir.types.classId

object FirSuperIsNotAnExpressionChecker : FirExpressionChecker<FirFunctionCall>() {
    @org.jetbrains.kotlin.fir.resolve.dfa.DfaInternals
    override fun check(functionCall: FirFunctionCall, reporter: DiagnosticReporter) {
//        println("========= DEBIG =========")
//        println(functionCall.calleeReference.name)

//        println(functionCall.dispatchReceiver.coneType?.type?.classId?.shortClassName ?: "<<<none>>>")
//        println(functionCall.extensionReceiver.typeRef)
//        println(functionCall.dispatchReceiver.typeRef)
//        functionCall.dispatchReceiver.typeRef.firClassLike()

//        println(functionCall.isSuperReferenceExpression())
//        println(functionCall.dispatchReceiver.isSuperReferenceExpression())
//        println(functionCall.explicitReceiver?.isSuperReferenceExpression() ?: "<<none>>")
//        println(functionCall.extensionReceiver.isSuperReferenceExpression())

        if (functionCall.explicitReceiver != null) {
            if (functionCall.isSuperReferenceExpression()) {
                reporter.report(functionCall.source)
            }
        }

//        println("********* DEBIG *********")
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let {
            report(Errors.SUPER_IS_NOT_AN_EXPRESSION.onSource(it, "Super can not be called as an expression"))
        }
    }
}