/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.enum_equals

import kotlin.test.*

enum class EnumA {
    A, B
}

enum class EnumB {
    B
}

@Test fun run() {
    println(EnumA.A == EnumA.A)
    println(EnumA.A == EnumA.B)
    println(EnumA.A == EnumB.B)
}