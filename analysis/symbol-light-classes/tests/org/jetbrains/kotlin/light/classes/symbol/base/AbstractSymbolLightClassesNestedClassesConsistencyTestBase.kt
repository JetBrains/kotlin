/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.nio.file.Path

abstract class AbstractSymbolLightClassesNestedClassesConsistencyTestBase(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val isTestAgainstCompiledCode: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
    override fun doLightClassTest(ktFiles: List<KtFile>, module: KtTestModule, testServices: TestServices) {
        val assertions = testServices.assertions
        for (file in ktFiles) {
            val lightClass = (file.declarations.first() as KtClassOrObject).toLightClass()!!
            checkLightClass(lightClass, assertions)
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
