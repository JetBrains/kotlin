/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator.test

import org.junit.jupiter.api.Test

class TestRunner {
    @Test
    fun runMain() = main()
}

fun main() {
    org.jetbrains.kotlin.k1k2uicomparator.main()
}
