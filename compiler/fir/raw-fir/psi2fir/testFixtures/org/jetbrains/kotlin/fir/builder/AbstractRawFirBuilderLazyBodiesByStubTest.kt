/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.source.PsiFileImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractRawFirBuilderLazyBodiesByStubTest : AbstractRawFirBuilderLazyBodiesTestCase() {
    override fun doRawFirTest(filePath: String) {
        val ignoreTreeAccess = InTextDirectivesUtils.isDirectiveDefined(File(filePath).readText(), "// IGNORE_TREE_ACCESS:")
        var treeAccessFound = false
        try {
            super.doRawFirTest(filePath)
        } catch (e: Throwable) {
            /**
             * @see com.intellij.psi.impl.source.PsiFileImpl.reportProhibitedAstAccess
             */
            if (!ignoreTreeAccess || e.message?.startsWith("Access to tree elements not allowed for") != true) {
                throw e
            }

            treeAccessFound = true
        }

        assertEquals("The tree access is not detected. 'IGNORE_TREE_ACCESS' have to be dropped", ignoreTreeAccess, treeAccessFound)
    }

    override fun createKtFile(filePath: String): KtFile {
        val originalFile = super.createKtFile(filePath)
        return createKtFile(originalFile, testRootDisposable)
    }

    override val alternativeTestPrefix: String? get() = "stub"

    companion object {
        fun createKtFile(originalFile: KtFile, disposable: Disposable): KtFile {
            val project = originalFile.project
            val originalProvider = originalFile.viewProvider
            val updatedProvider = object : SingleRootFileViewProvider(
                originalProvider.manager,
                originalProvider.virtualFile,
                originalProvider.isEventSystemEnabled,
                originalProvider.fileType,
            ) {
                /**
                 * This flag is required to treat the file as physical, as we build stubs only for physical files.
                 * The problem is that the file is physical itself, but the original provider is not.
                 *
                 * @see com.intellij.psi.AbstractFileViewProvider
                 */
                override fun isPhysical(): Boolean = true
            }

            /**
             * Throw an exception on an attempt to load a file tree if the file is stub-based.
             *
             * @see com.intellij.psi.impl.source.PsiFileImpl.loadTreeElement
             */
            updatedProvider.manager.setAssertOnFileLoadingFilter(
                {
                    val psiFile = it.findPsiFile(project) as? PsiFileImpl
                    psiFile == null || psiFile.stub != null
                },
                disposable,
            )

            val fileWithStub = object : KtFile(updatedProvider, false) {
                private val fakeStub get() = stubTree?.root as? KotlinFileStub

                // We have to override this method as well as the base implementation will skip
                override fun getStub(): KotlinFileStub? = fakeStub
                override val greenStub: KotlinFileStub? get() = fakeStub
            }

            /**
             * We have to replace the previous file to get the right stub
             *
             * @see com.intellij.psi.impl.source.PsiFileImpl.getStubTree
             */
            updatedProvider.forceCachedPsi(fileWithStub)

            assertNotNull("Stub for the file must not be null", fileWithStub.stub)
            return fileWithStub
        }
    }
}
