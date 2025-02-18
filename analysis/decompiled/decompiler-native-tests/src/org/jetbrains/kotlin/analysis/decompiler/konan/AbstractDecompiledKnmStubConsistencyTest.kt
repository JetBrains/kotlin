/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.files.serializeToString
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
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

    private fun checkKnmStubConsistency(knmFile: VirtualFile) {
        val decompiler = knmTestSupport.createDecompiler()
        val stubTreeBinaryFile = decompiler.stubBuilder.buildFileStub(FileContentImpl.createByFile(knmFile, environment.project))!!

        val fileViewProviderForDecompiledFile = decompiler.createFileViewProvider(
            knmFile, PsiManager.getInstance(project), physical = false,
        )

        val stubTreeForDecompiledFile = KtFileStubBuilder().buildStubTree(
            KlibDecompiledFile(fileViewProviderForDecompiledFile) { virtualFile ->
                decompiler.buildDecompiledTextForTests(virtualFile)
            }
        )

        Assert.assertEquals(
            "PSI and deserialized stubs don't match",
            stubTreeForDecompiledFile.serializeToString(),
            stubTreeBinaryFile.serializeToString()
        )
    }
}
