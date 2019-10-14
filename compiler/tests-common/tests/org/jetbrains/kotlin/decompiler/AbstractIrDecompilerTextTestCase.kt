/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.decompiler

import junit.framework.TestCase
import org.jetbrains.kotlin.codegen.CodegenTestFiles
import org.jetbrains.kotlin.codegen.ir.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.ir.AbstractIrGeneratorTestCase
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractIrDecompilerTextTestCase : AbstractIrGeneratorTestCase() {
    override fun doTest(wholeFile: File, testFiles: List<TestFile>) {
        val ignoreErrors = shouldIgnoreErrors(wholeFile)
        val originalIrModule = generateIrModule(ignoreErrors)
        val originalIrString = originalIrModule.dump()
        val decompiledSources = originalIrModule.decompile()
        val decompiledFile = KotlinTestUtils.createFile(wholeFile.name, decompiledSources, myEnvironment.project)
        myFiles = CodegenTestFiles.create(listOf(decompiledFile))
        val decompiledIrModule = generateIrModule(ignoreErrors)
        val decompiledIrString = decompiledIrModule.dump()
        TestCase.assertEquals(originalIrString, decompiledIrString)
        val decompiledFileSources = File(wholeFile.absolutePath.replace(".kt", ".kt.decompiled"))
        KotlinTestUtils.assertEqualsToFile(decompiledFileSources, decompiledSources)
    }
}

abstract class AbstractIrDecompilerBlackBoxTest : AbstractIrDecompilerTextTestCase() {
    private val blackBoxTestRunner = object : AbstractIrBlackBoxCodegenTest() {
        fun runTest(filePath: String) = doTest(filePath)
    }

    override fun doTest(wholeFile: File, testFiles: List<TestFile>) {
        val ignoreErrors = shouldIgnoreErrors(wholeFile)
        val originalIrModule = generateIrModule(ignoreErrors)
        val decompiledSources = originalIrModule.decompile()
        val decompiledFileSources = File(wholeFile.absolutePath.replace(".kt", ".decompiled.kt"))
        KotlinTestUtils.assertEqualsToFile(decompiledFileSources, decompiledSources)

        blackBoxTestRunner.runTest(decompiledFileSources.absolutePath)
//        decompiledFileSources.copyTo(File(decompiledFileSources.absolutePath.replace(".decompiled.kt", ".kt.decompiled")), true)
        assert(decompiledFileSources.delete())
    }
}