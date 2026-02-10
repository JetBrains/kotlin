/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.substitutorProvider

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

/**
 * Tests for [org.jetbrains.kotlin.analysis.api.components.KaSubstitutorProvider.createSubtypingSubstitutor].
 *
 * Test data format:
 * - Mark the subclass with `<caret_subclass>` marker
 * - Mark the supertype type reference with `<caret_supertype>` marker
 *
 * Example:
 * ```
 * interface <caret_subclass>A<T> : B<T>
 * interface B<T> : C<Int, T>
 * interface C<X, Y>
 *
 * val x: <caret_supertype>C<Int, String>
 * ```
 */
@OptIn(KaExperimentalApi::class)
abstract class AbstractCreateSubtypingSubstitutorTest : AbstractAnalysisApiBasedTest() {
    override fun doTest(testServices: TestServices) {
        val subClass = testServices.expressionMarkerProvider
            .getBottommostElementsOfTypeAtCarets<KtClassOrObject>(testServices, "subclass")
            .single().first

        val superTypeReference = testServices.expressionMarkerProvider
            .getBottommostElementsOfTypeAtCarets<KtTypeReference>(testServices, "supertype")
            .single().first

        val result = copyAwareAnalyzeForTest(
            contextElement = subClass.containingKtFile,
            danglingFileResolutionMode = KaDanglingFileResolutionMode.PREFER_SELF,
        ) { contextFile ->
            val contextSubClass = getDependentElementFromFile(originalElement = subClass, contextFile)
            val contextSuperTypeReference = getDependentElementFromFile(originalElement = superTypeReference, contextFile)

            val subClassSymbol = contextSubClass.classSymbol!!
            val superType = contextSuperTypeReference.type as KaClassType
            val substitutor = createSubtypingSubstitutor(subClassSymbol, superType)

            prettyPrint {
                appendLine("Subclass: ${subClassSymbol.name}")
                appendLine("Supertype: ${superType.render(typeRenderer, position = Variance.INVARIANT)}")
                appendLine("Substitutor: ${stringRepresentation(substitutor)}")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(result)
    }

    companion object {
        private val typeRenderer = KaTypeRendererForSource.WITH_SHORT_NAMES
    }
}
