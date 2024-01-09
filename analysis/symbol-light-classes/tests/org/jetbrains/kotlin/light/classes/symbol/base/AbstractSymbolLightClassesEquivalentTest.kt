/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolLightClassesEquivalentTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val lightQName = LightClassTestCommon.fqNameInTestDataFile(testDataPath.toFile())
        val declaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(mainFile)
        val lightElements = declaration.toLightElements()
        testServices.assertions.assertFalse(lightElements.isEmpty())
        if (lightElements.size > 1) {
            for (lightElement in lightElements) {
                for (other in lightElements) {
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

    override val configurator: AnalysisApiTestConfigurator
        get() = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}