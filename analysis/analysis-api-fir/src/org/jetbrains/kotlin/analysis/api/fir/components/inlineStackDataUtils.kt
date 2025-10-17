/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parents
import org.jetbrains.kotlin.analysis.api.components.DebuggerExtension
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.InlineLambdaArgument
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.isInlinable
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement

class InlineStackData(
    val capturedReifiedTypeParameterMapping: Map<FirTypeParameterSymbol, ConeKotlinType>,
    val inlineLambdaParameterMapping: Map<FirValueParameterSymbol, InlineLambdaArgument>,
    val firstNonInlineNonLocalFunInStack: KtDeclaration?,
)


internal fun retrieveInlineStackData(
    file: FirFile,
    resolutionFacade: LLResolutionFacade,
    debuggerExtension: DebuggerExtension?,
): InlineStackData {

    if (debuggerExtension == null) return InlineStackData(emptyMap(), emptyMap(), null)

    val unmappedTypeParameters = mutableSetOf<FirTypeParameterSymbol>()
    file.collectCapturedReifiedTypeParameters(unmappedTypeParameters, resolutionFacade)
    val capturedTypeParameters = unmappedTypeParameters.toSet()

    // We need to save the order to make a substitution on the correct order later
    val reifiedTypeParametersMapping = linkedMapOf<FirTypeParameterSymbol, FirTypeRef>()

    val unsubstitutedInlineLambdaParameters = mutableSetOf<FirValueParameterSymbol>()
    collectInlineLambdaParameters(file, unsubstitutedInlineLambdaParameters, resolutionFacade.useSiteFirSession)
    val inlineLambdaParameterMapping = mutableMapOf<FirValueParameterSymbol, InlineLambdaArgument>()

    // We roll back along the execution stack, until either all required type parameters are mapped on arguments, or
    // we are unable to proceed further.
    // E.g., we might reach the execution stack beginning or fail to extract relevant info from the call.
    // There are cases when a code fragment captures a reified type parameter, but we are still able to compile it
    // without reification, that is why we avoid fast-failing here when not all the type parameters are mapped.
    val stackIterator = debuggerExtension.stack.iterator()
    var depth = 0
    var firstNonInlineNonLocalFunInStack: KtDeclaration? = null
    while (stackIterator.hasNext() && (unmappedTypeParameters.isNotEmpty() || unsubstitutedInlineLambdaParameters.isNotEmpty())) {
        val previousExprPsi = stackIterator.next() ?: continue
        depth++
        updateReifiedTypeParametersInfo(previousExprPsi, resolutionFacade, unmappedTypeParameters, reifiedTypeParametersMapping)
        updateInlineLambdaInfo(
            previousExprPsi,
            resolutionFacade,
            unsubstitutedInlineLambdaParameters,
            inlineLambdaParameterMapping,
            depth
        )
        if (!stackIterator.hasNext()) {
            firstNonInlineNonLocalFunInStack = previousExprPsi.getNonLocalContainingOrThisDeclaration()
        }
    }

    val toConeTypeMapping: LinkedHashMap<FirTypeParameterSymbol, ConeKotlinType> =
        reifiedTypeParametersMapping.mapValues { (_, firTypeRef) -> firTypeRef.coneType }.toMap(LinkedHashMap())

    val typeSubstitutor = substitutorByMap(toConeTypeMapping, resolutionFacade.useSiteFirSession)

    // The parameters are ordered in the map according the order of declaring function in execution stack, e.g.:
    //
    // fun <reified T3> foo3() {
    //     ...suspension point...
    // }
    // fun <reified T2> foo2() {
    //     foo3<T2>()
    // }
    // fun <reified T1> foo1() {
    //     foo2<T1>()
    // }
    // ... entry point...
    // fun main() {
    //     foo1<Int>()
    // }
    //
    // Parameters will be ordered as T3, T2, T1, i.e. argument follows the parameter.
    // Thus, processing them in reversive order gives the transitive closure of substitution.
    for (typeParameter in toConeTypeMapping.keys.reversed().iterator()) {
        toConeTypeMapping[typeParameter] =
            typeSubstitutor.substituteOrSelf(toConeTypeMapping[typeParameter]!!)
    }

    // It's vital to leave only parameters immediately captured by code fragment, as JVM ReifiedTypeInliner does not distinguish
    // different type parameters with the same name
    // See IntelliJ test:
    // community/plugins/kotlin/jvm-debugger/test/testData/evaluation/singleBreakpoint/reifiedTypeParameters/crossfileInlining.kt
    return InlineStackData(
        toConeTypeMapping.filterKeys { it in capturedTypeParameters },
        inlineLambdaParameterMapping,
        firstNonInlineNonLocalFunInStack
    )
}

