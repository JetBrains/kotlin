/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.impl.source.tree.FileElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.testFederation.MustRunAlways

/**
 * This test is supposed to validate the source stubs output
 */
@MustRunAlways
abstract class AbstractSourceStubsTest : AbstractStubsTest() {
    override val outputFileExtension: String get() = "stubs.txt"
    override val configurator: AnalysisApiTestConfigurator = LLSourceLikeTestConfigurator()
    override val stubsTestEngine: StubsTestEngine get() = SourceStubsTestEngine

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        super.doTestByMainModuleAndOptionalMainFile(mainFile, mainModule, testServices)

        for (file in mainModule.ktFiles) {
            forceStubTree(file, testServices)
            val stubBasedLocality = collectStubBasedClassLocality(file)
            file.calcTreeElement()
            assertClassLocalityMatchesAst(stubBasedLocality, testServices)

            recomputeAstSpine(file)
        }
    }

    /**
     * Recomputes the stubbed spine of an AST without a stub tree while the spine PSI already reads its AST through the spine,
     * as in the IDE once the stub tree is garbage collected.
     * Stub element factories must not touch the PSI of the checked node, otherwise the spine computation recurses into itself.
     */
    private fun recomputeAstSpine(file: KtFile) {
        val astFile = KtPsiFactory(file.project).createFile(file.name, file.text)

        // Switches the spine PSI to the spine-based access to its AST
        val spine = astFile.stubbedSpine
        (astFile.node as FileElement).clearCaches()
        for (index in 0..<spine.stubCount) {
            spine.getStubPsi(index)?.node
        }
    }
}
