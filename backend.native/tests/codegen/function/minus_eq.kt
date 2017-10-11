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