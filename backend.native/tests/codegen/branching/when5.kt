package codegen.branching.when5

import kotlin.test.*

fun when5(i: Int): Int {
  when (i) {
    0 -> return 42
    1 -> return 4
    2 -> return 3
    3 -> return 2
    4 -> return 1
    else -> return 24
  }
}

@Test fun runTest() {
  if (when5(2) != 3) throw Error()
}