/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.assertStableResult
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractResolveCallTest : AbstractResolveByElementTest() {
    override val resolveKind: String get() = "call"

    override fun generateResolveOutput(mainElement: KtElement, testServices: TestServices): String = analyseForTest(mainElement) {
        val call = mainElement.resolveToCall()
        val secondCall = mainElement.resolveToCall()

        ignoreStabilityIfNeeded(testServices.moduleStructure.allDirectives) {
            assertStableResult(testServices, call, secondCall)
        }

        call?.let(::stringRepresentation) ?: "null"
    }
}
