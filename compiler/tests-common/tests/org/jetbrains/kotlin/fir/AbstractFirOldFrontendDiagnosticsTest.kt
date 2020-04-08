/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFirOldFrontendDiagnosticsTest : AbstractFirDiagnosticsTest() {
    override fun createTestFileFromPath(filePath: String): File {
        val newPath = if (File(filePath).readText().contains("// FIR_IDENTICAL")) filePath else filePath.replace(".kt", ".fir.kt")
        return File(newPath).also {
            prepareTestDataFile(filePath, it)
        }
    }

    private fun prepareTestDataFile(originalFilePath: String, firTestDataFile: File) {
        if (!firTestDataFile.exists()) {
            KotlinTestUtils.assertEqualsToFile(firTestDataFile, loadTestDataWithoutDiagnostics(File(originalFilePath)))
        }
    }

    override fun runAnalysis(testDataFile: File, testFiles: List<TestFile>, firFilesPerSession: Map<FirSession, List<FirFile>>) {
        if (testFiles.any { "FIR_IGNORE" in it.directives }) return
        val failure: FirRuntimeException? = try {
            for ((_, firFiles) in firFilesPerSession) {
                doFirResolveTestBench(firFiles, FirTotalResolveTransformer().transformers, gc = false)
            }
            null
        } catch (e: FirRuntimeException) {
            e
        }
        val failureFile = File(testDataFile.path.replace(".kt", ".fail"))
        if (failure == null) {
            val allFirFiles = firFilesPerSession.values.flatten()
            checkResultingFirFiles(allFirFiles, testDataFile)
            assertFalse("Test is good but there is expected exception", failureFile.exists())
            checkDiagnostics(testDataFile, testFiles, allFirFiles)
            if (testDataFile.absolutePath.endsWith(".fir.kt")) {
                val oldFrontendTestDataFile = File(testDataFile.absolutePath.replace(".fir.kt", ".kt"))
                compareAndMergeFirFileAndOldFrontendFile(oldFrontendTestDataFile, testDataFile)
            }

            val needDump = testFiles.any { "FIR_DUMP" in it.directives }
            if (needDump) {
                checkFir(testDataFile, allFirFiles)
            }
        } else {
            if (!failureFile.exists()) {
                throw failure
            }
            checkFailureFile(failure, failureFile)
        }
    }

    private fun checkFailureFile(failure: FirRuntimeException, failureFile: File) {
        val failureMessage = buildString {
            appendln(failure.message)
            append("Cause: ")
            appendln(failure.cause)
        }
        KotlinTestUtils.assertEqualsToFile(failureFile, failureMessage)
    }

    protected open fun checkResultingFirFiles(firFiles: List<FirFile>, testDataFile: File) {}
}

