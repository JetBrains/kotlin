/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.test.evaluate

import junit.framework.TestCase
import org.jetbrains.kotlin.generators.evaluate.DESTINATION
import org.jetbrains.kotlin.generators.evaluate.generateMap
import org.jetbrains.kotlin.test.KotlinTestUtils

class GenerateOperationsMapTest : TestCase() {
    fun testGeneratedDataIsUpToDate() {
        val text = generateMap()
        KotlinTestUtils.assertEqualsToFile(DESTINATION, text)
    }
}
