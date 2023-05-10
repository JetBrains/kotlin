/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.isPrimitive
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object FirSynchronizedByPrimitiveOrValueClassChecker : FirFunctionCallChecker() {
    private val synchronizedFunctionCallId = CallableId(FqName.topLevel(Name.identifier("kotlin")), Name.identifier("synchronized"))

    private fun FirTypeRef.isValueOrPrimitive(session: FirSession): Boolean =
        coneTypeOrNull?.let { it.isPrimitive || it.isValueClass(session) } == true

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val function = expression.calleeReference.toResolvedFunctionSymbol() ?: return
        if (function.callableId == synchronizedFunctionCallId) {
            val argument = expression.resolvedArgumentMapping?.filterValues { it.name == Name.identifier("lock") }?.keys?.singleOrNull() ?: return
            if (argument.typeRef.isValueOrPrimitive(context.session)) {
                reporter.reportOn(argument.source, FirErrors.FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES, argument.typeRef.coneType, context)
            }
        }
    }
}