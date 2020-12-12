/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.test.ConfigurationKind
import java.io.File

abstract class AbstractDiagnosticsTestWithStdLib : AbstractDiagnosticsTest() {

    override fun extractConfigurationKind(files: List<TestFile>): ConfigurationKind {
        return ConfigurationKind.NO_KOTLIN_REFLECT
    }

    override fun shouldValidateFirTestData(testDataFile: File): Boolean {
        val path = testDataFile.absolutePath
        return !path.endsWith(".kts")
    }
}
