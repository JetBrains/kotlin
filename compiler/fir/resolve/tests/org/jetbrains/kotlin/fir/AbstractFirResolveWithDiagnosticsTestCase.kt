/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.ParallelDiagnosticsCollector
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.components.DeclarationCheckersDiagnosticComponent
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFirResolveWithDiagnosticsTestCase : AbstractFirResolveTestCase() {
    override fun doTest(path: String) {
        val firFiles = processInputFile(path)
        checkFir(path, firFiles)
        checkDiagnostics(path, firFiles)
    }

    private fun checkDiagnostics(path: String, firFiles: List<FirFile>) {
//        val collector = SimpleDiagnosticsCollector()
        val collector = ParallelDiagnosticsCollector(4)
        collector.initializeComponents(DeclarationCheckersDiagnosticComponent(collector))
        val diagnostics = mutableListOf<ConeDiagnostic>()
        for (file in firFiles) {
            diagnostics += collector.collectDiagnostics(file)
        }
        val expectedPath = path.replace(".kt", ".diagnostics.txt")
        val expectedFile = File(expectedPath)
        if (diagnostics.isEmpty()) {
            assertFalse("There is no diagnostics but expected file exists", expectedFile.exists())
            return
        }

        val actual = diagnostics.joinToString("\n\n") {
            val text = (it.source as FirPsiSourceElement).psi.text
            "<${it.factory.name}>: $text"
        }
        KotlinTestUtils.assertEqualsToFile(expectedFile, actual)
    }
}