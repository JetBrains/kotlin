/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.light.classes.symbol.base.service.getLightClassesFromFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

open class AbstractSymbolLightClassesEqualityByPsiTest(
    configurator: AnalysisApiTestConfigurator,
    currentExtension: String,
    stopIfCompilationErrorDirectivePresent: Boolean
) : AbstractSymbolLightClassesEqualityTestBase(configurator, currentExtension, stopIfCompilationErrorDirectivePresent) {
    override fun lightClassesToCheck(ktFiles: List<KtFile>, module: KtTestModule, testServices: TestServices): Collection<PsiClass> {
        return ktFiles.flatMap { getLightClassesFromFile(it) }
    }
}
