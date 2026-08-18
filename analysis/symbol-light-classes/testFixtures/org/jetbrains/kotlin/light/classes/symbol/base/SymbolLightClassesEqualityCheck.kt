/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiRecordHeader
import org.jetbrains.kotlin.analysis.api.platform.modification.publishGlobalModuleStateModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.publishGlobalSourceOutOfBlockModificationEvent
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.testFramework.runWriteAction

/**
 * Checks that light elements survive cache invalidation: repeated accessor calls have to return new but `equals` instances.
 *
 * The check invalidates the global module or source state many times, so it has to be the last one performed on [lightClasses]:
 * everything computed before it may be dropped.
 */
internal fun checkLightClassesEquality(
    lightClasses: Collection<PsiClass>,
    isTestAgainstCompiledCode: Boolean,
    assertions: AssertionsService,
) {
    if (lightClasses.isEmpty()) return

    val testVisitor = createTestVisitor(lightClasses.first().project, isTestAgainstCompiledCode, assertions)
    for (lightClass in lightClasses) {
        lightClass.accept(testVisitor)
    }
}

private fun invalidateCaches(project: Project, isTestAgainstCompiledCode: Boolean) {
    runWriteAction {
        if (isTestAgainstCompiledCode) {
            project.publishGlobalModuleStateModificationEvent()
        } else {
            project.publishGlobalSourceOutOfBlockModificationEvent()
        }
    }
}

private fun createTestVisitor(
    project: Project,
    isTestAgainstCompiledCode: Boolean,
    assertions: AssertionsService,
): PsiElementVisitor = object : JavaElementVisitor() {
    override fun visitClass(aClass: PsiClass) {
        compareElementsWithInvalidation(aClass, PsiClass::getRecordHeader)
        compareArrayElementsWithInvalidation(aClass, PsiClass::getRecordComponents)
        compareArrayElementsWithInvalidation(aClass, PsiClass::getMethods)
        compareArrayElementsWithInvalidation(aClass, PsiClass::getFields)
        compareArrayElementsWithInvalidation(aClass, PsiClass::getInnerClasses)

        super.visitClass(aClass)
    }

    override fun visitRecordHeader(recordHeader: PsiRecordHeader) {
        compareArrayElementsWithInvalidation(recordHeader, PsiRecordHeader::getRecordComponents)

        super.visitRecordHeader(recordHeader)
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
        invalidateCaches(project, isTestAgainstCompiledCode)

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

            for ([index, expected] in before.withIndex()) {
                val actual = after[index]
                assertions.assertEquals(expected, actual) {
                    "Element: $element"
                }
            }
        }
    }
}
