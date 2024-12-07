/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiLibraryBaseTestServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtSourceTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.services.DependencyKindModuleStructureTransformer
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.configureLibraryCompilationSupport
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestServices

open class AnalysisApiFirSourceTestConfigurator(
    analyseInDependentSession: Boolean
) : AnalysisApiFirSourceLikeTestConfigurator(analyseInDependentSession) {
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        super.configureTest(builder, disposable)

        builder.apply {
            useAdditionalService<KtTestModuleFactory> { KtSourceTestModuleFactory }

            @OptIn(TestInfrastructureInternals::class)
            useModuleStructureTransformers(DependencyKindModuleStructureTransformer)

            configureLibraryCompilationSupport()
        }
    }

    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.serviceRegistrars + AnalysisApiLibraryBaseTestServiceRegistrar
}
