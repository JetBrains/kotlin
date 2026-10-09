/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.cli

import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.jklib.K2JKlibCompiler
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractJKlibCliTest : AbstractCliTest() {
    fun doJklibTest(fileName: String) {
        doTest(fileName, K2JKlibCompiler())
    }
}
