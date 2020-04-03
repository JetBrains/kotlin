/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import java.io.File

abstract class AbstractDiagnosticsTestWithFirValidation : AbstractDiagnosticsTest() {
    override fun shouldValidateFirTestData(testDataFile: File): Boolean {
        val path = testDataFile.absolutePath
        return !path.endsWith(".kts") && !path.contains("codegen")
    }
}