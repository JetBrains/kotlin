/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.analysis.test.data.manager.withAdditionalVariant
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import java.nio.file.Path

open class AbstractSymbolLightClassesParentingTestBase(
    configurator: AnalysisApiTestConfigurator,
    override val isTestAgainstCompiledCode: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override val variantChain: TestVariantChain
        get() = super.variantChain.withAdditionalVariant("parenting")

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(SymbolLightClassesParentingCheckDirectives)

    override fun getRenderResult(
        ktFile: KtFile,
        ktFiles: List<KtFile>,
        testDataFile: Path,
        module: KtTestModule,
        project: Project,
    ): String {
        throw IllegalStateException("This test is not rendering light elements")
    }
}
