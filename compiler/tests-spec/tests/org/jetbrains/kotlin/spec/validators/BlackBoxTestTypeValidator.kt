/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.validators

import org.jetbrains.kotlin.spec.models.AbstractSpecTest
import java.io.File

class BlackBoxTestTypeValidator(
    testDataFile: File,
    private val testInfo: AbstractSpecTest
) : AbstractTestValidator(testInfo, testDataFile) {
    override fun computeTestTypes() = mapOf(1 to testInfo.testType)
}
