package codegen.function.plus_eq

import kotlin.test.*

fun plus_eq(a: Int): Int {
  var b = 11
  b += a
  return b
}

@Test fun runTest() {
  if (plus_eq(3) != 14) throw Error()
}