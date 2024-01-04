/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.asJava.PsiClassRenderer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import java.nio.file.Path

abstract class AbstractSymbolLightClassesByFqNameTest(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val isTestAgainstCompiledCode: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override fun getRenderResult(ktFile: KtFile, ktFiles: List<KtFile>, testDataFile: Path, module: TestModule, project: Project): String {
        return LightClassTestCommon.getActualLightClassText(
            testDataFile.toFile(),
            { fqName -> findLightClass(fqName, ktFile) },
            LightClassTestCommon::removeEmptyDefaultImpls,
            if (isTestAgainstCompiledCode) MembersFilterForCompiledClasses else PsiClassRenderer.MembersFilter.DEFAULT,
        )
    }
}

private object MembersFilterForCompiledClasses : PsiClassRenderer.MembersFilter {
    override fun includeMethod(psiMethod: PsiMethod): Boolean {
        // Exclude methods for local functions.
        // JVM_IR generates local functions (and some lambdas) as private methods in the surrounding class.
        // Such methods are private and have names such as 'foo$...'.
        // They are not a part of the public API, and are not represented in the light classes.
        // NB this is a heuristic, and it will obviously fail for declarations such as 'private fun `foo$bar`() {}'.
        // However, it allows writing code in more or less "idiomatic" style in the light class tests
        // without thinking about private ABI and compiler optimizations.
        if (psiMethod.modifierList.hasExplicitModifier(PsiModifier.PRIVATE)) {
            return '$' !in psiMethod.name
        }

        return super.includeMethod(psiMethod)
    }
}
