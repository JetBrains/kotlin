/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.test.evaluate

import junit.framework.TestCase
import org.jetbrains.kotlin.generators.evaluate.FIR_DEST_FILE
import org.jetbrains.kotlin.generators.evaluate.generateFirMap
import org.jetbrains.kotlin.test.KotlinTestUtils

class GenerateFirOperationsMapTest : TestCase() {
    fun testGeneratedDataIsUpToDate() {
        val text = generateFirMap()
        KotlinTestUtils.assertEqualsToFile(FIR_DEST_FILE, text)
    }
}
