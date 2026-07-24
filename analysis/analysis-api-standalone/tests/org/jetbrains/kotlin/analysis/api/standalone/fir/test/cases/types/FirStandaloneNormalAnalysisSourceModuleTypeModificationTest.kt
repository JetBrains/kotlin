/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.types

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation.AbstractTypeModificationDslTest
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators.StandaloneModeConfigurator

@Suppress("JUnitTestCaseWithNoTests")
class FirStandaloneNormalAnalysisSourceModuleTypeModificationTest : AbstractTypeModificationDslTest() {
    override val configurator = StandaloneModeConfigurator()
}
