/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.fileAnnotationProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.fileAnnotations
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.TestAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbols
import org.jetbrains.kotlin.analysis.api.resolution.calls
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiBinaryLibraryIndexingMode
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiIndexingConfiguration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractFileAnnotationProviderTest : AbstractAnalysisApiBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)

        builder.apply {
            useAdditionalService { AnalysisApiIndexingConfiguration(AnalysisApiBinaryLibraryIndexingMode.INDEX_STUBS) }
        }
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = analyze(mainModule.ktModule) {
            copyAwareAnalyzeForTest(mainFile) { contextFile ->
                buildString {
                    contextFile.accept(object : KtTreeVisitorVoid() {
                        override fun visitReferenceExpression(expression: KtReferenceExpression) {
                            expression.resolveToCall()?.calls?.forEach { call ->
                                for (symbol in call.symbols()) {
                                    if (symbol !is KaDeclarationSymbol) continue
                                    appendLine(symbol.renderFileAnnotations())
                                }
                            }
                            super.visitReferenceExpression(expression)
                        }

                        override fun visitTypeReference(typeReference: KtTypeReference) {
                            (typeReference.type.abbreviation ?: typeReference.type as? KaClassType)?.let {
                                appendLine(it.symbol.renderFileAnnotations())
                            }
                            super.visitTypeReference(typeReference)
                        }

                        override fun visitDeclaration(declaration: KtDeclaration) {
                            appendLine(declaration.symbol.renderFileAnnotations())
                            return super.visitDeclaration(declaration)
                        }
                    }, null)
                }
            }
        }
        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    context(session: KaSession)
    private fun KaDeclarationSymbol.renderFileAnnotations(): String =
        "${stringRepresentation(this)}; ${TestAnnotationRenderer.renderAnnotations(session, fileAnnotations, prefix = "fileAnnotations")}"
}
