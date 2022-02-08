/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.fir

import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.utils.firTestDataFile
import java.io.File

class FirOldFrontendMetaConfigurator(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun transformTestDataPath(testDataFileName: String): String {
        val originalFile = File(testDataFileName)
        val isFirIdentical = originalFile.useLines { lines -> lines.any { it == "// ${FirDiagnosticsDirectives.FIR_IDENTICAL.name}" } }
        return if (isFirIdentical) {
            testDataFileName
        } else {
            val firFile = originalFile.firTestDataFile
            if (!firFile.exists()) {
                originalFile.copyTo(firFile)
            }
            firFile.absolutePath
        }
    }
}
