/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import a.*
import kotlin.test.*

fun main() {
    assertEquals(42, foo(7) { it * 2 })
}