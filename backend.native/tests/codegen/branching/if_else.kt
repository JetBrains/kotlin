package codegen.branching.if_else

import kotlin.test.*

fun if_else(b: Boolean): Int {
  if (b) return 42
  else   return 24
}

@Test fun runTest() {
  if (if_else(false) != 24) throw Error()
}