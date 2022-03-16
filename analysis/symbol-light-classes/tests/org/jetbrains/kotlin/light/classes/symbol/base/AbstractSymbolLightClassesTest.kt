/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestConfiguratorService
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import java.nio.file.Path

abstract class AbstractSymbolLightClassesTest(
    configurator: AnalysisApiTestConfiguratorService,
    override val currentExtension: String,
    override val stopIfCompilationErrorDirectivePresent: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override fun getRenderResult(ktFile: KtFile, testDataFile: Path, module: TestModule, project: Project): String {
        return LightClassTestCommon.getActualLightClassText(
            testDataFile.toFile(),
            { fqName -> findLightClass(fqName, project) },
            LightClassTestCommon::removeEmptyDefaultImpls
        )
    }

    private fun findLightClass(fqname: String, project: Project): PsiClass? {
        return JavaElementFinder
            .getInstance(project)
            .findClass(fqname, GlobalSearchScope.allScope(project))
    }
}