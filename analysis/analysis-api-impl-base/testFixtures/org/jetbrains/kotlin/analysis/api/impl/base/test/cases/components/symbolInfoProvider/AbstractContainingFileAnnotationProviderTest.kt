/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.fileAnnotationProvider

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.TestAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiBinaryLibraryIndexingMode
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiIndexingConfiguration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractContainingFileAnnotationProviderTest : AbstractAnalysisApiBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)

        builder.apply {
            useAdditionalService { AnalysisApiIndexingConfiguration(AnalysisApiBinaryLibraryIndexingMode.INDEX_STUBS) }
        }
    }

    @OptIn(KtExperimentalApi::class)
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = analyze(mainModule.ktModule) {
            copyAwareAnalyzeForTest(mainFile) { contextFile ->
                val targetSymbols = LinkedHashMap<KaDeclarationSymbol, String>()

                fun register(symbol: KaSymbol) {
                    if (symbol !is KaDeclarationSymbol) return
                    targetSymbols.computeIfAbsent(symbol) { stringRepresentation(it) }
                }

                contextFile.accept(object : KtTreeVisitorVoid() {
                    override fun visitElement(element: PsiElement) {
                        if (element is KtResolvable) {
                            element.resolveSymbols().forEach(::register)
                        }

                        super.visitElement(element)
                    }

                    override fun visitDeclaration(declaration: KtDeclaration) {
                        register(declaration.symbol)
                        return super.visitDeclaration(declaration)
                    }
                })

                buildString {
                    for ([symbol, stringRepresentation] in targetSymbols.entries.sortedBy { it.value }) {
                        val annotations = symbol.containingFileAnnotations ?: continue
                        val annotationsString = TestAnnotationRenderer.renderAnnotations(useSiteSession, annotations, prefix = "")
                        append(stringRepresentation).append(": ").appendLine(annotationsString)
                    }
                }
            }
        }
        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
