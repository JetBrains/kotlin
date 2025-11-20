// WITH_STDLIB

import kotlin.test.*

fun when5(i: Int): Int {
  when (i) {
    0 -> return 42
    1 -> return 4
    2 -> return 3
    3 -> return 2
    4 -> return 1
  }
  return 24
}

fun box(): String {
  val res = when5(5)
  if (res != 24) return "FAIL $res"

  return "OK"
}
