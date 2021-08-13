/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer.fir

import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test


@SuppressWarnings("all")
@TestMetadata("compiler/fir/raw-fir/psi2fir/testData/collectionLiteralDebug")
@com.intellij.testFramework.TestDataPath("\$PROJECT_ROOT")
class FirDebugTestRaish : AbstractFirVisualizerTest() {

    @Test
    @TestMetadata("simpleVariable.kt")
    fun testSimpleVariable() {
        runTest("compiler/visualizer/testData/collectionLiteralDebug/simpleVariable.kt")
    }

    @Test
    @TestMetadata("simpleFunctionCall.kt")
    fun testSimpleFunctionCall() {
        runTest("compiler/visualizer/testData/collectionLiteralDebug/simpleFunctionCall.kt")
    }

    @Test
    @TestMetadata("debug.kt")
    fun testDebug() {
        runTest("compiler/visualizer/testData/collectionLiteralDebug/debug.kt")
    }
}