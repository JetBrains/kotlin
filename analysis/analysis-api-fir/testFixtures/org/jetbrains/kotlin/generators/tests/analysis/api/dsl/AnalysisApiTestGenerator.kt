/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.dsl

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactory
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite

class AnalysisApiTestGenerator(
    val suite: TestGroupSuite,
    val configuratorFactories: List<AnalysisApiTestConfiguratorFactory>,
) {
    fun run(init: AnalysisApiTestGroup.() -> Unit) {
        AnalysisApiTestGroup(this, { true }, null).init()
    }
}