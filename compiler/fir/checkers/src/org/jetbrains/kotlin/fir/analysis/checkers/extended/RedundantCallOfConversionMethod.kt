/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression

object RedundantCallOfConversionMethod : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirFunctionCall) return
        val functionName = expression.calleeReference.name.asString()
        val qualifiedType = targetClassMap[functionName] ?: return

        if (expression.explicitReceiver?.isRedundant(qualifiedType) == true) {
            reporter.reportOn(expression.source, FirErrors.REDUNDANT_CALL_OF_CONVERSION_METHOD, context)
        }
    }

    private fun FirExpression.isRedundant(qualifiedClassId: ClassId): Boolean {
        val thisType = if (this is FirConstExpression<*>) {
            this.resolvedType.classId
        } else {
            when {
                resolvedType is ConeFlexibleType -> null
                psi?.parent !is KtSafeQualifiedExpression
                        && (psi is KtSafeQualifiedExpression || resolvedType.isMarkedNullable) -> null
                this.resolvedType.isMarkedNullable -> null
                else -> this.resolvedType.classId
            }
        }
        return thisType == qualifiedClassId
    }

    private val targetClassMap = hashMapOf(
        "toString" to StandardClassIds.String,
        "toDouble" to StandardClassIds.Double,
        "toFloat" to StandardClassIds.Float,
        "toLong" to StandardClassIds.Long,
        "toInt" to StandardClassIds.Int,
        "toChar" to StandardClassIds.Char,
        "toShort" to StandardClassIds.Short,
        "toByte" to StandardClassIds.Byte,
        "toULong" to StandardClassIds.ULong,
        "toUInt" to StandardClassIds.UInt,
        "toUShort" to StandardClassIds.UShort,
        "toUByte" to StandardClassIds.UByte
    )
}
