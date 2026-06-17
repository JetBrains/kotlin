/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.javaInterop.asPsiClass
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.symbols.classSymbol
import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.analysis.test.data.manager.withAdditionalVariant
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.nio.file.Path

abstract class AbstractSymbolLightClassesNestedClassesConsistencyTestBase(
    configurator: AnalysisApiTestConfigurator,
    override val isTestAgainstCompiledCode: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override val variantChain: TestVariantChain
        get() = super.variantChain.withAdditionalVariant("consistency")

    override fun doLightClassTest(ktFiles: List<KtFile>, module: KtTestModule, testServices: TestServices) {
        val assertions = testServices.assertions
        for (file in ktFiles) {
            analyze(file) {
                val lightClass = (file.declarations.first() as KtClassOrObject).classSymbol?.asPsiClass()!!
                checkLightClass(lightClass as KtLightClass, assertions)
            }
        }
    }

    private fun checkLightClass(lightClass: KtLightClass, assertions: AssertionsService) {
        val kotlinOrigin = lightClass.kotlinOrigin
            ?: error("No kotlin origin for ${lightClass::class.simpleName} ${lightClass.qualifiedName}")

        assertions.assertEquals(kotlinOrigin.fqName?.asString(), lightClass.qualifiedName)
        for (innerClass in lightClass.innerClasses) {
            checkLightClass(innerClass as KtLightClass, assertions)
        }
    }

    override fun getRenderResult(
        ktFile: KtFile,
        ktFiles: List<KtFile>,
        testDataFile: Path,
        module: KtTestModule,
        project: Project,
    ): String = throw UnsupportedOperationException()
}
