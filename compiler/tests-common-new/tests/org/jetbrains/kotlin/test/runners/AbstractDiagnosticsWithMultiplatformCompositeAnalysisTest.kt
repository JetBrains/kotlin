/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.MultiplatformDiagnosticsDirectives

abstract class AbstractDiagnosticsWithMultiplatformCompositeAnalysisTest : AbstractDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            +MultiplatformDiagnosticsDirectives.MULTIPLATFORM_COMPOSITE_ANALYSIS_MODE
            LanguageSettingsDirectives.LANGUAGE with "+MultiPlatformProjects"
        }
    }
}
