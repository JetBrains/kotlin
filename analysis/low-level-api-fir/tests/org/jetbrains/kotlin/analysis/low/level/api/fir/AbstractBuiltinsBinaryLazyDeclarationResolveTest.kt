/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiBuiltinsBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives

abstract class AbstractBuiltinsBinaryLazyDeclarationResolveTest : AbstractByQualifiedNameLazyDeclarationResolveTest() {
    override fun checkSession(firSession: LLResolutionFacade) {
        requireIsInstance<KaBuiltinsModule>(firSession.useSiteKtModule)
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.NO_RUNTIME
            +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
        }
    }

    override val configurator get() = AnalysisApiBuiltinsBinaryTestConfigurator

    override val outputRenderingMode: OutputRenderingMode
        get() = OutputRenderingMode.ONLY_TARGET_DECLARATION
}