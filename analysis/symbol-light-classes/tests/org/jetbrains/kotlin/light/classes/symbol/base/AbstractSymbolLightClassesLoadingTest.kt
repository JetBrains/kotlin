/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.SyntaxTraverser
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.renderClass
import org.jetbrains.kotlin.light.classes.symbol.base.service.withExtendedTypeRenderer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import java.nio.file.Path

abstract class AbstractSymbolLightClassesLoadingTest(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val stopIfCompilationErrorDirectivePresent: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {

    override fun getRenderResult(ktFile: KtFile, testDataFile: Path, module: TestModule, project: Project): String {
        val lightClasses = getLightClassesFromFile(ktFile, project)
        if (lightClasses.isEmpty()) return LightClassTestCommon.NOT_GENERATED_DIRECTIVE
        return withExtendedTypeRenderer(testDataFile) {
            lightClasses.joinToString("\n\n") { it.renderClass() }
        }
    }

    private fun getLightClassesFromFile(
        ktFile: KtFile,
        project: Project
    ): List<KtLightClass> {
        val ktClasses = SyntaxTraverser.psiTraverser(ktFile).filter(KtClassOrObject::class.java).toList()
        val kotlinAsJavaSupport = KotlinAsJavaSupport.getInstance(project)
        return ktClasses.mapNotNull(kotlinAsJavaSupport::getLightClass)
    }
}