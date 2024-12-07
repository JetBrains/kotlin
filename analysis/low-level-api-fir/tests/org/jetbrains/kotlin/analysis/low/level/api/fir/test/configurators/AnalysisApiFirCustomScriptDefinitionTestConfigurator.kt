/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.configureCustomScriptDefinitions
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

class AnalysisApiFirCustomScriptDefinitionTestConfigurator(analyseInDependentSession: Boolean) :
    AnalysisApiFirScriptTestConfigurator(analyseInDependentSession) {
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        super.configureTest(builder, disposable)
        builder.configureCustomScriptDefinitions()
    }
}
