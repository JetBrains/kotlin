/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCheckNotNullCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirSafeCallExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntegerConstantOperatorType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralConstantType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.ConeStubTypeForTypeVariableInSubtyping
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType
import org.jetbrains.kotlin.fir.types.isAny
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.types.renderReadable
import org.jetbrains.kotlin.fir.types.resolvedType
import kotlin.reflect.full.memberProperties

class IEReporter(
    private val source: KtSourceElement?,
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter,
    private val error: KtDiagnosticFactory1<String>,
) {
    operator fun invoke(v: IEData) {
        val dataStr = buildList {
            addAll(serializeData(v))
        }.joinToString("; ")
        val str = "$borderTag $dataStr $borderTag"
        reporter.reportOn(source, error, str, context)
    }

    private val borderTag: String = "KLEKLE"

    private fun serializeData(v: IEData): List<String> = buildList {
        v::class.memberProperties.forEach { property ->
            add("${property.name}: ${property.getter.call(v)}")
        }
    }
}

data class IEData(
    val simplifiedType: String? = null,
    val type: String? = null,
    val isAny: Boolean? = null,
)

context(context: CheckerContext)
fun ConeKotlinType.simplifyType(): ConeKotlinType? = when (this) {
    is ConeFlexibleType -> upperBound.simplifyType()
    is ConeDefinitelyNotNullType -> original.simplifyType()
    is ConeCapturedType -> leastUpperBound(context.session).simplifyType()
    is ConeIntegerConstantOperatorType -> this
    is ConeIntegerLiteralConstantType -> this
    is ConeIntersectionType -> this
    is ConeTypeParameterType -> leastUpperBound(context.session).simplifyType()
    is ConeLookupTagBasedType -> this
    is ConeStubTypeForTypeVariableInSubtyping -> null
    is ConeTypeVariableType -> null
}


object IECheckerBB : FirCheckNotNullCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCheckNotNullCall) {
        try {
            val report = IEReporter(expression.source, context, reporter, FirErrors.IE_DIAGNOSTIC)

            val argument = expression.argumentList.arguments.first()
            val type = argument.resolvedType
            val simplifiedType = argument.resolvedType.simplifyType()


            if (simplifiedType == null || simplifiedType.isAny || simplifiedType.isNullableAny) {
                report(
                    IEData(
                        isAny = simplifiedType?.isNullableAny,
                        type = type.renderReadable(),
                        simplifiedType = simplifiedType?.renderReadable(),
                    )
                )
            }
        } catch (_: Exception) {
        }
    }
}
