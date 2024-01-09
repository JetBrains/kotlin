/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiModifiablePsiTestServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.testFramework.runWriteAction

/**
 * [AbstractLowLevelApiModifiablePsiTest] applies additional configuration to support the modification of PSI during the test.
 *
 * NOTE: Modifiable PSI tests must not be used until KT-63650 is fixed.
 */
abstract class AbstractLowLevelApiModifiablePsiTest : AbstractAnalysisApiBasedTest() {
    override val configurator: AnalysisApiTestConfigurator get() = AnalysisApiFirModifiablePsiSourceTestConfigurator

    abstract fun doTestWithPsiModification(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            runWriteAction {
                doTestWithPsiModification(mainFile, testServices.moduleStructure, testServices)
            }
        }
    }
}

object AnalysisApiFirModifiablePsiSourceTestConfigurator : AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false) {
    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar> = buildList {
        addAll(super.serviceRegistrars)
        add(AnalysisApiModifiablePsiTestServiceRegistrar)
    }
}
