/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedSingleModuleTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractLightClassUtilTest : AbstractAnalysisApiBasedSingleModuleTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val ktFile = ktFiles.single()
        val declaration = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(ktFile)
        val lightElements = declaration.toLightElements()
        testServices.assertions.assertFalse(lightElements.isEmpty())
        val directives = module.directives
        val expectedLightElements = directives[Directives.EXPECTED]
        testServices.assertions.assertEquals(expectedLightElements.size, lightElements.size) {
            "Found ${lightElements.map { it.javaClass.name }}"
        }
        lightElements.forEachIndexed { index, element ->
            testServices.assertions.assertEquals(expectedLightElements[index], element.javaClass.name)
        }
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    private object Directives : SimpleDirectivesContainer() {
        val EXPECTED by stringDirective(description = "Expected light classes")
    }
}