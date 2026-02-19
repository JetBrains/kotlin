// WITH_STDLIB

import kotlin.test.*

fun plus_eq(a: Int): Int {
  var b = 11
  b += a
  return b
}

fun box(): String {
  assertEquals(14, plus_eq(3))
  return "OK"
}
