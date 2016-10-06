/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.scripts

import org.junit.Assert
import java.io.ByteArrayOutputStream
import java.io.PrintStream

internal const val NUM_4_LINE = "num: 4"

internal const val FIB_SCRIPT_OUTPUT_TAIL =
"""
fib(1)=1
fib(0)=1
fib(2)=2
fib(1)=1
fib(3)=3
fib(1)=1
fib(0)=1
fib(2)=2
fib(4)=5
"""

internal fun captureOut(body: () -> Unit): String {
    val outStream = ByteArrayOutputStream()
    val prevOut = System.out
    System.setOut(PrintStream(outStream))
    try {
        body()
    }
    finally {
        System.out.flush()
        System.setOut(prevOut)
    }
    return outStream.toString()
}

internal fun assertEqualsTrimmed(expected: String, actual: String) =
        Assert.assertEquals(expected.trim(), actual.trim())
