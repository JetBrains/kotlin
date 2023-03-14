/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.LightClassTestCommon
import org.jetbrains.kotlin.asJava.renderClass
import org.jetbrains.kotlin.light.classes.symbol.base.service.getLightClassesFromFile
import org.jetbrains.kotlin.light.classes.symbol.base.service.withExtendedTypeRenderer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import java.nio.file.Path

abstract class AbstractSymbolLightClassesByPsiTest(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val isTestAgainstCompiledCode: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override fun getRenderResult(ktFile: KtFile, ktFiles: List<KtFile>, testDataFile: Path, module: TestModule, project: Project): String {
        val lightClasses = ktFiles.flatMap { getLightClassesFromFile(it) }
        if (lightClasses.isEmpty()) return LightClassTestCommon.NOT_GENERATED_DIRECTIVE
        return withExtendedTypeRenderer(testDataFile) {
            lightClasses.sortedBy { it.name }.joinToString("\n\n") { it.renderClass() }
        }
    }
}
