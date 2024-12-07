/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractAnalysisApiSpecificAnnotationOnDeclarationTest : AbstractAnalysisApiBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val ktDeclaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(mainFile)
        val classIdString = mainModule.testModule.directives.singleValue(Directives.CLASS_ID)

        val actual = analyseForTest(ktDeclaration) {
            val declarationSymbol = ktDeclaration.symbol as KaAnnotatedSymbol
            val annotationList = declarationSymbol.annotations
            val classId = ClassId.fromString(classIdString)
            val renderer = DebugSymbolRenderer()
            fun renderAnnotation(application: KaAnnotation): String = buildString {
                appendLine("${KtDeclaration::class.simpleName}: ${ktDeclaration::class.simpleName} ${ktDeclaration.name}")
                append(renderer.renderAnnotationApplication(useSiteSession, application))
            }

            testServices.assertions.assertTrue(classId in annotationList) {
                "ClassId $classId is not found in the annotation list"
            }

            val rawList = renderAnnotation(annotationList[classId].single())
            val resolvedList = renderAnnotation(annotationList.single { it.classId == classId })
            testServices.assertions.assertEquals(resolvedList, rawList) {
                "Result before and after resolve are different"
            }

            resolvedList
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private object Directives : SimpleDirectivesContainer() {
        val CLASS_ID by stringDirective("ClassId of expected annotation")
    }
}

