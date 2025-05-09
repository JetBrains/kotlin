/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

object TypeParser {
    fun KaSession.parseTypeFromString(
        stringType: String,
        contextElement: KtElement,
        scopeForTypeParameters: KtElement,
    ): KaType {
        val type = KtPsiFactory(contextElement.project).createType(stringType)
        return convertType(type.typeElement ?: incorrectType(type), scopeForTypeParameters)
    }

    private fun KaSession.convertType(type: KtTypeElement, scopeForTypeParameters: KtElement): KaType =
        when (type) {
            is KtUserType -> {
                val qualifier = fullQualifier(type)
                when (val typeParameter = getSymbolByNameSafe<KaTypeParameterSymbol>(scopeForTypeParameters, qualifier)) {
                    null -> {
                        buildClassType(ClassId.topLevel(FqName(qualifier))) {
                            type.typeArguments.forEach { argument ->
                                argument(convertType(argument.typeReference?.typeElement ?: incorrectType(type), scopeForTypeParameters))
                            }
                        }
                    }
                    else -> {
                        buildTypeParameterType(typeParameter) {
                            isMarkedNullable = false
                        }

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