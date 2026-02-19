/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator

/**
 * This test is supposed to validate the source stubs output
 */
abstract class AbstractSourceStubsTest : AbstractStubsTest() {
    override val outputFileExtension: String get() = "stubs.txt"
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
    override val stubsTestEngine: StubsTestEngine get() = SourceStubsTestEngine
}
