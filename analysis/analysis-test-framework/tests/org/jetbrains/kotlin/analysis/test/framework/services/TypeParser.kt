/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.components.buildTypeParameterType
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

object TypeParser {
    context (KtAnalysisSession)
    fun parseTypeFromString(
        stringType: String,
        contextElement: KtElement,
        scopeForTypeParameters: KtElement,
    ): KtType {
        val type = KtPsiFactory(contextElement).createType(stringType)
        return convertType(type.typeElement ?: incorrectType(type), scopeForTypeParameters)
    }

    context (KtAnalysisSession)
    private fun convertType(type: KtTypeElement, scopeForTypeParameters: KtElement): KtType =
        when (type) {
            is KtUserType -> {
                val qualifier = fullQualifier(type)
                when (val typeParameter = getSymbolByNameSafe<KtTypeParameterSymbol>(scopeForTypeParameters, qualifier)) {
                    null -> {
                        buildClassType(ClassId.topLevel(FqName(qualifier))) {
                            type.typeArguments.forEach { argument ->
                                argument(convertType(argument.typeReference?.typeElement ?: incorrectType(type), scopeForTypeParameters))
                            }
                        }
                    }
                    else -> {
                        buildTypeParameterType(typeParameter)

                    }
                }
            }
            else -> TODO(type::class.java.name)
        }

    private fun fullQualifier(type: KtUserType) =
        type.children.takeWhile { it !is KtTypeArgumentList }.joinToString(separator = ".") { it.text }

    private fun incorrectType(type: KtElement): Nothing {
        error("Invalid type `${type.text}`")
    }
}