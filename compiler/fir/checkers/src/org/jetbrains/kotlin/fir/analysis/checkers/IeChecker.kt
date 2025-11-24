/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isFun
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirSamConversionExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.firClassLike
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.equalTypes
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
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
    val type: String? = null,
    val boundType: String? = null,
    val call: String? = null,
    val isReified: Boolean? = null,
    val oneLevelRedundant: Boolean? = null,
    val upperBound: Boolean? = null,
    val upperBoundedByTp: Boolean? = null,
)

object FirMyChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val report = IEReporter(expression.source, context, reporter, FirErrors.IE_DIAGNOSTIC)
        val symbol = expression.toResolvedCallableSymbol() ?: return
        val usedParameters =
            buildSet {
                symbol.fir.returnTypeRef.coneType.forEachType { type ->
                    if (type is ConeTypeParameterType) {
                        add(type.lookupTag.typeParameterSymbol)
                    }
                }
                (symbol.fir as? FirFunction)?.let { function ->
                    function.valueParameters.forEachIndexed { index, argument ->
                        val samUsed = expression.argumentList.arguments.getOrNull(index) is FirSamConversionExpression
                        val regClass = argument.returnTypeRef.firClassLike(context.session) as? FirRegularClass
                        val abstractMethod = abstractMethod(regClass)
                        if (samUsed && abstractMethod != null) {
                            regClass!!
                            val isUsed = regClass.typeParameters.map { tp ->
                                abstractMethod.fir.returnTypeRef.coneType.forEachType {
                                    if (it is ConeTypeParameterType && it.lookupTag.typeParameterSymbol == tp.symbol) {
                                        return@map true
                                    }
                                }
                                abstractMethod.fir.valueParameters.forEach { arg ->
                                    arg.returnTypeRef.coneType.forEachType {
                                        if (it is ConeTypeParameterType && it.lookupTag.typeParameterSymbol == tp.symbol) {
                                            return@map true
                                        }
                                    }
                                }
                                false
                            }
                            argument.returnTypeRef.coneType.typeArguments.forEachIndexed { index, typeProjection ->
                                if (isUsed[index]) {
                                    if (typeProjection is ConeKotlinTypeProjection) {
                                        typeProjection.type.forEachType {
                                            if (it is ConeTypeParameterType) {
                                                add(it.lookupTag.typeParameterSymbol)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            argument.returnTypeRef.coneType.forEachType { type ->
                                if (type is ConeTypeParameterType) {
                                    add(type.lookupTag.typeParameterSymbol)
                                }
                            }
                        }
                    }
                }
            }

        expression.typeArguments.indices.forEach { index ->
            if (!expression.typeArguments[index].isExplicit) return@forEach
            val type = (expression.typeArguments[index] as? FirTypeProjectionWithVariance)?.typeRef?.coneType ?: return@forEach
            val param = symbol.typeParameterSymbols[index]
            val originalParam = symbol.originalOrSelf().typeParameterSymbols.getOrNull(index)

            val singleBound = if (param.resolvedBounds.size == 1) param.resolvedBounds.first().coneType else null
            val originalSingleBound = if (originalParam?.resolvedBounds?.size == 1) originalParam.resolvedBounds.first().coneType else null

            val upperBound = singleBound?.equalTypes(type, context.session) ?: false
            val upperBoundedByTp = originalSingleBound is ConeTypeParameterType
            val oneLevelRedundant = param !in usedParameters
            report(
                IEData(
                    type = type.renderReadable(),
                    boundType = singleBound?.renderReadable(),
                    call = symbol.callableIdAsString(),
                    isReified = param.isReified,
                    oneLevelRedundant = oneLevelRedundant,
                    upperBound = upperBound,
                    upperBoundedByTp = upperBoundedByTp,
                )
            )
        }
    }

    context(context: CheckerContext)
    fun abstractMethod(funInterface: FirRegularClass?): FirNamedFunctionSymbol? {
        if (funInterface == null) return null

        val scope = funInterface.unsubstitutedScope()

        for (name in scope.getCallableNames()) {
            val functions = scope.getFunctions(name)

            for (function in functions) {
                if (function.isAbstract) {
                    return function
                }
            }
        }
        return null
    }
}
