/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * The test suite checks the raw FIR built in the [BodyBuildingMode.LAZY_BODIES] mode.
 *
 * The AST-based dump is the golden one, and the stub-based dump is stored in a `stub`-prefixed file
 * only if it differs from the golden one.
 *
 * Files with [Directives.IGNORE_TREE_ACCESS] cannot be built from stubs at all,
 * so only the AST-based dump is checked for them.
 */
abstract class AbstractLLRawFirBuilderLazyBodiesTest : AbstractLLStubBasedTest<String>() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        if (Directives.IGNORE_TREE_ACCESS in testServices.moduleStructure.allDirectives) {
            // The file cannot be built from stubs, so only the AST-based dump is checked
            mainFile.calcTreeElement()
            withResolutionFacade(mainFile) { facade ->
                assertGoldenDump(mainFile, testServices, facade = facade)
            }

            return
        }

        super.doTestByMainFile(mainFile, mainModule, testServices)
    }

    context(facade: LLResolutionFacade)
    override fun doStubBasedTest(stubBasedFile: KtFile, mainModule: KtTestModule, testServices: TestServices): String {
        return dumpLazyBodies(stubBasedFile)
    }

    context(facade: LLResolutionFacade)
    override fun doAstBasedValidation(
        stubBasedOutput: String,
        astBasedFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    ) {
        // The golden file has to be checked first, so the stub-based one can be recognized as redundant
        assertGoldenDump(astBasedFile, testServices)
        testServices.assertions.assertEqualsToTestOutputFile(
            stubBasedOutput,
            extension = LAZY_BODIES_EXTENSION,
            testPrefixes = variantChain + STUB_VARIANT,
        )
    }

    context(facade: LLResolutionFacade)
    private fun assertGoldenDump(astBasedFile: KtFile, testServices: TestServices) {
        testServices.assertions.assertEqualsToTestOutputFile(dumpLazyBodies(astBasedFile), extension = LAZY_BODIES_EXTENSION)
    }
}

private const val LAZY_BODIES_EXTENSION = ".lazyBodies.txt"
private const val STUB_VARIANT = "stub"

context(facade: LLResolutionFacade)
private fun dumpLazyBodies(file: KtFile): String {
    val session = facade.useSiteFirSession
    val firFile = PsiRawFirBuilder(
        session,
        session.kotlinScopeProvider,
        bodyBuildingMode = BodyBuildingMode.LAZY_BODIES,
    ).buildFirFile(file)

    return FirRenderer.withDeclarationAttributes().renderElementAsString(firFile)
}

abstract class AbstractLLSourceLikeRawFirBuilderLazyBodiesTest : AbstractLLRawFirBuilderLazyBodiesTest() {
    override val configurator = LLSourceLikeTestConfigurator()
}
