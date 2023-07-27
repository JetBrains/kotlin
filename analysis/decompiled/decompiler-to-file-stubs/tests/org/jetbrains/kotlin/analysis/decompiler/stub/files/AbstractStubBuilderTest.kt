/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.files

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.stubs.impl.STUB_TO_STRING_PREFIX
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.nio.file.Paths

abstract class AbstractStubBuilderTest : AbstractDecompiledClassTest() {
    fun runTest(testDirectory: String) {
        val testDirectoryPath = Paths.get(testDirectory)
        val testData = TestData.createFromDirectory(testDirectoryPath)
        testData.withFirIgnoreDirective {
            doTest(testData, useStringTable = true)
            doTest(testData, useStringTable = false)
        }
    }


    private fun doTest(testData: TestData, useStringTable: Boolean) {
        val classFile = getClassFileToDecompile(testData, useStringTable)
        testClsStubsForFile(classFile, testData)
    }

    private fun testClsStubsForFile(classFile: VirtualFile, testData: TestData) {
        val stub = getStubToTest(classFile)
        KotlinTestUtils.assertEqualsToFile(testData.getExpectedFile(useK2ToCompileCode), stub.serializeToString())
        testData.checkIfIdentical(useK2ToCompileCode)
    }

    protected abstract fun getStubToTest(classFile: VirtualFile): PsiFileStub<*>
}

private fun StubElement<out PsiElement>.serializeToString(): String {
    return serializeStubToString(this)
}

private fun serializeStubToString(stubElement: StubElement<*>): String {
    val treeStr = DebugUtil.stubTreeToString(stubElement).replace(SpecialNames.SAFE_IDENTIFIER_FOR_NO_NAME.asString(), "<no name>")

    // Nodes are stored in form "NodeType:Node" and have too many repeating information for Kotlin stubs
    // Remove all repeating information (See KotlinStubBaseImpl.toString())
    return treeStr.lines().joinToString(separator = "\n") {
        if (it.contains(STUB_TO_STRING_PREFIX)) {
            it.takeWhile(Char::isWhitespace) + it.substringAfter("KotlinStub$")
        } else {
            it
        }
    }.replace(", [", "[")
}

