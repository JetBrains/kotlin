/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.files

import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinClsStubBuilder
import java.nio.file.Paths

/**
 * Checks that the element type of the stub's PSI ([com.intellij.psi.stubs.StubElement.getPsi]) is equal to the element type of the stub.
 */
abstract class AbstractPsiStubElementTypeConsistencyTest : AbstractDecompiledClassTest() {
    fun runTest(testDirectory: String) {
        val testDirectoryPath = Paths.get(testDirectory)
        val testData = TestData.createFromDirectory(testDirectoryPath)
        testData.withFirIgnoreDirective(useK2ToCompileCode) {
            doTest(testData, useStringTable = true)
            doTest(testData, useStringTable = false)
        }
    }

    private fun doTest(testData: TestData, useStringTable: Boolean) {
        val classFile = getClassFileToDecompile(testData, useStringTable)
        val fileStub = ClsClassFinder.allowMultifileClassPart {
            KotlinClsStubBuilder().buildFileStub(FileContentImpl.createByFile(classFile))
        } ?: error("Couldn't build a file stub for the file: $classFile")

        checkPsiElementTypeConsistency(fileStub)
    }

    private fun checkPsiElementTypeConsistency(stubElement: StubElement<*>) {
        val psi = stubElement.psi as? StubBasedPsiElement<*>
        if (psi != null) {
            assertEquals(
                "Expected the PSI of `$stubElement` to have the same element type. Instead got: `${psi.elementType}`.",
                stubElement.stubType,
                psi.elementType,
            )
        }

        stubElement.childrenStubs.forEach(::checkPsiElementTypeConsistency)
    }
}
