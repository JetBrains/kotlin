package codegen.branching.when2

import kotlin.test.*

fun when2(i: Int): Int {
  when (i) {
    0 -> return 42
    else -> return 24
  }
}

@Test fun runTest() {
  if (when2(0) != 42) throw Error()
}