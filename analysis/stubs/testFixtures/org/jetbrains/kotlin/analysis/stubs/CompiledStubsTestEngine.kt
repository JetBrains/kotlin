/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.stubs.StubTreeLoader
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.analysis.stubs.CompiledStubsTestEngine.compute
import org.jetbrains.kotlin.analysis.stubs.CompiledStubsTestEngine.validate
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.suppressIf
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

object CompiledStubsTestEngine : StubsTestEngine() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    private object Directives : SimpleDirectivesContainer() {
        /** @see validate */
        val INCONSISTENT_TREE by stringDirective(
            "Temporary disables '${CompiledStubsTestEngine::validate.name}' until the issue is fixed. YT ticket must be provided"
        )
    }

    override fun compute(file: KtFile): KotlinFileStubImpl = ClsClassFinder.allowMultifileClassPart {
        requireIsInstance<KtDecompiledFile>(file)

        // The tree loader is called to build a stub tree for a binary file directly
        val fileStub = StubTreeLoader.getInstance()
            .build(/* project = */ null, /* vFile = */ file.virtualFile, /* psiFile = */ null)
            ?.root

        requireNotNull(fileStub) { "A stub tree is expected to be present for all decompiled files since they are built from it" }

        requireIsInstance<KotlinFileStubImpl>(fileStub)

        fileStub
    }

    /**
     * The purpose of this function to validate the consistency between stub and AST trees for [file] which has to be a [decompiled file][KtDecompiledFile].
     *
     * Via [PsiFileImpl.calcStubTree][com.intellij.psi.impl.source.PsiFileImpl.calcStubTree] it performs:
     * 1. The AST-tree computation from the decompiled text
     * 2. The stub-tree computation from the binary data (effectively the same as [compute])
     * 3. The binding between the AST and stub trees (via [com.intellij.psi.impl.source.FileTrees.reconcilePsi])
     *
     * @see Directives.INCONSISTENT_TREE
     */
    override fun validate(testServices: TestServices, file: KtFile, fileStub: KotlinFileStubImpl) {
        super.validate(testServices, file, fileStub)

        requireIsInstance<KtDecompiledFile>(file)

        testServices.moduleStructure.allDirectives.suppressIf(
            suppressionDirective = Directives.INCONSISTENT_TREE,
            filter = { true },
        ) {
            // This computation might throw an exception in the case of inconsistency between the AST and stub trees
            val decompiledStub = file.calcStubTree().root
            requireIsInstance<KotlinFileStubImpl>(decompiledStub)

            testServices.assertions.assertEquals(
                expected = render(fileStub),
                actual = render(decompiledStub)
            ) {
                "The stub tree computed from the decompiled text must be the same as the stub tree computed from the binary data"
            }
        }
    }
}
