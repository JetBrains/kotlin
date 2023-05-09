/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiEnumConstant
import org.jetbrains.kotlin.analysis.providers.createAllLibrariesModificationTracker
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.nio.file.Path

abstract class AbstractSymbolLightClassesEqualityTestBase(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val isTestAgainstCompiledCode: Boolean
) : AbstractSymbolLightClassesTestBase(configurator) {
    override fun getRenderResult(ktFile: KtFile, ktFiles: List<KtFile>, testDataFile: Path, module: TestModule, project: Project): String {
        throw IllegalStateException("This test is not rendering light elements")
    }

    final override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val lightClasses = lightClassesToCheck(ktFiles, module, testServices)
        if (lightClasses.isEmpty()) return
        val project = lightClasses.first().project
        val modificationTracker = if (isTestAgainstCompiledCode) {
            project.createAllLibrariesModificationTracker()
        } else {
            project.createProjectWideOutOfBlockModificationTracker()
        } as SimpleModificationTracker

        val testVisitor = createTestVisitor(modificationTracker, testServices.assertions)
        for (lightClass in lightClasses) {
            lightClass.accept(testVisitor)
        }
    }

    private fun createTestVisitor(
        modificationTracker: SimpleModificationTracker,
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
            modificationTracker.incModificationCount()

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

    abstract fun lightClassesToCheck(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices): Collection<PsiClass>
}
