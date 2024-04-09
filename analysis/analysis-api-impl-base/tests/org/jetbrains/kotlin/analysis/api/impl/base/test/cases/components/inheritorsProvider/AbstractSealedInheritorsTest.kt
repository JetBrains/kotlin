/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.inheritorsProvider

import org.jetbrains.kotlin.analysis.api.impl.base.test.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KtDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KtTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KtUsualClassTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSealedInheritorsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        doTestByKtFile(mainFile, testServices)
    }

    /**
     * [ktFile] may be a fake file for dangling module tests.
     */
    protected fun doTestByKtFile(ktFile: KtFile, testServices: TestServices) {
        analyseForTest(ktFile) {
            val classSymbol = getSingleTestTargetSymbolOfType<KtNamedClassOrObjectSymbol>(ktFile, testDataPath)

            val actualText = classSymbol.getSealedClassInheritors().joinToString("\n\n") { inheritor ->
                // Render sealed inheritor supertypes as fully expanded types to avoid discrepancies between Standalone and IDE mode.
                //
                // Assume the following code is compiled to a library:
                //
                // ```
                // open class C
                // typealias T = C
                // class A : T()
                // ```
                //
                // The supertype of `A` will be different in Standalone and IDE mode:
                //
                //  - Standalone: The compiled library contains fully expanded types, so `A`'s supertype is `C`.
                //  - IDE: Symbols from libraries are deserialized from stubs, where type aliases are currently not fully expanded, so
                //    `A`'s supertype is `T`.
                //
                // We want to render `class A : C()` in both cases, so we need to expand the type alias.
                val declarationRenderer = KtDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
                    typeRenderer = KtTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
                        usualClassTypeRenderer = KtUsualClassTypeRenderer.AS_FULLY_EXPANDED_CLASS_TYPE_WITH_TYPE_ARGUMENTS
                    }
                }

                "${inheritor.classIdIfNonLocal!!}\n${inheritor.render(declarationRenderer)}"
            }

            testServices.assertions.assertEqualsToTestDataFileSibling(actualText)
        }
    }
}
