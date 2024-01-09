/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeCreator

import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KtTypeRendererForDebug
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.TypeParser
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractBuildClassTypeTest : AbstractAnalysisApiBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(Directives)
        }
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val typeString = mainModule.directives.singleValue(Directives.CLASS_TYPE)
        val actual = analyseForTest(mainFile) {
            val ktType = TypeParser.parseTypeFromString(typeString, mainFile, mainFile)
            buildString {
                appendLine("originalTypeString: $typeString")
                appendLine(
                    "ktType: ${
                        ktType.render(
                            renderer = KtTypeRendererForDebug.WITH_QUALIFIED_NAMES,
                            position = Variance.INVARIANT,
                        )
                    }"
                )
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private object Directives : SimpleDirectivesContainer() {
        val CLASS_TYPE by stringDirective("Class type to create")
    }
}
