/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test

import org.junit.runner.RunWith
import org.junit.runners.Suite

@Suite.SuiteClasses(
    ContinuationStackTraceTestGenerated::class,
    XCoroutinesStackTraceTestGenerated::class
)
@RunWith(Suite::class)
class CoroutineDebuggerTestSuite