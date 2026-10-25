/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.toEffectiveVisibilityOrNull
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirSuperReceiverExpression
import org.jetbrains.kotlin.fir.isVisible
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.resolve.calls.syntheticNamesProvider
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.name.Name
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
    NO_GETTER,
    TYPES_ARE_EQUAL,
}

data class IeData(
    val funcName: String,
    val funcId: String,
    val canUsePropertySyntax: Boolean,

    val setterN: Int,
    val getterN: Int,
    val setterShape: String,
    val getterShape: String,

    val mainGetterUnique: Boolean?,

    val setterDifferentFromGetter: SetterDifferentFromGetter?,

    val differentVisibilityOfGetterAndSetter: Boolean?,
)

object IeChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext)
    private fun isInterestingSetter(funcName: String, expression: FirFunctionCall): Boolean {
        if (funcName.length < 4 || !(funcName.startsWith("set") || !funcName[3].isUpperCase())) return false
        if (funcName == "setOf") return false
//        if (funcName.startsWith("set") && expression.arguments.size != 1) return
//        val symbol = expression.calleeReference.symbol as? FirNamedFunctionSymbol ?: return
//        val isSetter = funcName.startsWith("set")
//        if (symbol.valueParameterSymbols.size != if (isSetter) 1 else 0) return
//        if (symbol.valueParameterSymbols.any { it.isVararg || it.hasDefaultValue }) return
        return true
    }

    private fun String.setterNameToGetter(): String {
        return "get" + removePrefix("set")
    }

    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val reporter = IEReporter(expression.source, context, reporter, FirErrors.IE_WARNING)

        val funcName = expression.calleeReference.name.identifierOrNullIfSpecial ?: return
        val funcId = (try {
            expression.calleeReference.resolved?.symbol?.packageFqName()?.asString() ?: "_."
        } catch (_: RuntimeException) {
            "_."
        } + ".$funcName")

        val isSetter = isInterestingSetter(funcName, expression)
        if (!isSetter) return

        val symbol = expression.calleeReference.resolved?.symbol as? FirNamedFunctionSymbol ?: return
        if (symbol.isStatic) return

//        val origin = symbol.unwrapFakeOverrides().origin
//        val isJava = origin is FirDeclarationOrigin.Enhancement || origin is FirDeclarationOrigin.Java

        val receiverType = expression.dispatchReceiver?.resolvedType ?: return
        val scope = receiverType.scope(
            context.session,
            context.scopeSession,
            CallableCopyTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        ) ?: return

        val properties = syntheticProperties(expression, symbol, receiverType, scope)
        val matchingProperties = properties.filter { property ->
            property.setterSymbol?.delegateFunctionSymbol?.unwrapFakeOverrides() == symbol.unwrapFakeOverrides()
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

        val setterOverloads = mutableListOf<FirNamedFunctionSymbol>()
        scope.processFunctionsByName(Name.identifier(funcName)) { setterOverloads.add(it) }
        val getterOverloads = mutableListOf<FirNamedFunctionSymbol>()
        scope.processFunctionsByName(Name.identifier(funcName.setterNameToGetter())) { getterOverloads.add(it) }

        val setterN = setterOverloads.size
        val getterN = getterOverloads.size
        val setterShape = setterOverloads.map { it.fir.valueParameters.size }.distinct().sorted().joinToString(", ")
        val getterShape = getterOverloads.map { it.fir.valueParameters.size }.distinct().sorted().joinToString(", ")

        val mainGetter = getterOverloads.singleOrNull { it.fir.valueParameters.isEmpty() }
        val mainGetterVisibility = mainGetter?.fir?.visibility?.toEffectiveVisibilityOrNull(null)
        val setterVisibility = setterOverloads
            .filter { it.fir.valueParameters.size == 1 }
            .map { it.fir.visibility.toEffectiveVisibilityOrNull(null) }

        val differentVisibilityOfGetterAndSetter = mainGetterVisibility?.let { getVis -> setterVisibility.any { getVis != it } }


        val parameterType = symbol.valueParameterSymbols.singleOrNull()?.resolvedReturnType
        val mismatch = parameterType?.let { parameterType ->
            when {
                canUsePropertySyntax -> null
                getterN == 0 -> SetterDifferentFromGetter.NO_GETTER
                mainGetter == null -> null
                equalTypes(mainGetter.resolvedReturnType, parameterType) -> SetterDifferentFromGetter.TYPES_ARE_EQUAL
                equalTypes(
                    mainGetter.resolvedReturnType.withNullability(false, context.session.typeContext),
                    parameterType.withNullability(false, context.session.typeContext),
                ) -> SetterDifferentFromGetter.NULLABILITY_MISMATCH
                else -> SetterDifferentFromGetter.GENERAL_TYPE_MISMATCH
            }
        }

        reporter(
            IeData(
                funcName = funcName,
                funcId = funcId,
                setterN = setterN,
                getterN = getterN,
                setterShape = setterShape,
                getterShape = getterShape,
                canUsePropertySyntax = canUsePropertySyntax,
                mainGetterUnique = if (getterN == 0) null else mainGetter != null,
                setterDifferentFromGetter = mismatch,
                differentVisibilityOfGetterAndSetter = differentVisibilityOfGetterAndSetter,
            )
        )
    }

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
