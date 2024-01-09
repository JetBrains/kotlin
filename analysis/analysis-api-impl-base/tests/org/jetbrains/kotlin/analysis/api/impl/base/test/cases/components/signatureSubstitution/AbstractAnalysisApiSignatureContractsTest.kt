/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.components.buildSubstitutor
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.math.pow

abstract class AbstractAnalysisApiSignatureContractsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        mainFile.collectDescendantsOfType<KtCallableDeclaration>().forEach {
            testContractsOnDeclaration(it, testServices)
        }
    }

    private fun testContractsOnDeclaration(
        callableDeclaration: KtCallableDeclaration,
        testServices: TestServices,
    ) {
        analyseForTest(callableDeclaration) {
            val typesToCheckOn = buildList {
                add(builtinTypes.INT)
                add(buildClassType(StandardClassIds.List) { argument(builtinTypes.LONG) })
            }

            val symbol = callableDeclaration.getSymbolOfType<KtCallableSymbol>()
            val typeParameters = buildList {
                addAll(symbol.typeParameters)
                (symbol.getContainingSymbol() as? KtClassOrObjectSymbol)?.let { addAll(it.typeParameters) }
            }
            val combinations = buildList { combinations(typesToCheckOn, persistentListOf(), typeParameters.size) }
            check(combinations.size == typesToCheckOn.size.toDouble().pow(typeParameters.size).toInt())
            val allSubstitutors = buildList {
                combinations.forEach { typesPermutation ->
                    add(buildSubstitutor { substitutions(typeParameters.zip(typesPermutation).toMap()) })
                }
            }

            allSubstitutors.forEach { substitutor ->
                testContractsOnDeclarationSymbol(symbol, substitutor, testServices)
            }
        }
    }

    private fun KtAnalysisSession.testContractsOnDeclarationSymbol(
        symbol: KtCallableSymbol,
        substitutor: KtSubstitutor,
        testServices: TestServices,
    ) {
        run {
            val substitutedViaSignature = symbol.asSignature().substitute(substitutor)
            val directlySubstituted = symbol.substitute(substitutor)
            testServices.assertions.assertEquals(directlySubstituted, substitutedViaSignature)
            testServices.assertions.assertEquals(symbol, directlySubstituted.symbol)
            testServices.assertions.assertEquals(symbol, substitutedViaSignature.symbol)
        }
        when (symbol) {
            is KtFunctionLikeSymbol -> {
                val substitutedViaSignature: KtFunctionLikeSignature<KtFunctionLikeSymbol> = symbol.asSignature().substitute(substitutor)
                val directlySubstituted: KtFunctionLikeSignature<KtFunctionLikeSymbol> = symbol.substitute(substitutor)

                testServices.assertions.assertEquals(directlySubstituted, substitutedViaSignature)
                testServices.assertions.assertEquals(symbol, directlySubstituted.symbol)
                testServices.assertions.assertEquals(symbol, substitutedViaSignature.symbol)

                checkSubstitutionResult(symbol, directlySubstituted, substitutor, testServices)
            }
            is KtVariableLikeSymbol -> {
                val substitutedViaSignature: KtVariableLikeSignature<KtVariableLikeSymbol> = symbol.asSignature().substitute(substitutor)
                val directlySubstituted: KtVariableLikeSignature<KtVariableLikeSymbol> = symbol.substitute(substitutor)

                testServices.assertions.assertEquals(directlySubstituted, substitutedViaSignature)
                testServices.assertions.assertEquals(symbol, directlySubstituted.symbol)
                testServices.assertions.assertEquals(symbol, substitutedViaSignature.symbol)

                checkSubstitutionResult(symbol, directlySubstituted, substitutor, testServices)
            }
        }
    }

    private fun KtAnalysisSession.checkSubstitutionResult(
        symbol: KtFunctionLikeSymbol,
        signature: KtFunctionLikeSignature<*>,
        substitutor: KtSubstitutor,
        testServices: TestServices,
    ) {
        testServices.assertions.assertEquals(symbol.receiverType?.let(substitutor::substitute), signature.receiverType)
        testServices.assertions.assertEquals(symbol.returnType.let(substitutor::substitute), signature.returnType)

        testServices.assertions.assertEquals(symbol.valueParameters.size, signature.valueParameters.size)

        for ((unsubstituted, substituted) in symbol.valueParameters.zip(signature.valueParameters)) {
            testServices.assertions.assertEquals(substituted.returnType, unsubstituted.returnType.let(substitutor::substitute))
        }
    }

    private fun KtAnalysisSession.checkSubstitutionResult(
        symbol: KtVariableLikeSymbol,
        signature: KtVariableLikeSignature<*>,
        substitutor: KtSubstitutor,
        testServices: TestServices,
    ) {
        testServices.assertions.assertEquals(symbol.receiverType?.let(substitutor::substitute), signature.receiverType)
        testServices.assertions.assertEquals(symbol.returnType.let(substitutor::substitute), signature.returnType)
    }

    private fun <L> MutableList<List<L>>.combinations(list: List<L>, state: PersistentList<L>, size: Int) {
        if (size == 0) {
            add(state)
        } else {
            for (e in list) {
                combinations(list, state.add(e), size - 1)
            }
        }
    }
}