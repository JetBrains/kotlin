/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.relationProvider

import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.combinedMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.analysis.internal.utils.buildIndentedText
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.collections.buildSet

abstract class AbstractImplementationStateTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val ktClass = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtClassOrObject>(contextFile)
            val classSymbol = ktClass.symbol as KaClassSymbol

            val callables = collectCallables(contextFile)

            buildIndentedText {
                for (callable in callables) {
                    val state = callable.implementationState(classSymbol) ?: continue
                    appendLine(callable.callableId.toString())
                    append("    ").appendLine(state)
                }
            }
        }
        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    context(_: KaSession)
    private fun collectCallables(file: KtFile): Set<KaCallableSymbol> = buildSet {
        fun collectSupertypeCallables(classSymbol: KaClassSymbol) {
            for (superType in classSymbol.superTypes) {
                val superTypeSymbol = superType.symbol as? KaClassSymbol ?: continue
                superTypeSymbol.combinedMemberScope.callables.forEach { add(it) }
                collectSupertypeCallables(superTypeSymbol)
            }
        }

        for (declaration in file.descendantsOfType<KtDeclaration>()) {
            when (declaration) {
                is KtClassOrObject -> {
                    val classSymbol = declaration.symbol as? KaClassSymbol ?: continue
                    collectSupertypeCallables(classSymbol)
                }
                is KtCallableDeclaration -> {
                    val callableSymbol = declaration.symbol as? KaCallableSymbol ?: continue
                    add(callableSymbol)
                }
            }
        }
    }
}
