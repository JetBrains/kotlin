/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.defaults10

import kotlin.test.*

enum class A(one: Int, val two: Int = one) {
    FOO(42)
}

@Test fun runTest() {
    println(A.FOO.two)
}