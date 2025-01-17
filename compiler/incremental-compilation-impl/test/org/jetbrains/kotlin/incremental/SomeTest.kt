/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import kotlin.test.Test

class SomeTest {
    @Test fun test1() {
        throw RuntimeException("SomeTest.test1")
    }

    @Test fun test2() {
        throw RuntimeException("SomeTest.test1")
    }
}