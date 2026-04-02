/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtSourceLikeTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator.Companion.defaultTargetPlatformValue
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

/**
 * A universal test configurator for source-like tests that use FIR.
 *
 * It covers [AnalysisApiFirSourceTestConfigurator] and [AnalysisApiFirScriptTestConfigurator].
 *
 * @see AnalysisApiFirSourceTestConfigurator
 * @see AnalysisApiFirScriptTestConfigurator
 */
open class LLSourceLikeTestConfigurator(
    analyseInDependentSession: Boolean = false,
    override val defaultTargetPlatform: TargetPlatform = defaultTargetPlatformValue,
) : LLSourceLikeBaseTestConfigurator(analyseInDependentSession) {
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        super.configureTest(builder, disposable)

        builder.useAdditionalService<KtTestModuleFactory> { KtSourceLikeTestModuleFactory }
        AnalysisApiFirSourceTestConfigurator.configureTest(builder)
    }
}
