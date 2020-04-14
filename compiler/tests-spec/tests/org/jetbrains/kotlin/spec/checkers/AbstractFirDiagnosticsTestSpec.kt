/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.checkers

import org.jetbrains.kotlin.fir.AbstractFirOldFrontendDiagnosticsTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.spec.utils.models.AbstractSpecTest
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser
import org.jetbrains.kotlin.test.ConfigurationKind
import java.io.File

abstract class AbstractFirDiagnosticsTestSpec : AbstractFirOldFrontendDiagnosticsTest() {
    private lateinit var specTest: AbstractSpecTest

    override fun extractConfigurationKind(files: List<TestFile>): ConfigurationKind {
        return ConfigurationKind.ALL
    }

    override fun getKtFiles(testFiles: List<TestFile>, includeExtras: Boolean): List<KtFile> {
        val ktFiles = super.getKtFiles(testFiles, includeExtras) as MutableList

        ktFiles.addAll(AbstractDiagnosticsTestSpec.additionalKtFiles(specTest, project))

        return ktFiles
    }

    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        val testFilePath = testDataFile.canonicalPath

        specTest = CommonParser.parseSpecTest(testFilePath, files.associate { Pair(it.fileName, it.clearText) }).first

        super.analyzeAndCheck(testDataFile, files)
    }
}
