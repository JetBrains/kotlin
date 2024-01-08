/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.NO_RUNTIME
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractFirLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveOverAllPhasesTest() {
    override fun checkSession(firSession: LLFirResolveSession) {
        require(firSession.isSourceSession)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        doLazyResolveTest(mainFile, testServices, renderAllFiles = true) { firResolveSession ->
            findFirDeclarationToResolve(mainFile, testServices.moduleStructure, testServices, firResolveSession)
        }
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            forTestsNotMatching("analysis/low-level-api-fir/testData/lazyResolve/noRuntime/*") {
                defaultDirectives {
                    +WITH_STDLIB
                }
            }

            forTestsMatching("analysis/low-level-api-fir/testData/lazyResolve/noRuntime/*") {
                defaultDirectives {
                    +NO_RUNTIME
                }
            }
        }
    }
}

abstract class AbstractFirSourceLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractFirOutOfContentRootLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}

abstract class AbstractFirScriptLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
