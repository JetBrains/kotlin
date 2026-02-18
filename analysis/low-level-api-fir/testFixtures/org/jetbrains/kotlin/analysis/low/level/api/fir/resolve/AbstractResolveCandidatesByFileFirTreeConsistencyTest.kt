/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolve

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveCandidatesByFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * [org.jetbrains.kotlin.analysis.api.components.KaResolver.resolveToCallCandidates]
 * has quite fragile and danger implementation which may easily change the original FIR tree structure.
 *
 * This test ensures that the tree structure is not affected.
 *
 * @see AbstractResolveCandidatesFirTreeConsistencyTest
 */
abstract class AbstractResolveCandidatesByFileFirTreeConsistencyTest : AbstractResolveCandidatesByFileTest() {
    override fun generateResolveOutput(mainElement: KtElement, testServices: TestServices): String {
        return testServices.assertions.assertFirTreeConsistency(mainElement.containingKtFile) {
            super.generateResolveOutput(mainElement, testServices)
        }
    }
}

/**
 * A workaround to mark the test as a non-golden one.
 *
 * It is fine to just add a synthetic prefix since [AbstractResolveCandidatesByFileFirTreeConsistencyTest] is not supposed to generate any outputs
 */
private fun List<String>.patched(): List<String> {
    val original = this
    return original + listOfNotNull(original.lastOrNull()?.let { "$it.consistency" })
}

abstract class AbstractSourceResolveCandidatesByFileFirTreeConsistencyTest : AbstractResolveCandidatesByFileFirTreeConsistencyTest() {
    override val configurator: AnalysisApiTestConfigurator =
        object : AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false) {
            override val testPrefixes: List<String>
                get() = super.testPrefixes.patched()
        }
}

abstract class AbstractScriptResolveCandidatesByFileFirTreeConsistencyTest : AbstractResolveCandidatesByFileFirTreeConsistencyTest() {
    override val configurator: AnalysisApiTestConfigurator =
        object : AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false) {
            override val testPrefixes: List<String>
                get() = super.testPrefixes.patched()
        }
}
