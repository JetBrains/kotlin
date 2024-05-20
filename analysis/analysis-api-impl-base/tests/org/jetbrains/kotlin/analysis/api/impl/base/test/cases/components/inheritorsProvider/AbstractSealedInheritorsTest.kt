/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.inheritorsProvider

import org.jetbrains.kotlin.analysis.api.impl.base.test.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.KaExpandedTypeRenderingMode
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
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
            val classSymbol = getSingleTestTargetSymbolOfType<KaNamedClassOrObjectSymbol>(ktFile, testDataPath)

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
                val declarationRenderer = KaDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
                    typeRenderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
                        expandedTypeRenderingMode = KaExpandedTypeRenderingMode.RENDER_EXPANDED_TYPE
                    }
                }

                "${inheritor.classIdIfNonLocal!!}\n${inheritor.render(declarationRenderer)}"
            }

            testServices.assertions.assertEqualsToTestDataFileSibling(actualText)
        }
    }
}
