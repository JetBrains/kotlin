/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.asJava.renderClass
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.nio.file.Path

abstract class AbstractSymbolLightClassesAnnotationEqualityTest(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val isTestAgainstCompiledCode: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override fun doLightClassTest(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val directives = module.directives
        val expectedAnnotations = directives[Directives.EXPECTED]
        val unexpectedAnnotations = directives[Directives.UNEXPECTED]
        val qualifiersToCheck = expectedAnnotations + unexpectedAnnotations
        testServices.assertions.assertTrue(qualifiersToCheck.isNotEmpty()) { error("Nothing to check") }

        val actualLightDeclaration = findLightDeclaration(ktFiles, module, testServices)

        val annotationsFromFindAnnotation = mutableSetOf<PsiAnnotation>()
        val modifierList = actualLightDeclaration.modifierList!!
        for ((qualifier, isExpected) in qualifiersToCheck) {
            val actual = modifierList.hasAnnotation(qualifier)
            testServices.assertions.assertEquals(expected = isExpected, actual = actual) {
                "$qualifier isExpected: $isExpected, but $actual is found"
            }

            val psiAnnotation = modifierList.findAnnotation(qualifier)
            if (isExpected) {
                testServices.assertions.assertNotNull(psiAnnotation)
            }

            psiAnnotation?.let(annotationsFromFindAnnotation::add)
        }

        testServices.assertions.assertEquals(expected = expectedAnnotations.size, actual = annotationsFromFindAnnotation.size)
        val annotations = modifierList.annotations.toList()
        for (annotation in annotationsFromFindAnnotation) {
            testServices.assertions.assertContainsElements(collection = annotations, annotation)
        }

        val unexpectedQualifiers = unexpectedAnnotations.mapTo(hashSetOf(), AnnotationData::qualifierName)
        for (annotation in annotations) {
            val qualifiedName = annotation.qualifiedName
            testServices.assertions.assertTrue(qualifiedName !in unexpectedQualifiers) {
                "$qualifiedName is unexpected annotation"
            }
        }

        compareResults(module, testServices) {
            val psiClass = actualLightDeclaration.parentOfType<PsiClass>(withSelf = true) ?: error("PsiClass is not found")
            psiClass.renderClass()
        }
    }

    override fun getRenderResult(ktFile: KtFile, ktFiles: List<KtFile>, testDataFile: Path, module: TestModule, project: Project): String {
        throw UnsupportedOperationException()
    }

    private fun findLightDeclaration(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices): PsiMember {
        val directives = module.directives
        val lightElementClassQualifier = directives.singleValue(Directives.PSI)
        val declaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(ktFiles.first())
        val lightElements = declaration.toLightElements()
        val actualLightDeclaration = lightElements.find { it::class.qualifiedName == lightElementClassQualifier }
            ?: error("$lightElementClassQualifier is not found in ${lightElements.map { it::class.qualifiedName }}")

        return actualLightDeclaration as PsiMember
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    private object Directives : SimpleDirectivesContainer() {
        val EXPECTED by valueDirective(description = "Expected annotation qualifier to check equality") {
            AnnotationData(qualifierName = it, isExpected = true)
        }

        val UNEXPECTED by valueDirective(description = "Unexpected annotation qualifier to check equality") {
            AnnotationData(qualifierName = it, isExpected = false)
        }

        val PSI by stringDirective(description = "Qualified name of expected light declaration")
    }
}

private data class AnnotationData(val qualifierName: String, val isExpected: Boolean)
