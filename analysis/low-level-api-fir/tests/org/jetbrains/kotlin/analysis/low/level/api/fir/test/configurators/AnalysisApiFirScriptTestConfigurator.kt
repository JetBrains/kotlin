/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiScriptTestServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtScriptModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

class AnalysisApiFirScriptTestConfigurator(analyseInDependentSession: Boolean) :
    AnalysisApiFirSourceLikeTestConfigurator(analyseInDependentSession) {
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        super.configureTest(builder, disposable)

        builder.apply {
            useAdditionalService<KtModuleFactory> { KtScriptModuleFactory }
        }
    }

    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar> = super.serviceRegistrars + AnalysisApiScriptTestServiceRegistrar
}
