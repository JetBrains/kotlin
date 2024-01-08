/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode

// TODO replace with structural type comparison?
internal object PsiDeclarationAndKtSymbolEqualityChecker {
    fun KtAnalysisSession.representsTheSameDeclaration(psi: PsiMethod, symbol: KtCallableSymbol): Boolean {
        // TODO: receiver type comparison?
        if (!returnTypesMatch(psi, symbol)) return false
        if (!typeParametersMatch(psi, symbol)) return false
        if (symbol is KtFunctionLikeSymbol && !valueParametersMatch(psi, symbol)) return false
        return true
    }

    private fun KtAnalysisSession.returnTypesMatch(psi: PsiMethod, symbol: KtCallableSymbol): Boolean {
        if (symbol is KtConstructorSymbol) return true
        return psi.returnType?.let {
            isTheSameTypes(
                psi,
                it,
                symbol.returnType,
                KtTypeMappingMode.RETURN_TYPE,
            )
        } ?: false
    }

    private fun typeParametersMatch(psi: PsiMethod, symbol: KtCallableSymbol): Boolean {
        if (psi.typeParameters.size != symbol.typeParameters.size) return false
        psi.typeParameters.zip(symbol.typeParameters) { psiTypeParameter, typeParameterSymbol ->
            if (psiTypeParameter.name != typeParameterSymbol.name.asString()) return false
            // TODO: type parameter bounds comparison
        }
        return true
    }

    private fun KtAnalysisSession.valueParametersMatch(psi: PsiMethod, symbol: KtFunctionLikeSymbol): Boolean {
        val valueParameterCount = if (symbol.isExtension) symbol.valueParameters.size + 1 else symbol.valueParameters.size
        if (psi.parameterList.parametersCount != valueParameterCount) return false
        if (symbol.isExtension) {
            val psiParameter = psi.parameterList.parameters[0]
            if (symbol.receiverType?.let { isTheSameTypes(psi, psiParameter.type, it) } != true) return false
        }
        val offset = if (symbol.isExtension) 1 else 0
        symbol.valueParameters.forEachIndexed { index, valueParameterSymbol ->
            val psiParameter = psi.parameterList.parameters[index + offset]
            // The type of `vararg` value param at last v.s. non-last is mapped differently:
            //   * last -> ellipsis type
            //   * non-last -> array type
            // In [PsiParameter], we only know whether it `isVarArgs`.
            // If that's set to `true`, obviously symbol's `isVararg` should be `true`.
            // But, `isVarArgs` being `false` doesn't mean it is not `vararg`.
            // It may be the case that it's just not the last value parameter.
            if (psiParameter.isVarArgs && !valueParameterSymbol.isVararg) {
                return false
            }
            if (!isTheSameTypes(
                    psi,
                    psiParameter.type,
                    valueParameterSymbol.returnType,
                    KtTypeMappingMode.VALUE_PARAMETER,
                    valueParameterSymbol.isVararg,
                    psiParameter.isVarArgs,
                )
            ) return false
        }
        return true
    }

    private fun KtAnalysisSession.isTheSameTypes(
        context: PsiMethod,
        psi: PsiType,
        ktType: KtType,
        mode: KtTypeMappingMode = KtTypeMappingMode.DEFAULT,
        isVararg: Boolean = false,
        isVarargs: Boolean = false, // isVarargs == isVararg && last param
    ): Boolean {
        // Shortcut: primitive void == Unit as a function return type
        if (psi == PsiType.VOID && ktType.isUnit) return true
        val ktTypeRendered = ktType.asPsiType(context, allowErrorTypes = true, mode) ?: return false
        return if (isVararg) {
            if (isVarargs) {
                // last vararg
                PsiEllipsisType(ktTypeRendered) == psi
            } else {
                // non-last vararg
                PsiArrayType(ktTypeRendered) == psi
            }
        } else {
            ktTypeRendered == psi
        }
    }
}
