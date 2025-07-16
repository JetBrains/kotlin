/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

object RedundantCallOfConversionMethodChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val functionName = expression.calleeReference.name.asString()
        val qualifiedTypeId = targetClassMap[functionName] ?: return

        if (expression.explicitReceiver?.isRedundant(qualifiedTypeId, context.session) == true) {
            reporter.reportOn(expression.source, FirErrors.REDUNDANT_CALL_OF_CONVERSION_METHOD)
        }
    }

    private fun FirExpression.isRedundant(qualifiedClassId: ClassId, session: FirSession): Boolean {
        val thisTypeId = if (this is FirLiteralExpression) {
            resolvedType.classId
        } else {
            when {
                resolvedType is ConeFlexibleType -> null
                resolvedType.isMarkedNullable -> null
                else -> resolvedType.fullyExpandedClassId(session)
            }
        }
        return thisTypeId == qualifiedClassId
    }

    private val targetClassMap: HashMap<String, ClassId> = hashMapOf(
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