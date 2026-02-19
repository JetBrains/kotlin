/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtCodeFragmentTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.services.DependencyKindModuleStructureTransformer
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.configureLibraryCompilationSupport
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

class AnalysisApiFirCodeFragmentTestConfigurator(
    analyseInDependentSession: Boolean
) : AnalysisApiFirSourceLikeTestConfigurator(analyseInDependentSession) {
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        super.configureTest(builder, disposable)

        builder.apply {
            useAdditionalService<KtTestModuleFactory> { KtCodeFragmentTestModuleFactory }

            @OptIn(TestInfrastructureInternals::class)
            useModuleStructureTransformers(DependencyKindModuleStructureTransformer)

            configureLibraryCompilationSupport()
        }
    }
}
