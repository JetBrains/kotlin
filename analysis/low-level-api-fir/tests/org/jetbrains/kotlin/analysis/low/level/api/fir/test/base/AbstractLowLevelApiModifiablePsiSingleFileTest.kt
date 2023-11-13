/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import org.jetbrains.kotlin.analysis.api.impl.base.test.configurators.AnalysisApiModifiablePsiTestServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.testFramework.runWriteAction

/**
 * [AbstractLowLevelApiModifiablePsiSingleFileTest] applies additional configuration to support the modification of PSI during the test.
 */
abstract class AbstractLowLevelApiModifiablePsiSingleFileTest : AbstractLowLevelApiSingleFileTest() {
    override val configurator: AnalysisApiTestConfigurator get() = AnalysisApiFirModifiablePsiSourceTestConfigurator

    abstract fun doTestWithPsiModification(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices)

    final override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction {
                WriteCommandAction.runWriteCommandAction(ktFile.project) {
                    doTestWithPsiModification(ktFile, moduleStructure, testServices)
                }
            }
        }
    }
}

object AnalysisApiFirModifiablePsiSourceTestConfigurator : AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false) {
    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar> = buildList {
        addAll(super.serviceRegistrars)
        add(AnalysisApiModifiablePsiTestServiceRegistrar)
    }

    override val isWriteAccessAllowed: Boolean get() = true
}
