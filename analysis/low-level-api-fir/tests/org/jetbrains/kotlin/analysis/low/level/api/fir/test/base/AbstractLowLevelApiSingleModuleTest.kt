/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import org.jetbrains.kotlin.analysis.test.framework.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.test.framework.base.AnalysisApiBasedTest


abstract class AbstractLowLevelApiSingleModuleTest : AnalysisApiBasedTest() {
    override val configurator: FrontendApiTestConfiguratorService get() = FirLowLevelFrontendApiTestConfiguratorService
}