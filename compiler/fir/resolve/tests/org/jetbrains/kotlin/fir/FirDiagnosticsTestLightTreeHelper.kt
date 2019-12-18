/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest
import java.io.File

interface FirDiagnosticsTestLightTreeHelper {
    fun analyzeAndCheck(testDataFile: File, files: List<BaseDiagnosticsTest.TestFile>) {
        try {
            analyzeAndCheckUnhandled(testDataFile, files, useLightTree = true)
        } catch (t: AssertionError) {
            throw t
        } catch (t: Throwable) {
            throw t
        }
    }

    fun analyzeAndCheckUnhandled(testDataFile: File, files: List<BaseDiagnosticsTest.TestFile>, useLightTree: Boolean)
}