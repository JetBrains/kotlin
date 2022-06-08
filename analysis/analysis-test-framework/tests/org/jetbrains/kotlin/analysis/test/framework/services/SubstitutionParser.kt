/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.buildSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule

object SubstitutionParser {
    fun KtAnalysisSession.parseSubstitutors(module: TestModule, file: KtFile): List<KtSubstitutor> {
        val directives = module.directives[Directives.SUBSTITUTOR]
        if (directives.isEmpty()) return emptyList()
        val allTypeParameterSymbols = file
            .collectDescendantsOfType<KtTypeParameter>()
            .map { it.getTypeParameterSymbol() }
            .groupBy { it.name.asString() }
            .mapValues { it.value.single() }

        return directives.map { subsitutor ->
            buildSubstitutor {
                subsitutor.forEach { (typeParameterName, typeString) ->
                    val typeParameterSymbol = allTypeParameterSymbols.getValue(typeParameterName)
                    val type = with(TypeParser) { parseTypeFromString(typeString, file, allTypeParameterSymbols) }
                    substitution(typeParameterSymbol, type)
                }
            }
        }
    }

    object Directives : SimpleDirectivesContainer() {
        val SUBSTITUTOR by valueDirective("") { stringValue ->
            stringValue.trim().split(";").map { it.trim() }.map { substitution ->
                val asList = substitution.split("->").map { it.trim() }
                check(asList.size == 2) {
                    "Substitution should look like `x -> y` but was `$substitution`"
                }
                asList[0] to asList[1]
            }
        }
    }
}