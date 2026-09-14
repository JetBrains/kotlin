/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirSuperReceiverExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.resolve.calls.syntheticNamesProvider
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import kotlin.reflect.full.memberProperties

class IEReporter(
    private val source: KtSourceElement?,
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter,
    private val error: KtDiagnosticFactory1<String>,
) {
    operator fun invoke(v: Any) {
        val dataStr = buildList {
            addAll(serializeData(v))
        }.joinToString("; ")
        val str = "$borderTag $dataStr $borderTag"
        reporter.reportOn(source, error, str, context)
    }

    private val borderTag: String = "KLEKLE"

    private fun serializeData(v: Any): List<String> = buildList {
        v::class.memberProperties.forEach { property ->
            add("${property.name}: ${property.getter.call(v)}")
        }
    }
}

data class IeData(
    val funcName: String,
    val isStatic: Boolean,
    val canUsePropertySyntax: Boolean,
)

object IeChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val reporter = IEReporter(expression.source, context, reporter, FirErrors.IE_WARNING)
        val funcName = expression.calleeReference.name.identifierOrNullIfSpecial ?: return
        if (funcName.length < 4 || !(funcName.startsWith("set") || funcName.startsWith("get")) || !funcName[3].isUpperCase()) return
        if (funcName.startsWith("set") && expression.arguments.size != 1) return
        if (funcName.startsWith("get") && expression.arguments.isNotEmpty()) return
        if (expression.calleeReference.symbol?.origin !is FirDeclarationOrigin.Enhancement &&
            expression.calleeReference.symbol?.origin !is FirDeclarationOrigin.Java
        ) return
        val symbol = expression.calleeReference.symbol as? FirNamedFunctionSymbol ?: return
        reporter(IeData(funcName, symbol.isStatic, canUsePropertySyntax(expression, symbol)))
    }

    context(context: CheckerContext)
    private fun canUsePropertySyntax(expression: FirFunctionCall, symbol: FirNamedFunctionSymbol): Boolean {
        if (symbol.isStatic) return false
        val namesProvider = context.session.syntheticNamesProvider ?: return false
        val receiverType = expression.dispatchReceiver?.resolvedType?.let {
            if (it.isRaw()) it.convertToNonRawVersion() else it
        } ?: return false
        val scope = receiverType.scope(
            context.session,
            context.scopeSession,
            CallableCopyTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        ) ?: return false
        val syntheticScope = FirSyntheticPropertiesScope.createIfSyntheticNamesProviderIsDefined(
            context.session,
            receiverType,
            scope,
            isSuperCall = expression.explicitReceiver is FirSuperReceiverExpression,
        ) ?: return false
        val isSetter = symbol.name.asString().startsWith("set")
        return namesProvider.possiblePropertyNamesByAccessorName(symbol.name).any { propertyName ->
            var found = false
            syntheticScope.processPropertiesByName(propertyName) { property ->
                if (property is FirSyntheticPropertySymbol) {
                    val accessor = if (isSetter) property.setterSymbol else property.getterSymbol
                    if (accessor?.delegateFunctionSymbol?.unwrapFakeOverrides() == symbol.unwrapFakeOverrides()) {
                        found = true
                    }
                }
            }
            found
        }
    }
}
