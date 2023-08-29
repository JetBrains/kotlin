/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLowLevelApiCodeFragmentTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.test.assertNull

abstract class AbstractCodeFragmentInBlockModificationTest : AbstractLowLevelApiCodeFragmentTest() {
    override fun doTest(ktCodeFragment: KtCodeFragment, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val targetElement = testServices.expressionMarkerProvider
            .getBottommostSelectedElementOfType(ktCodeFragment, KtElement::class.java)

        assertNull(targetElement.getNonLocalReanalyzableContainingDeclaration())

        val actualText = testInBlockModification(ktCodeFragment, ktCodeFragment, testServices, dumpFirFile = false)

        testServices.assertions.assertEqualsToTestDataFileSibling(actualText)
    }
}