// WITH_STDLIB

import kotlin.test.*

fun cycle(cnt: Int): Int {
  var sum = 1
  while (sum == cnt) {
    sum = sum + 1
  }
  return sum
}

fun box(): String {
  assertEquals(2, cycle(1))
  assertEquals(1, cycle(0))

  return "OK"
}
