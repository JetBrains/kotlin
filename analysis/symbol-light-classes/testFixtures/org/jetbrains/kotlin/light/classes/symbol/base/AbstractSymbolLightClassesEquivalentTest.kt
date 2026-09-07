/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstantInitializer
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.light.classes.symbol.base.service.getLightElements
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolLightClassesEquivalentTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val lightQName = LightClassTestCommon.fqNameInTestDataFile(testDataPath.toFile())
        val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtDeclaration>(mainFile)
        val lightElements = analyzeForTest(declaration) {
            declaration.getLightElements()
        }
        testServices.assertions.assertFalse(lightElements.isEmpty())
        if (lightElements.size > 1) {
            for (lightElement in lightElements) {
                for (other in lightElements) {
                    /**
                     * Kotlin enum entry with an initializer produces two LC elements through [getLightElements]:
                     * [com.intellij.psi.PsiEnumConstant] as well as [PsiEnumConstantInitializer]. These two elements are not
                     * equivalent by design and should not be compared here.
                     */
                    if (isEnumConstantWithItsInitializer(lightElement, other)) continue

                    testServices.assertions.assertTrue(lightElement.isEquivalentTo(other)) {
                        "Light elements are not equivalent: $lightElement and $other"
                    }
                }
            }
        }
        val lightElement = lightElements.find { it.javaClass.name == lightQName }
        testServices.assertions.assertNotNull(lightElement) { "Expected $lightQName, got: " + lightElements.joinToString { it::class.java.name } }
        testServices.assertions.assertTrue(lightElement!!.isEquivalentTo(declaration)) { "Light element is not equivalent to the corresponding ktElement" }
    }

    private fun isEnumConstantWithItsInitializer(first: PsiElement, second: PsiElement): Boolean =
        first is PsiEnumConstantInitializer && first.enumConstant == second ||
                second is PsiEnumConstantInitializer && second.enumConstant == first

    override val configurator: AnalysisApiTestConfigurator
        get() = SymbolLightClassSourceJvmTestConfigurator
}
