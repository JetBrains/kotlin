/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.lowLevelFir

import org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators.StandaloneModeLibraryBinaryDecompiledTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLibraryGetOrBuildFirTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator

/**
 * In Standalone mode, FIR declarations of binary libraries are deserialized from class files and have no PSI, so the FIR declaration
 * for a decompiled declaration is found by comparing their components with
 * [LLStandaloneFirElementByPsiElementChooser][org.jetbrains.kotlin.analysis.api.standalone.fir.services.LLStandaloneFirElementByPsiElementChooser].
 */
abstract class AbstractStandaloneLibraryGetOrBuildFirTest : AbstractLibraryGetOrBuildFirTest() {
    override val configurator: AnalysisApiTestConfigurator =
        StandaloneModeLibraryBinaryDecompiledTestConfigurator(AnalysisApiTestConfigurator.defaultTargetPlatformValue)
}
