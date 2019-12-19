/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.FirDiagnosticsCollector
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFirDiagnosticsTest : AbstractFirBaseDiagnosticsTest() {
    override fun runAnalysis(testDataFile: File, testFiles: List<TestFile>, firFilesPerSession: Map<FirSession, List<FirFile>>) {
        for ((_, firFiles) in firFilesPerSession) {
            doFirResolveTestBench(
                firFiles,
                FirTotalResolveTransformer().transformers,
                gc = false
            )
        }
        val allFirFiles = firFilesPerSession.values.flatten()
        checkDiagnostics(testDataFile, testFiles, allFirFiles)
        checkFir(testDataFile, allFirFiles)
    }

    fun checkFir(testDataFile: File, firFiles: List<FirFile>) {
        val firFileDump = StringBuilder().apply { firFiles.forEach { it.accept(FirRenderer(this), null) } }.toString()
        val expectedPath = testDataFile.absolutePath.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(
            File(expectedPath),
            firFileDump
        )
    }

    protected fun checkDiagnostics(file: File, testFiles: List<TestFile>, firFiles: List<FirFile>) {
        val collector = createCollector()
        val actualText = StringBuilder()
        for (testFile in testFiles) {
            val firFile = firFiles.firstOrNull { it.psi == testFile.ktFile }
            if (firFile != null) {
                val coneDiagnostics = collector.collectDiagnostics(firFile)
                testFile.getActualText(coneDiagnostics, actualText)
            } else {
                actualText.append(testFile.expectedText)
            }
        }
        KotlinTestUtils.assertEqualsToFile(file, actualText.toString())
    }

    protected fun createCollector(): AbstractDiagnosticCollector {
        return FirDiagnosticsCollector.create()
    }
}