// WITH_STDLIB

import kotlin.test.*

fun when2(i: Int): Int {
  when (i) {
    0 -> return 42
    else -> return 24
  }
}

fun box(): String {
  val res = when2(0)
  if (res != 42) return "FAIL $res"

  return "OK"
}
