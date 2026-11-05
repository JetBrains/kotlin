/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.components.symbolDeclarationOverridesProvider

import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.scopes.delegatedMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.classSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Renders the overridden symbols of each callable in the delegated member scope of the class at the caret.
 */
abstract class AbstractOverriddenDeclarationProviderByDelegatedMemberScopeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = executeOnPooledThreadInReadAction {
            // Since analyzing overrides requires checking multiple declarations, we should use `PREFER_SELF` for copy-aware analysis.
            // Otherwise, supertypes might not be properly resolved.
            copyAwareAnalyzeForTest(
                mainFile,
                danglingFileResolutionMode = KaDanglingFileResolutionMode.PREFER_SELF,
            ) { contextFile ->
                val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtClassOrObject>(contextFile)
                val classSymbol = checkNotNull(declaration.classSymbol)

                classSymbol.delegatedMemberScope.callables.joinToString(separator = "\n\n") { callableSymbol ->
                    renderSignature(callableSymbol) + "\n" + renderOverriddenSymbols(callableSymbol).trimEnd().prependIndent("  ")
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
