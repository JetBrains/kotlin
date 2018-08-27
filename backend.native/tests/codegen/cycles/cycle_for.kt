/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.cycles.cycle_for

import kotlin.test.*

fun cycle_for(arr: Array<Int>) : Int {
  var sum = 0
  for (i in arr) {
    sum += i
  }
  return sum
}

@Test fun runTest() {
  if (cycle_for(Array(4, init = { it })) != 6) throw Error()
}