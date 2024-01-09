/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.light.classes.symbol.base.service.getLightClassesFromFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolLightClassesParentingTestByPsi(
    configurator: AnalysisApiTestConfigurator,
    currentExtension: String,
    stopIfCompilationErrorDirectivePresent: Boolean,
) : AbstractSymbolLightClassesParentingTestBase(configurator, currentExtension, stopIfCompilationErrorDirectivePresent) {
    override fun doLightClassTest(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val elementVisitor = createLightElementsVisitor(module.directives, testServices.assertions)
        for (ktFile in ktFiles) {
            for (psiClass in getLightClassesFromFile(ktFile)) {
                psiClass.accept(elementVisitor)
            }
        }
    }
}
