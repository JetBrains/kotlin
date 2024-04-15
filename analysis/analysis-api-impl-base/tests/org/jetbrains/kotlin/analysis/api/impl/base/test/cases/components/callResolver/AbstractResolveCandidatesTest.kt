/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveByElementTest
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveCandidatesTest : AbstractResolveByElementTest() {
    override fun doResolveTest(element: KtElement, testServices: TestServices) {
        super.doResolveTest(element, testServices)

        val actual = analyseForTest(element) {
            val candidates = element.collectCallCandidatesOld()
            renderCandidates(candidates)
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}
