/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType

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
        return psi.returnType?.let { isTheSameTypes(psi, it, symbol.returnType, isVararg = false) }
            ?: false
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
            if (symbol.receiverType?.let { isTheSameTypes(psi, psiParameter.type, it, isVararg = false) } != true) return false
        }
        val offset = if (symbol.isExtension) 1 else 0
        symbol.valueParameters.forEachIndexed { index, valueParameterSymbol ->
            val psiParameter = psi.parameterList.parameters[index + offset]
            if (valueParameterSymbol.name.asString() != psiParameter.name) return false
            if (valueParameterSymbol.isVararg != psiParameter.isVarArgs) return false
            if (!isTheSameTypes(psi, psiParameter.type, valueParameterSymbol.returnType, valueParameterSymbol.isVararg)) return false
        }
        return true
    }

    private fun KtAnalysisSession.isTheSameTypes(
        context: PsiMethod,
        psi: PsiType,
        ktType: KtType,
        isVararg: Boolean = false
    ): Boolean {
        // Shortcut: primitive void == Unit as a function return type
        if (psi == PsiType.VOID && ktType.isUnit) return true
        val ktTypeRendered = ktType.asPsiType(context) ?: return false
        val rendered = if (isVararg) ktTypeRendered.createArrayType() else ktTypeRendered
        return rendered == psi
    }
}