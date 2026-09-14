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
import org.jetbrains.kotlin.fir.isVisible
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.resolve.calls.syntheticNamesProvider
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.types.AbstractTypeChecker
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

enum class SetterDifferentFromGetter {
    GENERAL_TYPE_MISMATCH,
    NULLABILITY_MISMATCH,
    NO_GETTER
}

data class IeData(
    val funcName: String,
    val isStatic: Boolean,
    val canUsePropertySyntax: Boolean,

    // Java setters only; the mismatch reason is null when the getter type matches.
    val setterDifferentFromGetter: SetterDifferentFromGetter?,
    val getterExistsButWithDifferentPrefix: String?,
    val setterOverload: Boolean?,
    val matchingGetterExistsButInvisible: Boolean?,

    // Not null if it is function with prefix set from Kotlin
    val setPrefixFunctionFromKotlin: Boolean?,
)

object IeChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val reporter = IEReporter(expression.source, context, reporter, FirErrors.IE_WARNING)
        val funcName = expression.calleeReference.name.identifierOrNullIfSpecial ?: return
        if (funcName.length < 4 || !(funcName.startsWith("set") || funcName.startsWith("get")) || !funcName[3].isUpperCase()) return
        if (funcName.startsWith("set") && expression.arguments.size != 1) return
        if (funcName.startsWith("get") && expression.arguments.isNotEmpty()) return
        val symbol = expression.calleeReference.symbol as? FirNamedFunctionSymbol ?: return
        val isSetter = funcName.startsWith("set")
        if (symbol.valueParameterSymbols.size != if (isSetter) 1 else 0) return
        if (symbol.valueParameterSymbols.any { it.isVararg || it.hasDefaultValue }) return

        val origin = symbol.unwrapFakeOverrides().origin
        val isJava = origin is FirDeclarationOrigin.Enhancement || origin is FirDeclarationOrigin.Java
        val receiverType = expression.dispatchReceiver?.resolvedType?.let {
            if (it.isRaw()) it.convertToNonRawVersion() else it
        }
        val scope = receiverType?.scope(
            context.session,
            context.scopeSession,
            CallableCopyTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        )
        val properties = if (receiverType != null && scope != null) syntheticProperties(expression, symbol, receiverType, scope) else emptyList()
        val matchingProperties = properties.filter { property ->
            val accessor = if (isSetter) property.setterSymbol else property.getterSymbol
            accessor?.delegateFunctionSymbol?.unwrapFakeOverrides() == symbol.unwrapFakeOverrides()
        }
        val canUsePropertySyntax = matchingProperties.any { property ->
            val getter = property.getterSymbol?.delegateFunctionSymbol ?: return@any false
            context.session.visibilityChecker.isVisible(
                getter,
                context.session,
                context.containingFileSymbol!!,
                context.containingDeclarations,
                expression.dispatchReceiver,
            )
        }
        val matchingGetterExistsButInvisible = matchingProperties.isNotEmpty() && !canUsePropertySyntax
        val namesProvider = context.session.syntheticNamesProvider
        val getterNames = namesProvider?.let { provider ->
            provider.possiblePropertyNamesByAccessorName(symbol.name)
                .flatMap { provider.possibleGetterNamesByPropertyName(it) }
                .filter { provider.setterNameByGetterName(it) == symbol.name }
                .toSet()
        }.orEmpty()
        val getters = buildList {
            if (isSetter && isJava) {
                for (name in getterNames) {
                    scope?.processFunctionsByName(name) { if (it.isGetterCandidate(symbol)) add(it) }
                }
            }
        }
        val parameterType = symbol.valueParameterSymbols.singleOrNull()?.resolvedReturnType
        val mismatch = when {
            !isSetter || !isJava || canUsePropertySyntax -> null
            getters.isEmpty() -> SetterDifferentFromGetter.NO_GETTER
            getters.any { equalTypes(it.resolvedReturnType, parameterType!!) } -> null
            getters.any {
                equalTypes(
                    it.resolvedReturnType.withNullability(false, context.session.typeContext),
                    parameterType!!.withNullability(false, context.session.typeContext),
                )
            } -> SetterDifferentFromGetter.NULLABILITY_MISMATCH
            else -> SetterDifferentFromGetter.GENERAL_TYPE_MISMATCH
        }
        val differentPrefixGetters = buildList {
            if (isSetter && isJava && scope != null) {
                val suffix = funcName.removePrefix("set")
                for (name in scope.getCallableNames().sortedBy { it.asString() }) {
                    val candidateName = name.asString()
                    if (name in getterNames || !candidateName.endsWith(suffix)) continue
                    val prefix = candidateName.removeSuffix(suffix)
                    if (prefix.isEmpty() || !prefix.all { it.isLowerCase() }) continue
                    scope.processFunctionsByName(name) {
                        if (it.isGetterCandidate(symbol) && equalTypes(it.resolvedReturnType, parameterType!!)) add(candidateName)
                    }
                }
            }
        }
        reporter(
            IeData(
                funcName, symbol.isStatic, canUsePropertySyntax, mismatch,
                differentPrefixGetters.distinct().takeIf { it.isNotEmpty() }?.joinToString(", "),
                if (isSetter && isJava) properties.any {
                    val setter = it.setterSymbol?.delegateFunctionSymbol
                    it.getterSymbol?.delegateFunctionSymbol?.name in getterNames &&
                            setter != null && setter.unwrapFakeOverrides() != symbol.unwrapFakeOverrides()
                } else null,
                matchingGetterExistsButInvisible,
                if (isSetter && !isJava) true else null,
            )
        )
    }

    private fun FirNamedFunctionSymbol.isGetterCandidate(setter: FirNamedFunctionSymbol): Boolean =
        isStatic == setter.isStatic && valueParameterSymbols.isEmpty() && typeParameterSymbols.isEmpty() &&
                receiverParameterSymbol == null && !resolvedReturnType.isUnit

    context(context: CheckerContext)
    private fun equalTypes(first: ConeKotlinType, second: ConeKotlinType): Boolean =
        AbstractTypeChecker.equalTypes(context.session.typeContext, first, second)

    context(context: CheckerContext)
    private fun syntheticProperties(
        expression: FirFunctionCall,
        symbol: FirNamedFunctionSymbol,
        receiverType: ConeKotlinType,
        scope: FirTypeScope,
    ): List<FirSyntheticPropertySymbol> {
        if (symbol.isStatic) return emptyList()
        val namesProvider = context.session.syntheticNamesProvider ?: return emptyList()
        val syntheticScope = FirSyntheticPropertiesScope.createIfSyntheticNamesProviderIsDefined(
            context.session,
            receiverType,
            scope,
            isSuperCall = expression.explicitReceiver is FirSuperReceiverExpression,
        ) ?: return emptyList()
        return buildList {
            for (propertyName in namesProvider.possiblePropertyNamesByAccessorName(symbol.name)) {
                syntheticScope.processPropertiesByName(propertyName) { property ->
                    if (property is FirSyntheticPropertySymbol) add(property)
                }
            }
        }
    }
}
