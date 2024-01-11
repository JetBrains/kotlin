/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.SingleRootFileViewProvider
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
         * Throw exception on an attempt to load a file tree
         *
         * @see com.intellij.psi.impl.source.PsiFileImpl.loadTreeElement
         */
        updatedProvider.manager.setAssertOnFileLoadingFilter(VirtualFileFilter.ALL, testRootDisposable)

        val fileWithStub = object : KtFile(updatedProvider, false) {
            // We have to override this method as well as the base implementation will skip
            override fun getStub(): KotlinFileStub? = stubTree?.root as? KotlinFileStub
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

    override val alternativeTestPrefix: String? get() = "stub"
}
