/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.lambdaAsAny

import kotlin.test.*

inline fun foo(x: Any) {
    println(if (x === x) "Ok" else "Fail")
}

@Test fun runTest() {
    foo { 42 }
}