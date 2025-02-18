/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.files.DECOMPILED_TEST_DATA_K2_SUFFIX
import org.jetbrains.kotlin.analysis.decompiler.stub.files.DECOMPILED_TEST_DATA_SUFFIX
import org.jetbrains.kotlin.analysis.decompiler.stub.files.extractAdditionalStubInfo
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name

abstract class AbstractAdditionalStubInfoKnmTest : AbstractDecompiledKnmFileTest() {
    override val knmTestSupport: KnmTestSupport
        get() = K2KnmTestSupport

    override fun doTest(testDirectoryPath: Path) {
        val stubBuilder = knmTestSupport.createDecompiler().stubBuilder
        val knmFiles = compileToKnmFiles(testDirectoryPath)
        val knmFile = knmFiles.singleOrNull { "root_package" !in it.path }
            ?: error("Expected a single non-root .knm file, but received:${System.lineSeparator()}" +
                             knmFiles.joinToString(separator = System.lineSeparator()) { it.path }
            )

        val stub = stubBuilder.buildFileStub(FileContentImpl.createByFile(knmFile, environment.project))!!
        KotlinTestUtils.assertEqualsToFile(
            getExpectedFile(testDirectoryPath),
            extractAdditionalStubInfo(stub)
        )
    }

    private fun getExpectedFile(testDirectoryPath: Path): File {
        return testDirectoryPath.resolve("${testDirectoryPath.name}$DECOMPILED_TEST_DATA_K2_SUFFIX").toFile().takeIf { it.exists() }
            ?: testDirectoryPath.resolve("${testDirectoryPath.name}$DECOMPILED_TEST_DATA_SUFFIX").toFile().takeIf { it.exists() }
            ?: error("Test data file doesn't exist in: ${testDirectoryPath.absolutePathString()}")
    }
}
