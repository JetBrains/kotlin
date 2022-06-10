/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.psi.PsiComment
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.buildSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

object SubstitutionParser {
    context(KtAnalysisSession)
    fun parseSubstitutor(declaration: KtCallableDeclaration): KtSubstitutor {
        val comment = declaration.firstChild as PsiComment
        return parseSubstitutor(comment, declaration)
    }

    context(KtAnalysisSession)
    fun parseSubstitutor(ktFile: KtFile, declaration: KtCallableDeclaration): KtSubstitutor {
        val comment = ktFile.children.filterIsInstance<PsiComment>().single { it.text.startsWith(SUBSTITUTOR_PREFIX) }
        return parseSubstitutor(comment, declaration)
    }


    context(KtAnalysisSession)
    fun parseSubstitutor(comment: PsiComment, scopeForTypeParameters: KtElement): KtSubstitutor {
        val directivesAsString = comment.text.trim()
        check(directivesAsString.startsWith(SUBSTITUTOR_PREFIX))
        val substitutorAsMap = parseSubstitutions(directivesAsString.removePrefix(SUBSTITUTOR_PREFIX))

        val allTypeParameterSymbols = scopeForTypeParameters
            .collectDescendantsOfType<KtTypeParameter>()
            .map { it.getTypeParameterSymbol() }
            .groupBy { it.name.asString() }
            .mapValues { it.value.single() }

        return buildSubstitutor {
            substitutorAsMap.forEach { (typeParameterName, typeString) ->
                val typeParameterSymbol = allTypeParameterSymbols.getValue(typeParameterName)
                val type = TypeParser.parseTypeFromString(typeString, scopeForTypeParameters, allTypeParameterSymbols)
                substitution(typeParameterSymbol, type)
            }
        }
    }

    private fun parseSubstitutions(substitutionsAsString: String): List<Pair<String, String>> =
        substitutionsAsString.trim().split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { substitution ->
                val asList = substitution.split("->").map { it.trim() }
                check(asList.size == 2) {
                    "Substitution should look like `x -> y` but was `$substitution`"
                }
                asList[0] to asList[1]
            }

    const val SUBSTITUTOR_PREFIX = "// SUBSTITUTOR:"
}