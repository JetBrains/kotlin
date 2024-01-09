/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolLightClassesStructureByFqNameTest(
    configurator: AnalysisApiTestConfigurator,
    testPrefix: String,
    stopIfCompilationErrorDirectivePresent: Boolean,
) : AbstractSymbolLightClassesStructureTestBase(configurator, testPrefix, stopIfCompilationErrorDirectivePresent) {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun doLightClassTest(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val result = prettyPrint {
            val fqName = module.directives.singleValue(Directives.FQ_NAME)
            val psiClass = findLightClass(fqName, ktFiles.first().project)
            psiClass?.let { handleClass(it) } ?: append(LightClassTestCommon.NOT_GENERATED_DIRECTIVE)
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(result, testPrefix = testPrefix)

        doTestInheritors(ktFiles, testServices)
    }

    private object Directives : SimpleDirectivesContainer() {
        val FQ_NAME by stringDirective(description = "Light class to render")
    }
}
