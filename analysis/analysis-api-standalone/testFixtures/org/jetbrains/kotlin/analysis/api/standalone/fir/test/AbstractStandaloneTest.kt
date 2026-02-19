/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test

import org.jetbrains.kotlin.analysis.test.framework.TestWithDisposable
import java.nio.file.Path
import java.nio.file.Paths

abstract class AbstractStandaloneTest : TestWithDisposable() {
    abstract val suiteName: String

    protected fun testDataPath(path: String): Path {
        return Paths.get("testData/$suiteName").resolve(path)
    }
}