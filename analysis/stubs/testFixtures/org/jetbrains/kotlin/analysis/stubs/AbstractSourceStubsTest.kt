/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.services.assertions
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
            val stubBasedFqNames = collectStubBasedFqNames(file)
            file.calcTreeElement()
            assertClassLocalityMatchesAst(stubBasedLocality, testServices)
            assertFqNamesMatchAst(file, stubBasedFqNames, testServices)

            recomputeAstSpine(file)
        }
    }

    /**
     * Computes [KtNamedDeclaration.getFqName] for every named declaration stub of the stub-based [file] without loading its AST.
     */
    private fun collectStubBasedFqNames(file: KtFile): Map<KtNamedDeclaration, FqName?> = withAstLoadingAssertion(file) {
        buildMap {
            fun visit(stub: StubElement<*>) {
                (stub.psi as? KtNamedDeclaration)?.let { put(it, it.fqName) }
                stub.childrenStubs.forEach(::visit)
            }

            visit(file.stub ?: error("Stub should be present for unloaded file"))
        }
    }

    /**
     * Checks [stubBasedFqNames] against the FqNames of the declarations of a copy of [file] that has only an AST.
     * The loaded AST of [file] itself is not enough, as its PSI keeps reading the green stub.
     */
    private fun assertFqNamesMatchAst(
        file: KtFile,
        stubBasedFqNames: Map<KtNamedDeclaration, FqName?>,
        testServices: TestServices,
    ) {
        val astFile = KtPsiFactory(file.project).createFile(file.name, file.text)
        val astFqNames = astFile.collectDescendantsOfType<KtNamedDeclaration>().associate { declaration ->
            Pair(declaration.textRange.startOffset, declaration::class) to declaration.fqName
        }

        for ([declaration, stubBasedFqName] in stubBasedFqNames) {
            val astFqName = astFqNames.getValue(Pair(declaration.textRange.startOffset, declaration::class))
            testServices.assertions.assertEquals(astFqName, stubBasedFqName) {
                "Stub-based and AST-based FqNames differ for '${declaration.name}'"
            }
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
