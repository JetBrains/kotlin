/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.compiled.ClassFileDecompiler
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.BinaryFileStubBuilders
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.files.serializeToString
import org.junit.Assert
import java.nio.file.Path

abstract class AbstractDecompiledKnmStubConsistencyFe10Test : AbstractDecompiledKnmStubConsistencyTest() {
    override val knmTestSupport: KnmTestSupport
        get() = Fe10KnmTestSupport
}

abstract class AbstractDecompiledKnmStubConsistencyK2Test : AbstractDecompiledKnmStubConsistencyTest() {
    override val knmTestSupport: KnmTestSupport
        get() = K2KnmTestSupport
}

abstract class AbstractDecompiledKnmStubConsistencyTest : AbstractDecompiledKnmFileTest() {

    override fun doTest(testDirectoryPath: Path) {
        val files = compileToKnmFiles(testDirectoryPath)

        for (knmFile in files) {
            checkKnmStubConsistency(knmFile)
        }
    }

    override fun setUp() {
        super.setUp()
        BinaryFileTypeDecompilers.getInstance().addExplicitExtension(KlibMetaFileType, ClassFileDecompiler(), testRootDisposable)
        BinaryFileStubBuilders.INSTANCE.addExplicitExtension(KlibMetaFileType, ClassFileStubBuilder(), testRootDisposable)
        ClassFileDecompilers.getInstance().EP_NAME.point.registerExtension(
            knmTestSupport.createDecompiler(),
            LoadingOrder.FIRST,
            testRootDisposable
        )
    }

    private fun checkKnmStubConsistency(knmFile: VirtualFile) {
        val decompiler = knmTestSupport.createDecompiler()
        val stubTreeBinaryFile = decompiler.stubBuilder.buildFileStub(FileContentImpl.createByFile(knmFile, environment.project))!!
        val expectedText = stubTreeBinaryFile.serializeToString()

        val fileViewProviderForDecompiledFile = decompiler.createFileViewProvider(
            knmFile, PsiManager.getInstance(project), physical = false,
        )

        val stubTreeForDecompiledFile = KlibDecompiledFile(fileViewProviderForDecompiledFile) { virtualFile ->
            decompiler.buildDecompiledTextForTests(virtualFile)
        }.calcStubTree().root

        Assert.assertEquals(
            "PSI and deserialized stubs don't match",
            expectedText,
            stubTreeForDecompiledFile.serializeToString(),
        )
    }
}
