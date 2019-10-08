/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFirDiagnosticsTest : AbstractFirDiagnosticsSmokeTest() {

    override fun getConfigurationKind(): ConfigurationKind {
        return ConfigurationKind.ALL
    }

    override fun checkResultingFirFiles(
        firFiles: MutableList<FirFile>,
        testDataFile: File
    ) {
        val firFileDump = StringBuilder().also { stringBuilder ->
            firFiles.map {
                it.accept(FirRenderer(stringBuilder), null)
            }
        }.toString()
        val expectedPath = testDataFile.path.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(File(expectedPath), firFileDump)
    }
}
