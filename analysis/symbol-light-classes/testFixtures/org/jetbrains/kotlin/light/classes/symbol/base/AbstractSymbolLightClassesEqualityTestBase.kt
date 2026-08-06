/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.analysis.test.data.manager.withAdditionalVariant
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.nio.file.Path

abstract class AbstractSymbolLightClassesEqualityTestBase(
    configurator: AnalysisApiTestConfigurator,
    override val isTestAgainstCompiledCode: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override val variantChain: TestVariantChain
        get() = super.variantChain.withAdditionalVariant("equality")

    override fun getRenderResult(
        ktFile: KtFile,
        ktFiles: List<KtFile>,
        testDataFile: Path,
        module: KtTestModule,
        project: Project,
    ): String {
        throw IllegalStateException("This test is not rendering light elements")
    }

    final override fun doLightClassTest(ktFiles: List<KtFile>, module: KtTestModule, testServices: TestServices) {
        checkLightClassesEquality(
            lightClasses = lightClassesToCheck(ktFiles, module, testServices),
            isTestAgainstCompiledCode = isTestAgainstCompiledCode,
            assertions = testServices.assertions,
        )
    }

    abstract fun lightClassesToCheck(ktFiles: List<KtFile>, module: KtTestModule, testServices: TestServices): Collection<PsiClass>
}
