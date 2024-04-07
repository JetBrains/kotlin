/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiEnumConstant
import org.jetbrains.kotlin.analysis.providers.KotlinGlobalModificationService
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.nio.file.Path

abstract class AbstractSymbolLightClassesEqualityTestBase(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val isTestAgainstCompiledCode: Boolean,
) : AbstractSymbolLightClassesTestBase(configurator) {
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
        val lightClasses = lightClassesToCheck(ktFiles, module, testServices)
        if (lightClasses.isEmpty()) return

        val testVisitor = createTestVisitor(lightClasses.first().project, testServices.assertions)
        for (lightClass in lightClasses) {
            lightClass.accept(testVisitor)
        }
    }

    private fun invalidateCaches(project: Project) {
        val globalModificationService = KotlinGlobalModificationService.getInstance(project)
        if (isTestAgainstCompiledCode) {
            globalModificationService.publishGlobalModuleStateModification()
        } else {
            globalModificationService.publishGlobalSourceOutOfBlockModification()
        }
    }

    private fun createTestVisitor(
        project: Project,
        assertions: AssertionsService,
    ): PsiElementVisitor = object : JavaElementVisitor() {
        override fun visitClass(aClass: PsiClass) {
            compareArrayElementsWithInvalidation(aClass, PsiClass::getMethods)
            compareArrayElementsWithInvalidation(aClass, PsiClass::getFields)
            compareArrayElementsWithInvalidation(aClass, PsiClass::getInnerClasses)

            super.visitClass(aClass)
        }

        override fun visitEnumConstant(enumConstant: PsiEnumConstant) {
            compareElementsWithInvalidation(enumConstant, PsiEnumConstant::getInitializingClass)

            super.visitEnumConstant(enumConstant)
        }

        private fun <T, R> compareElementsWithInvalidation(
            element: T,
            accessor: T.() -> R,
            comparator: (before: R, after: R) -> Unit = ::assertElementEquals,
        ) {
            val before = element.accessor()
            invalidateCaches(project)

            val after = element.accessor()
            comparator(before, after)
        }

        private fun <T> assertElementEquals(before: T, after: T) {
            assertions.assertEquals(before, after)
        }

        private fun <T, R : Any> compareArrayElementsWithInvalidation(element: T, accessor: T.() -> Array<R>) {
            compareElementsWithInvalidation(element, accessor) { before, after ->
                assertions.assertEquals(before.size, after.size) {
                    "Element: $element\nAccessor: $accessor"
                }

                if (before.isEmpty()) {
                    assertions.assertEquals(before, after) {
                        "Empty arrays must be the same"
                    }
                } else {
                    assertions.assertNotEquals(before, after) {
                        "Not empty arrays mustn't be equal for several invocations"
                    }
                }

                for ((index, expected) in before.withIndex()) {
                    val actual = after[index]
                    assertions.assertEquals(expected, actual) {
                        "Element: $element"
                    }
                }
            }
        }
    }

    abstract fun lightClassesToCheck(ktFiles: List<KtFile>, module: KtTestModule, testServices: TestServices): Collection<PsiClass>
}
