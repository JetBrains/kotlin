/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.firClassByPsiClassProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractFirClassByPsiClassProviderTest : AbstractAnalysisApiBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(Directives)
        }
    }

    override fun doTest(testServices: TestServices) {
        val psiClassUnderCaret =
            testServices.expressionMarkerProvider.getElementsOfTypeAtCarets<PsiClass>(testServices).single().first
        val useSiteModule = testServices.moduleStructure.modules.firstOrNull {
            Directives.USE_SITE_MODULE in it.directives
        } ?: testServices.moduleStructure.modules.single()
        val useSiteKtModule = testServices.ktTestModuleStructure.getKtTestModule(useSiteModule).ktModule
        val resolveSession = LLFirResolveSessionService.getInstance(useSiteKtModule.project).getFirResolveSessionNoCaching(useSiteKtModule)
        val firClass = resolveSession.useSiteFirSession.firClassByPsiClassProvider.getFirClass(psiClassUnderCaret)!!
        val rendered = firClass.fir.render()
        testServices.assertions.assertEqualsToTestDataFileSibling(rendered)
    }

    object Directives : SimpleDirectivesContainer() {
        val USE_SITE_MODULE by directive("Use site module to start analysis with", DirectiveApplicability.Module)
    }
}