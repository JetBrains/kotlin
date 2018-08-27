/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.propertyCallableReference.varModule

import kotlin.test.*

var x = 42

@Test fun runTest() {
    val p = ::x
    p.set(117)
    println(x)
    println(p.get())
}