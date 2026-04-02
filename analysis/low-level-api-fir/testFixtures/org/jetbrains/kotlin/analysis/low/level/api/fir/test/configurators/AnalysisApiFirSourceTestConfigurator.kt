/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtSourceTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.services.DependencyKindModuleStructureTransformer
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.configureLibraryCompilationSupport
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator.Companion.defaultTargetPlatformValue
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

/**
 * Test configurator for FIR-based source tests.
 *
 * Consider using [LLSourceLikeTestConfigurator] once not only source tests are expected.
 *
 * @see LLSourceLikeTestConfigurator
 */
open class AnalysisApiFirSourceTestConfigurator(
    analyseInDependentSession: Boolean,
    override val defaultTargetPlatform: TargetPlatform = defaultTargetPlatformValue
) : LLSourceLikeBaseTestConfigurator(analyseInDependentSession) {
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        super.configureTest(builder, disposable)

        builder.useAdditionalService<KtTestModuleFactory> { KtSourceTestModuleFactory }
        configureTest(builder)
    }

    companion object {
        fun configureTest(builder: TestConfigurationBuilder): Unit = with(builder) {
            @OptIn(TestInfrastructureInternals::class)
            useModuleStructureTransformers(DependencyKindModuleStructureTransformer)

            configureLibraryCompilationSupport()
        }
    }
}
