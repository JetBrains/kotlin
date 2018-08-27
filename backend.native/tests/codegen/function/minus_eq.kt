/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.minus_eq

import kotlin.test.*

fun minus_eq(a: Int): Int {
  var b = 11
  b -= a
  return b
}

@Test fun runTest() {
  if (minus_eq(23) != -12) throw Error()
}