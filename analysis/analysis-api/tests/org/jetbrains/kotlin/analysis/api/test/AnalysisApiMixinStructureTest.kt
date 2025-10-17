/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.test

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.components.KaSessionComponentImplementationDetail
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestDataAssertions
import org.junit.jupiter.api.Test
import java.io.File


/**
 * The test checks that all [KaSessionComponent][org.jetbrains.kotlin.analysis.api.components.KaSessionComponent] implementations
 * are properly marked to not leak them as types to user code
 */
class AnalysisApiMixinStructureTest : AbstractAnalysisApiSurfaceCodebaseValidationTest() {
    @Test
    fun testMixnStructure() = doTest()

    override fun processFile(file: File, psiFile: PsiFile) {
        if (psiFile !is KtFile) return

        val sessionComponent = psiFile.findSessionComponent()?.takeUnless {
            it.name == KA_SESSION_CLASS
        } ?: return

        assertNoCompanion(file, sessionComponent)

        // OptIn annotation itself
        assertSpecialAnnotation(file, sessionComponent, KA_SESSION_COMPONENT_IMPLEMENTATION_DETAIL_ANNOTATION)

        // OptIn annotation for subclasses
        assertSpecialAnnotation(file, sessionComponent, KA_SESSION_COMPONENT_IMPLEMENTATION_DETAIL_SUBCLASS_ANNOTATION)
    }

    private fun assertNoCompanion(file: File, sessionComponent: KtClassOrObject) {
        if (sessionComponent.companionObjects.isNotEmpty()) {
            error("Session component '${sessionComponent.name}' (${file}) should not have a companion object since it exposes the session component type")
        }
    }

    private fun assertSpecialAnnotation(file: File, sessionComponent: KtClassOrObject, annotationText: String) {
        if (sessionComponent.annotationEntries.any { it.textMatches(annotationText) }) {
            return
        }

        val actualText = fileTextWithNewAnnotation(sessionComponent, annotationText)
        TestDataAssertions.assertEqualsToFile(
            /* message = */
            "The session component has to be marked by '$annotationText' in order to not guarantee its compatibility",
            /* expectedFile = */ file,
            /* actual = */ actualText,
        )
    }

    private companion object {
        val KA_SESSION_COMPONENT_IMPLEMENTATION_DETAIL_ANNOTATION: String = "@${KaSessionComponentImplementationDetail::class.simpleName}"
        val KA_SESSION_COMPONENT_IMPLEMENTATION_DETAIL_SUBCLASS_ANNOTATION: String =
            "@${SubclassOptInRequired::class.simpleName}(${KaSessionComponentImplementationDetail::class.simpleName}::class)"
    }
}
