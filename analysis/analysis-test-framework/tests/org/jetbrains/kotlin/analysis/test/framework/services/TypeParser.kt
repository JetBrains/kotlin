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
    fun KtAnalysisSession.parseTypeFromString(
        stringType: String,
        ktFile: KtFile,
        typeParameterByName: Map<String, KtTypeParameterSymbol>
    ): KtType {
        val type = KtPsiFactory(ktFile).createType(stringType)
        return convertType(type.typeElement ?: incorrectType(type), typeParameterByName)
    }

    private fun KtAnalysisSession.convertType(type: KtTypeElement, typeParameterByName: Map<String, KtTypeParameterSymbol>): KtType =
        when (type) {
            is KtUserType -> {
                val qualifier = fullQualifier(type)
                if (qualifier in typeParameterByName) {
                    buildTypeParameterType(typeParameterByName.getValue(qualifier))
                } else {
                    buildClassType(ClassId.topLevel(FqName(qualifier))) {
                        type.typeArguments.forEach { argument ->
                            argument(convertType(argument.typeReference?.typeElement ?: incorrectType(type), typeParameterByName))
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