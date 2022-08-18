/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.renderClass
import org.jetbrains.kotlin.light.classes.symbol.base.service.withExtendedTypeRenderer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import java.nio.file.Path

abstract class AbstractSymbolLightClassesFacadeTest(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val stopIfCompilationErrorDirectivePresent: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override fun getRenderResult(ktFile: KtFile, testDataFile: Path, module: TestModule, project: Project): String {
        val lightClasses = getFacades(project)
        if (lightClasses.isEmpty()) return LightClassTestCommon.NOT_GENERATED_DIRECTIVE
        return withExtendedTypeRenderer((testDataFile)) {
            lightClasses.joinToString("\n\n") { it.renderClass() }
        }
    }

    private fun getFacades(
        project: Project
    ): List<KtLightClass> {
        val kotlinAsJavaSupport = KotlinAsJavaSupport.getInstance(project)
        val scope = GlobalSearchScope.allScope(project)
        val facades = kotlinAsJavaSupport.getFacadeNames(FqName.ROOT, scope).sorted()
        return facades
            .flatMap { facadeName -> kotlinAsJavaSupport.getFacadeClasses(FqName(facadeName), scope) }
            .filterIsInstance<KtLightClass>()
    }
}