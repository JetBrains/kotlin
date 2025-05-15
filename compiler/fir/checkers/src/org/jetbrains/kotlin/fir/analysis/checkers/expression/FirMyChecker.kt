/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.isClassLikeVisible
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.getContainingFile
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visibilityChecker
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

    private val borderTag: String = "ROMANV"

    @OptIn(SymbolInternals::class)
    private fun serializeLocation(): String {
        val filename = context.containingFileSymbol?.sourceFile?.run { path ?: name } ?: "unknown"
        val mapping = context.containingFileSymbol?.fir?.sourceFileLinesMapping
        val loc = source?.startOffset?.let { mapping?.getLineAndColumnByOffset(it) }
        return "loc: ${filename}:${loc?.first}:${loc?.second}"
    }

    private fun serializeData(v: IEData): List<String> = buildList {
        v::class.memberProperties.forEach { property ->
            add("${property.name}: ${property.getter.call(v)}")
        }
    }
}


data class IEData(
    val exprType: String? = null,
    val exposeKind: String? = null,
    val wholeType: String? = null,
    val exposedType: String? = null,
    val hasSuppression: String? = null,
    val reasonInTheSameFile: String? = null,
)

object FirMyChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        if (expression !is FirExpression) return
        val report = IEReporter(expression.source, context, reporter, FirErrors.MY_IE_ERROR)

        val hasSuppression = context.suppressedDiagnostics.contains("INVISIBLE_MEMBER") ||
                context.suppressedDiagnostics.contains("INVISIBLE_REFERENCE")

        val returnType = expression.resolvedType

        val useSiteFile = context.containingFileSymbol
        fun FirClassSymbol<*>.isInTheSameFile(): Boolean {
            return context.session.firProvider.getContainingFile(this)?.symbol == useSiteFile
        }

        fun ConeKotlinType.checkType(exposeKind: String, isReturnType: Boolean = false) {
            isTypeVisibilityBroken()?.let { reason ->
                if (isReturnType && reason.isCompanion && expression is FirResolvedQualifier) return
                report(
                    IEData(
                        exprType = expression::class.simpleName,
                        exposeKind = exposeKind,
                        wholeType = renderForDebugging(),
                        exposedType = reason.classId.asString(),
                        hasSuppression = hasSuppression.toString(),
                        reasonInTheSameFile = reason.isInTheSameFile().toString()
                    )
                )
            }
        }

        returnType.checkType("return_type", true)
        if (expression is FirFunctionCall) {
            expression.argumentList.arguments.forEach {
                it.resolvedType.checkType("value_argument")
            }
            expression.typeArguments.forEach { typeArgument ->
                typeArgument.toConeTypeProjection().type?.checkType("type_argument")
            }
        }
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext)
    private fun ConeKotlinType.isTypeVisibilityBroken(): FirClassSymbol<*>? {

        val visibilityChecker = context.session.visibilityChecker
        val classSymbol = toClassSymbol(context.session)
        val containingFile = context.containingFileSymbol
        if (classSymbol == null || containingFile == null) return null
        if (
            !visibilityChecker.isClassLikeVisible(
                symbol = classSymbol,
                session = context.session,
                useSiteFileSymbol = containingFile,
                containingDeclarations = context.containingDeclarations
            )
        ) {
            return classSymbol
        }
        for (typeArgument in typeArguments) {
            if (typeArgument is ConeKotlinTypeProjection) {
                val nested = typeArgument.type.isTypeVisibilityBroken()
                if (nested != null) return nested
            }
        }
        return null
    }
}
