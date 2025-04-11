/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractFirLazyDeclarationResolveTestCase.Directives.LAZY_MODE
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirCustomScriptDefinitionTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.NO_RUNTIME
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractFirLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveOverAllPhasesTest() {
    override fun checkSession(resolutionFacade: LLResolutionFacade) {
        require(resolutionFacade.isSourceSession)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        doLazyResolveTest(mainFile, testServices, OutputRenderingMode.ALL_FILES_FROM_ALL_MODULES) { resolutionFacade ->
            findFirDeclarationToResolve(mainFile, testServices, resolutionFacade)
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

            forTestsMatching("analysis/low-level-api-fir/testData/lazyResolve/withCallableMembers/*") {
                defaultDirectives {
                    LAZY_MODE.with(LazyResolveMode.WithCallableMembers)
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

abstract class AbstractFirCustomScriptDefinitionLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirCustomScriptDefinitionTestConfigurator(
        analyseInDependentSession = false,
    )
}
