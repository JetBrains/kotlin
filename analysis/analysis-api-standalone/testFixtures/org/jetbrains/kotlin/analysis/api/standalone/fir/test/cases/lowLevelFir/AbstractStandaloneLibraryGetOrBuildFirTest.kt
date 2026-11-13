/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.lowLevelFir

import org.jetbrains.kotlin.analysis.api.standalone.fir.test.configurators.StandaloneModeLibraryBinaryDecompiledTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLibraryGetOrBuildFirTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator

/**
 * Runs the library `getOrBuildFir` tests in Standalone mode, which handles binary libraries differently from the IDE mode. The FIR of
 * library declarations is deserialized from library files instead of being built from stubs, so it has no PSI. To find the FIR declaration
 * of a decompiled declaration, the IDE mode looks for the FIR declaration whose PSI is the decompiled declaration, which isn't possible
 * here. Instead, Standalone mode takes the deserialized FIR declarations with the same name, such as overloads, and picks the one whose
 * signature matches the decompiled declaration: its receiver type, parameters, type parameters and return type. The IDE mode tests don't
 * cover any of this.
 *
 * The tests share the test data of [AbstractLibraryGetOrBuildFirTest], and only the results that differ from the IDE mode ones are recorded
 * in `.standalone.fir.txt` files. This way, the test data also shows how the FIR of library declarations differs between the two modes.
 */
abstract class AbstractStandaloneLibraryGetOrBuildFirTest : AbstractLibraryGetOrBuildFirTest() {
    override val configurator: AnalysisApiTestConfigurator =
        StandaloneModeLibraryBinaryDecompiledTestConfigurator(AnalysisApiTestConfigurator.defaultTargetPlatformValue)
}
