/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractLLFirPreresolvedReversedScriptDiagnosticCompilerTestDataTest :
    AbstractLLReversedDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureCustomScriptDefinitions()
    }
}
