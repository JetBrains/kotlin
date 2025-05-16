/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.files

import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinClsStubBuilder
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.nio.file.Paths

abstract class AbstractAdditionalStubInfoTest : AbstractDecompiledClassTest() {
    fun runTest(testDirectory: String) {
        val testDirectoryPath = Paths.get(testDirectory)
        val testData = TestData.createFromDirectory(testDirectoryPath)
        testData.withFirIgnoreDirective(useK2ToCompileCode) {
            val stub = KotlinClsStubBuilder().buildFileStub(FileContentImpl.createByFile(getClassFileToDecompile(testData, false)))!!
            KotlinTestUtils.assertEqualsToFile(testData.getExpectedFile(useK2ToCompileCode), extractAdditionalStubInfo(stub))
            testData.checkIfIdentical(useK2ToCompileCode)
        }
    }
}
