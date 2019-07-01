/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.jetbrains.kotlin.idea.conversion.copy.ConvertJavaCopyPasteProcessor

abstract class AbstractPerformanceNewJavaToKotlinCopyPasteConversionTest :
    AbstractPerformanceJavaToKotlinCopyPasteConversionTest(newJ2K = true) {

    override fun validate(path: String, noConversionExpected: Boolean) {
        // nj2k behaves slightly different to classic j2k in cases where conversion is not expected
        // this is a performance test rather validation test
        if (!noConversionExpected) {
            kotlin.test.assertEquals(
                noConversionExpected, !ConvertJavaCopyPasteProcessor.conversionPerformed,
                if (noConversionExpected) "Conversion to Kotlin should not be suggested" else "No conversion to Kotlin suggested"
            )
        }
        // as well output of nj2k could be slightly different to output of j2k those test data is used
    }
}