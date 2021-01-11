/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.test.interpreter

import junit.framework.TestCase
import org.jetbrains.kotlin.generators.interpreter.DESTINATION
import org.jetbrains.kotlin.generators.interpreter.generateMap
import org.jetbrains.kotlin.test.KotlinTestUtils

class GenerateInterpreterMapTest : TestCase() {
    fun testGeneratedDataIsUpToDate() {
        val text = generateMap()
        KotlinTestUtils.assertEqualsToFile(DESTINATION, text)
    }
}
