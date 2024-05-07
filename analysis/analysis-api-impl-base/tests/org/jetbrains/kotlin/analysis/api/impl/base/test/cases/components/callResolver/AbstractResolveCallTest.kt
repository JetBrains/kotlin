/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveCallTest : AbstractResolveTest() {
    override fun doResolutionTest(mainElement: KtElement, testServices: TestServices) {
        val actual = analyseForTest(mainElement) {
            val call = mainElement.resolveCall()
            call?.let(::stringRepresentation) ?: "null"
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
