/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.light.classes.symbol.base.AbstractSymbolLightClassesTestBase
import org.jetbrains.kotlin.light.classes.symbol.decompiled.test.configurators.SymbolLightClassesDecompiledJvmTestConfigurator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.nio.file.Path

abstract class AbstractSymbolLightClassesMatcherForLibraryTest :
    AbstractSymbolLightClassesTestBase(SymbolLightClassesDecompiledJvmTestConfigurator),
    SymbolLightClassesDeclarationMatcher {

    override val isTestAgainstCompiledCode: Boolean = true

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(SymbolLightClassesDeclarationMatchingDirectives)

    override fun doLightClassTest(
        ktFiles: List<KtFile>,
        module: KtTestModule,
        testServices: TestServices,
    ) {
        checkLightClassesDeclarationMatching(
            matcher = this,
            ktFiles = ktFiles,
            testDataPath = testDataPath,
            assertions = testServices.assertions,
        )
    }

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
