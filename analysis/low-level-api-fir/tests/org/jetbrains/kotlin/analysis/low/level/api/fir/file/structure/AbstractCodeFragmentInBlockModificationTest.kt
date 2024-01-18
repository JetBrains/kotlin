/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.test.assertNull

abstract class AbstractCodeFragmentInBlockModificationTest : AbstractAnalysisApiBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val targetElement = testServices.expressionMarkerProvider
            .getBottommostSelectedElementOfType(mainFile, KtElement::class.java)

        assertNull(targetElement.getNonLocalReanalyzableContainingDeclaration())

        val actualText = testInBlockModification(mainFile, mainFile, testServices, dumpFirFile = false)

        testServices.assertions.assertEqualsToTestDataFileSibling(actualText)
    }
}