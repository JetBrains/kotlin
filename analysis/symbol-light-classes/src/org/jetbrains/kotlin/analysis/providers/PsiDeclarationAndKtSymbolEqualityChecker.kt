/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode

// TODO replace with structural type comparison?
internal object PsiDeclarationAndKtSymbolEqualityChecker {
    fun KaSession.representsTheSameDeclaration(psi: PsiMethod, symbol: KaCallableSymbol): Boolean {
        // TODO: receiver type comparison?
        if (!returnTypesMatch(psi, symbol)) return false
        if (!typeParametersMatch(psi, symbol)) return false
        if (symbol is KaFunctionLikeSymbol && !valueParametersMatch(psi, symbol)) return false
        return true
    }

    private fun KaSession.returnTypesMatch(psi: PsiMethod, symbol: KaCallableSymbol): Boolean {
        if (symbol is KaConstructorSymbol) return psi.isConstructor
        return psi.returnType?.let {
            isTheSameTypes(
                psi,
                it,
                symbol.returnType,
                KaTypeMappingMode.RETURN_TYPE,
            )
        } ?: false
    }

    private fun typeParametersMatch(psi: PsiMethod, symbol: KaCallableSymbol): Boolean {
        // PsiMethod for constructor won't have type parameters
        if (symbol is KaConstructorSymbol) return psi.isConstructor
        if (psi.typeParameters.size != symbol.typeParameters.size) return false
        psi.typeParameters.zip(symbol.typeParameters) { psiTypeParameter, typeParameterSymbol ->
            if (psiTypeParameter.name != typeParameterSymbol.name.asString()) return false
            // TODO: type parameter bounds comparison
        }
        return true
    }

    private fun KaSession.valueParametersMatch(psi: PsiMethod, symbol: KaFunctionLikeSymbol): Boolean {
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
                    KaTypeMappingMode.VALUE_PARAMETER,
                    valueParameterSymbol.isVararg,
                    psiParameter.isVarArgs,
                )
            ) return false
        }
        return true
    }

    private fun KaSession.isTheSameTypes(
        context: PsiMethod,
        psi: PsiType,
        ktType: KaType,
        mode: KaTypeMappingMode = KaTypeMappingMode.DEFAULT,
        isVararg: Boolean = false,
        isVarargs: Boolean = false, // isVarargs == isVararg && last param
    ): Boolean {
        // Shortcut: primitive void == Unit as a function return type
        if (psi == PsiTypes.voidType() && ktType.isUnit) return true
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
