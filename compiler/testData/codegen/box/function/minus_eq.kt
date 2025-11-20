// WITH_STDLIB

import kotlin.test.*

fun minus_eq(a: Int): Int {
  var b = 11
  b -= a
  return b
}

fun box(): String {
  assertEquals (-12, minus_eq(23))
  return "OK"
}