private fun updateInlineLambdaInfo(
    previousExprPsi: PsiElement,
    resolutionFacade: LLResolutionFacade,
    unsubstitutedInlineLambdaParameters: MutableSet<FirValueParameterSymbol>,
    inlineLambdaParameterMapping: MutableMap<FirValueParameterSymbol, InlineLambdaArgument>,
    depth: Int,
) {
    val inlineCall: FirCall = previousExprPsi.parents(withSelf = true)
        .filterIsInstance<KtCallExpression>()
        .firstNotNullOfOrNull { psiElement ->
            psiElement.getOrBuildFir(resolutionFacade) as? FirCall
        } ?: return
    // Retrieve param->arg mapping from the parameters default values
    val paramsWithDefaultValues = buildList {
        if (inlineCall is FirQualifiedAccessExpression) {
            val callee = inlineCall.calleeReference.toResolvedCallableSymbol()?.fir
            if (callee is FirFunction) {
                val defaultValuesMap = callee.valueParameters
                    .filter { valueParam ->
                        valueParam.defaultValue != null && valueParam.symbol in unsubstitutedInlineLambdaParameters
                    }
                    .associate { valueParam ->
                        valueParam.symbol to InlineLambdaArgument(valueParam.defaultValue!!, depth - 1)
                    }
                inlineLambdaParameterMapping.putAll(defaultValuesMap)
                addAll(defaultValuesMap.keys)
            }
        }
    }
    // Retrieve param->arg mapping from the arguments list, overwrite default values
    val paramToExpr = inlineCall.resolvedArgumentMapping?.entries?.associate { (key, value) -> value.symbol to key } ?: return
    val newlyMapped = paramToExpr.keys.intersect(unsubstitutedInlineLambdaParameters.union(paramsWithDefaultValues))
    inlineLambdaParameterMapping.putAll(newlyMapped.associateWith { InlineLambdaArgument(paramToExpr[it]!!, depth) })
    unsubstitutedInlineLambdaParameters.removeAll(newlyMapped)
    collectInlineLambdaParameters(inlineCall, unsubstitutedInlineLambdaParameters, resolutionFacade.useSiteFirSession)
}

private fun collectInlineLambdaParameters(
    element: FirElement,
    unsubstitutedInlineLambdaParameters: MutableSet<FirValueParameterSymbol>,
    session: FirSession,
) {
    element.accept(object : FirDefaultVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
            propertyAccessExpression.acceptChildren(this)
            val valueParam = propertyAccessExpression.toResolvedCallableSymbol() as? FirValueParameterSymbol ?: return
            if (valueParam.fir.isInlinable(session)) unsubstitutedInlineLambdaParameters.add(valueParam)
        }
    })
}

private fun updateReifiedTypeParametersInfo(
    previousExprPsi: PsiElement,
    resolutionFacade: LLResolutionFacade,
    unmappedTypeParameters: MutableSet<FirTypeParameterSymbol>,
    mapping: LinkedHashMap<FirTypeParameterSymbol, FirTypeRef>,
) {
    // Rolling back by parents trying to find type arguments
    // The property setter call is a special case as it's represented as `FirVariableAssignment`
    // and the type arguments should be extracted from its `lvalue`
    val typeArgumentHolder: FirQualifiedAccessExpression = previousExprPsi.parents(withSelf = true).firstNotNullOfOrNull { psiElement ->
        if (psiElement is KtElement) {
            val fir = psiElement.getOrBuildFir(resolutionFacade)
            when (fir) {
                is FirQualifiedAccessExpression -> fir
                is FirVariableAssignment -> if (fir.lValue is FirQualifiedAccessExpression) {
                    fir.lValue as FirQualifiedAccessExpression
                } else {
                    null
                }
                else -> null
            }
        } else {
            null
        }
    } ?: return

    val extractedFromPreviousExpression = extractReifiedTypeArguments(typeArgumentHolder)

    for ((extractedParam, extractedArg) in extractedFromPreviousExpression) {
        if (extractedParam in unmappedTypeParameters) {
            mapping[extractedParam] = extractedArg
            unmappedTypeParameters.remove(extractedParam)
            extractedArg.collectTypeParameters(unmappedTypeParameters)
        }
    }
}

private fun extractReifiedTypeArguments(typeArgumentsHolder: FirQualifiedAccessExpression): Map<FirTypeParameterSymbol, FirTypeRef> {
    val callableSymbol = typeArgumentsHolder.calleeReference.toResolvedCallableSymbol() ?: return emptyMap()
    return buildMap {
        for ((typeParameterSymbol, typeArgument) in callableSymbol.typeParameterSymbols.zip(typeArgumentsHolder.typeArguments)) {
            if (typeParameterSymbol.isReified && typeArgument is FirTypeProjectionWithVariance) {
                put(typeParameterSymbol, typeArgument.typeRef)
            }
        }
    }
}

private fun FirElement.collectCapturedReifiedTypeParameters(
    destination: MutableSet<FirTypeParameterSymbol>,
    resolutionFacade: LLResolutionFacade,
) {
    this.accept(object : FirDefaultVisitorVoid() {
        override fun visitElement(element: FirElement) {
            when (element) {
                is FirExpression -> {
                    val symbol = element.resolvedType.toSymbol(resolutionFacade.useSiteFirSession)
                    if (symbol is FirTypeParameterSymbol && symbol.isReified) destination.add(symbol)
                }
                is FirResolvedTypeRef -> {
                    processConeType(element.coneType)
                }
            }
            element.acceptChildren(this)
        }

        private fun processConeType(type: ConeKotlinType) {
            if (type is ConeTypeParameterType) {
                val symbol = type.lookupTag.typeParameterSymbol
                if (symbol.isReified) destination.add(symbol)
            }
            for (typeArgument in type.typeArguments) {
                typeArgument.type?.let { processConeType(it) }
            }
        }
    })
}

private fun FirTypeRef.collectTypeParameters(destination: MutableSet<FirTypeParameterSymbol>) =
    (this as? FirResolvedTypeRef)?.coneType?.collectTypeParameters(destination)

private fun ConeKotlinType.collectTypeParameters(destination: MutableSet<FirTypeParameterSymbol>) {
    if (this is ConeTypeParameterType) {
        destination.add(lookupTag.typeParameterSymbol)
        return
    }
    typeArguments.forEach { typeArgument ->
        typeArgument.type?.collectTypeParameters(destination)
    }
}